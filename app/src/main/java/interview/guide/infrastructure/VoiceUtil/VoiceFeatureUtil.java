package interview.guide.infrastructure.VoiceUtil;


import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
@Component
public class VoiceFeatureUtil {

    // 音频采样率：16kHz，适合人声分析（人声频率范围 85Hz-255Hz）
    private static final float SAMPLE_RATE = 16000;

    // 静音检测阈值：RMS 音量低于此值认为是静音/停顿
    private static final double SILENCE_THRESHOLD = 0.002;

    /**
     * 提取语音三大特征：
     * 1. 音量稳定性 - 反映说话人的自信程度和情绪稳定性
     * 2. 音高起伏 - 通过过零率估算，反映语调变化
     * 3. 静音占比 - 反映停顿频率，评估流利度
     *
     * @param audioBytes PCM 格式的音频字节数组
     * @return double[3] = [音量稳定性，音高起伏，静音占比]
     */
    public double[] getVoiceFeature(byte[] audioBytes) throws Exception {
        // 将字节数组转换为音频输入流
        ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
        AudioInputStream ais = AudioSystem.getAudioInputStream(bais);

        // 获取音频格式信息
        AudioFormat format = ais.getFormat();
        int sampleSizeInBits = format.getSampleSizeInBits(); // 采样位数（8bit 或 16bit）

        // 存储每帧的音频特征
        List<Float> volumeList = new ArrayList<>();      // 音量（RMS 值）
        List<Integer> zeroCrossingRates = new ArrayList<>(); // 过零率（估算音高）
        List<Boolean> isSilenceList = new ArrayList<>();   // 是否静音

        // 缓冲区：每次读取 1024 字节
        byte[] buffer = new byte[1024];
        int bytesRead;

        // 逐帧读取音频数据
        while ((bytesRead = ais.read(buffer)) != -1) {
            // 将字节转换为归一化的 double 样本值（-1.0 ~ 1.0）
            double[] samples = bytesToDouble(buffer, bytesRead, sampleSizeInBits);

            // 计算 RMS 音量（均方根，反映声音强度）
            double rms = calculateRMS(samples);
            volumeList.add((float) rms);

            // 判断是否为静音段
            isSilenceList.add(rms < SILENCE_THRESHOLD);

            // 计算过零率（单位时间内波形穿过零点的次数，高频=高音调）
            int zcr = calculateZeroCrossingRate(samples);
            zeroCrossingRates.add(zcr);
        }

        // 关闭资源
        ais.close();
        bais.close();

        // ========== 计算特征值 ==========

        // 1. 音量稳定性：平均值越高、方差越小 = 越稳定
        double volAvg = volumeList.stream().mapToDouble(Float::doubleValue).average().orElse(0);
        double volVar = volumeList.stream().mapToDouble(v -> Math.pow(v - volAvg, 2)).average().orElse(0);
        // 稳定性公式：1 - (标准差 / 均值)，范围 0~1
        double volStable = volAvg > 0 ? Math.max(0, 1 - Math.sqrt(volVar) / volAvg) : 0.5;

        // 2. 音高起伏：用过零率的平均值近似表示
        double zcrAvg = zeroCrossingRates.stream().mapToInt(Integer::intValue).average().orElse(0);
        // 归一化到 0~1 范围
        double pitchFluctuate = Math.min(1, zcrAvg / 1000);

        // 3. 静音占比：静音帧数 / 总帧数
        long silenceCount = isSilenceList.stream().filter(Boolean::booleanValue).count();
        double silenceRatio = isSilenceList.isEmpty() ? 0 : (double) silenceCount / isSilenceList.size();

        return new double[]{volStable, pitchFluctuate, silenceRatio};
    }

