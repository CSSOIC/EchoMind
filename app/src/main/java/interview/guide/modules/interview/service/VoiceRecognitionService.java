package interview.guide.modules.interview.service;

import interview.guide.infrastructure.VoiceUtil.VoiceFeatureUtil;
import interview.guide.modules.interview.model.AsrVoiceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceRecognitionService {

    private final VoiceFeatureUtil featureUtil;
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 使用阿里云 Qwen3-Omni-Flash 模型进行语音识别
     * 该模型支持音频输入，可直接上传音频文件
     */
    public AsrVoiceResult recognizeVoice(MultipartFile file) {
        AsrVoiceResult result = new AsrVoiceResult();

        try {
            byte[] audioBytes = file.getBytes();

            log.info("收到语音识别请求，文件大小：{} bytes", audioBytes.length);

            // 1. 提取音频特征（用于后续评分）
            double[] features = featureUtil.getVoiceFeature(audioBytes);
            double volStable = features[0];
            double pitchFluct = features[1];
            double silenceRatio = features[2];

            log.info("音频特征提取完成：音量稳定性={}, 音高起伏={}, 静音占比={}",
                    volStable, pitchFluct, silenceRatio);

            // 2. 使用 ChatClient 调用阿里云 Qwen3-Omni-Flash 模型
            String recognizedText = callQwenOmniFlash(file);

            if (recognizedText == null || recognizedText.trim().isEmpty()) {
                result.setSuccess(false);
                result.setMsg("未识别到有效语音内容");
                return result;
            }

            // 3. 简单估算单词数量（中文按字符，英文按空格）
            int wordCount = estimateWordCount(recognizedText);

            // 4. 计算语速（假设音频时长通过特征估算）
            double estimatedDuration = estimateAudioDuration(audioBytes.length);
            double speedScore = featureUtil.calSpeedScore(wordCount, estimatedDuration);
            double wpm = (wordCount / estimatedDuration) * 60;

            // 5. 计算自信度和紧张感
            double confidenceScore = featureUtil.calConfidence(volStable, pitchFluct, silenceRatio);
            double nervousScore = featureUtil.calNervous(confidenceScore, wpm);

            // 6. 封装返回结果
            result.setSuccess(true);
            result.setText(recognizedText);
            result.setSpeedScore(Math.round(speedScore * 100.0) / 100.0);
            result.setConfidenceScore(Math.round(confidenceScore * 100.0) / 100.0);
            result.setNervousScore(Math.round(nervousScore * 100.0) / 100.0);

            log.info("语音识别成功：文本长度={}, 语速分={}, 自信度={}, 紧张感={}",
                    recognizedText.length(), speedScore, confidenceScore, nervousScore);

            return result;

        } catch (Exception e) {
            log.error("语音识别失败", e);
            result.setSuccess(false);
            result.setMsg("识别服务异常：" + e.getMessage());
            return result;
        }
    }

    /**
     * 调用阿里云 Qwen3-Omni-Flash 模型进行语音识别
     * 使用 Spring AI ChatClient 的多模态能力
     */
    private String callQwenOmniFlash(MultipartFile file) throws Exception {
        // 构建 ChatClient
        ChatClient chatClient = chatClientBuilder.defaultOptions(ChatOptions.builder().model("qwen-3-omni-flash").build()).build();

        // 创建消息内容（包含音频文件）
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("text", "请识别这段音频中的语音内容，直接返回识别出的文字，不要添加任何解释或说明。");

        // 将音频文件转换为字节数组并传递
        byte[] audioBytes = file.getBytes();

        log.info("正在调用 Qwen3-Omni-Flash 模型进行语音识别...");

        // 使用 ChatClient 调用多模态模型
        // 注意：Spring AI 的多模态支持需要通过特定的 Message 类型传递媒体资源
        String response = chatClient.prompt()
                .user(u -> u
                        .text("请识别这段音频中的语音内容，直接返回识别出的文字，不要添加任何解释或说明。")
                        .media(MimeType.valueOf("audio/wav"), file.getResource())
                )
                .call()
                .content();

        log.info("语音识别完成，识别结果长度：{}", response != null ? response.length() : 0);

        return response != null ? response.trim() : "";
    }

    /**
     * 简单估算单词/字符数量
     * 中文：每个汉字算一个词
     * 英文：按空格分词
     */
    private int estimateWordCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 移除标点符号
        String cleaned = text.replaceAll("[\\p{P}\\p{S}]", " ");

        // 按空格分割并统计
        String[] tokens = cleaned.split("\\s+");
        int count = 0;
        for (String token : tokens) {
            if (!token.trim().isEmpty()) {
                count++;
            }
        }

        return count;
    }

    /**
     * 根据音频字节数估算时长（假设 16kHz, 16bit, 单声道）
     * 每秒数据量 = 16000 * 2 = 32000 字节
     */
    private double estimateAudioDuration(int byteSize) {
        int sampleRate = 16000;
        int bytesPerSample = 2; // 16bit
        int channels = 1; // 单声道

        double bytesPerSecond = sampleRate * bytesPerSample * channels;
        return byteSize / bytesPerSecond;
    }
}
