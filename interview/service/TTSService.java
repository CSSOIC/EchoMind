
package interview.guide.modules.interview.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TTSService {

    @Value("${app.tts.api-key}")
    private String apiKey;

    @Value("${app.tts.endpoint}")
    private String endpoint;

    /**
     * 将文本转换为语音（MP3 格式）
     *
     * @param text 要转换的文本
     * @param voice 音色（可选，默认 zh-CN-XiaoxiaoNeural）
     * @param rate 语速（可选，默认 1.0）
     * @return MP3 音频字节数组
     */
    public byte[] generateSpeech(String text, String voice, Double rate) {
        try {
            log.info("开始生成 TTS 语音，文本长度：{}", text.length());

            // 清理特殊字符，防乱码
            String safeText = cleanText(text);

            // 设置默认参数
            String selectedVoice = (voice == null || voice.isBlank())
                    ? "zh-CN-XiaoxiaoNeural"
                    : voice;
            double selectedRate = (rate == null) ? 1.0 : rate;

            log.info("使用音色：{}, 语速：{}", selectedVoice, selectedRate);

            // 调用硅基智能 TTS API（使用 Hutool HttpRequest）
            byte[] audioBytes = callTTSApi(safeText, selectedVoice, selectedRate);

            log.info("TTS 语音生成成功，音频大小：{} bytes", audioBytes.length);

            return audioBytes;

        } catch (Exception e) {
            log.error("TTS 语音生成失败", e);
            throw new RuntimeException("TTS 语音生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 清理文本中的特殊字符
     */
    private String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("\"", "\\\"")
                .replace("\n", "")
                .replace("\r", "");
    }

    /**
     * 调用硅基智能 TTS API（使用 Hutool HttpRequest）
     */
    private byte[] callTTSApi(String text, String voice, double rate) {
        // 构建请求体
        String requestBody = String.format("""
            {
                "model": "FunAudioLLM/CosyVoice2-0.5B",
                "input": "%s",
                "voice": "%s",
                "response_format": "mp3",
                "speed": %.2f
            }
            """, text, voice, rate);

        log.debug("TTS 请求体：{}", requestBody);

        // 使用 Hutool HttpRequest 发送 POST 请求
        try (HttpResponse httpResponse = HttpRequest.post(endpoint)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(requestBody)
                .setConnectionTimeout(10000)
                .setReadTimeout(30000)
                .execute()) {

            // 检查响应状态
            if (httpResponse.isOk()) {
                return httpResponse.bodyBytes();
            } else {
                throw new RuntimeException("TTS API 调用失败，状态码：" + httpResponse.getStatus());
            }
        }
    }

    /**
     * 生成并返回可下载的 Resource 对象
     *
     * @param text 要转换的文本
     * @param voice 音色
     * @param rate 语速
     * @return 包含 MP3 音频的 Resource 对象
     */
    public ByteArrayResource generateSpeechAsResource(String text, String voice, Double rate) {
        byte[] audioBytes = generateSpeech(text, voice, rate);
        return new ByteArrayResource(audioBytes);
    }
}