    /**
     * 将原始音频字节转换为归一化的 double 样本值
     *
     * @param bytes 原始音频字节
     * @param length 有效字节长度
     * @param sampleSizeInBits 采样位数（8 或 16）
     * @return 归一化的样本数组（范围 -1.0 ~ 1.0）
     */
    private double[] bytesToDouble(byte[] bytes, int length, int sampleSizeInBits) {
        // 计算样本数量
        int numSamples = length / (sampleSizeInBits / 8);
        double[] samples = new double[numSamples];

        for (int i = 0; i < numSamples; i++) {
            if (sampleSizeInBits == 16) {
                // 16bit PCM：小端序，两个字节组成一个样本
                int idx = i * 2;
                // 合并高低字节（Java 的 short 是有符号的）
                short sample = (short) ((bytes[idx + 1] << 8) | (bytes[idx] & 0xFF));
                // 归一化到 -1.0 ~ 1.0（16bit 最大值是 32767）
                samples[i] = sample / 32768.0;
            } else if (sampleSizeInBits == 8) {
                // 8bit PCM：无符号，需要减去 128 转为有符号
                samples[i] = (bytes[i] - 128) / 128.0;
            }
        }

        return samples;
    }

    /**
     * 计算 RMS（Root Mean Square，均方根）
     * 反映声音的响度/强度，比简单平均值更准确
     *
     * @param samples 音频样本数组
     * @return RMS 值（0.0 ~ 1.0）
     */
    private double calculateRMS(double[] samples) {
        double sum = 0;
        // 平方和
        for (double sample : samples) {
            sum += sample * sample;
        }
        // 开平方根
        return Math.sqrt(sum / samples.length);
    }

    /**
     * 计算过零率（Zero-Crossing Rate）
     * 波形从正变负或从负变正的次数
     * 高频声音（如元音）过零率高，低频声音（如浊音）过零率低
     *
     * @param samples 音频样本数组
     * @return 过零次数
     */
    private int calculateZeroCrossingRate(double[] samples) {
        int zcr = 0;
        // 遍历相邻样本对
        for (int i = 1; i < samples.length; i++) {
            // 检测符号变化（>=0 为"正"，<0 为"负"）
            if ((samples[i - 1] >= 0 && samples[i] < 0) ||
                    (samples[i - 1] < 0 && samples[i] >= 0)) {
                zcr++;
            }
        }
        return zcr;
    }

    /**
     * 计算语速得分（0-10 分）
     * 基于 WPM（Words Per Minute，每分钟单词数）
     *
     * @param wordNum 单词总数
     * @param duration 语音时长（秒）
     * @return 语速得分
     */
    public double calSpeedScore(int wordNum, double duration) {
        if (duration < 1) return 5; // 时长太短，返回中等分数

        // 计算 WPM
        double wpm = (wordNum / duration) * 60;

        // 分段评分：
        // <100 WPM：太慢（2 分）
        if (wpm < 100) return 2;

        // 100-200 WPM：线性增长（4-7 分）
        if (wpm < 200) return wpm * 0.03 + 1;

        // 200-260 WPM：理想语速（9 分）
        if (wpm <= 260) return 9;

        // 260-320 WPM：稍快，开始扣分
        if (wpm <= 320) return 10 - (wpm - 260) * 0.03;

        // >320 WPM：太快（3 分）
        return 3;
    }

    /**
     * 计算自信度得分（0-10 分）
     * 综合音量稳定性、音高起伏、停顿占比
     *
     * @param volStable 音量稳定性（0-1）
     * @param pitchFluct 音高起伏（0-1）
     * @param silenceRatio 静音占比（0-1）
     * @return 自信度得分
     */
    public double calConfidence(double volStable, double pitchFluct, double silenceRatio) {
        // 基础分：音量稳定性占 40% 权重
        double score = volStable * 4;

        // 音高适中加分（0.2-0.6 之间）：语调有变化但不过分
        if (pitchFluct > 0.2 && pitchFluct < 0.6) score += 3;
        else score += 1.5;

        // 适度停顿加分（10%-25%）：思考自然
        if (silenceRatio >= 0.1 && silenceRatio <= 0.25) score += 3;
        else score += 1;

        return Math.min(10, score);
    }

    /**
     * 计算紧张感得分（0-10 分）
     * 自信度越低、语速越快/越慢 = 越紧张
     *
     * @param confidence 自信度得分
     * @param wpm 语速（WPM）
     * @return 紧张感得分
     */
    public double calNervous(double confidence, double wpm) {
        // 基础分：与自信度相反
        double base = 10 - confidence;

        // 语速过快（>280 WPM）：紧张表现
        if (wpm > 280) base += 2;

        // 语速过慢（<120 WPM）：也可能是紧张导致的卡顿
        if (wpm < 120) base += 1;

        return Math.min(10, base);
    }
}

