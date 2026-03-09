package interview.guide.modules.interview;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import interview.guide.common.annotation.RateLimit;
import interview.guide.common.result.Result;
import interview.guide.infrastructure.VoiceUtil.VoiceFeatureUtil;
import interview.guide.modules.interview.model.*;
import interview.guide.modules.interview.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 面试控制器
 * 提供模拟面试相关的API接口
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewSessionService sessionService;
    private final InterviewHistoryService historyService;
    private final InterviewPersistenceService persistenceService;

    /**
     * 创建面试会话
     */
    @PostMapping("/api/interview/sessions")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL, RateLimit.Dimension.IP}, count = 5)
    public Result<InterviewSessionDTO> createSession(@RequestBody CreateInterviewRequest request) {
        log.info("创建面试会话，题目数量: {}", request.questionCount());
        InterviewSessionDTO session = sessionService.createSession(request);
        return Result.success(session);
    }

    /**
     * 获取会话信息
     */
    @GetMapping("/api/interview/sessions/{sessionId}")
    public Result<InterviewSessionDTO> getSession(@PathVariable String sessionId) {
        InterviewSessionDTO session = sessionService.getSession(sessionId);
        return Result.success(session);
    }

    /**
     * 获取当前问题
     */
    @GetMapping("/api/interview/sessions/{sessionId}/question")
    public Result<Map<String, Object>> getCurrentQuestion(@PathVariable String sessionId) {
        return Result.success(sessionService.getCurrentQuestionResponse(sessionId));
    }

    /**
     * 提交答案
     */
    @PostMapping("/api/interview/sessions/{sessionId}/answers")
    @RateLimit(dimensions = {RateLimit.Dimension.GLOBAL}, count = 10)
    public Result<SubmitAnswerResponse> submitAnswer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        log.info("提交答案: 会话{}, 问题{}", sessionId, questionIndex);
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer);
        SubmitAnswerResponse response = sessionService.submitAnswer(request);
        return Result.success(response);
    }

    /**
     * 生成面试报告
     */
    @GetMapping("/api/interview/sessions/{sessionId}/report")
    public Result<InterviewReportDTO> getReport(@PathVariable String sessionId) {
        log.info("生成面试报告: {}", sessionId);
        InterviewReportDTO report = sessionService.generateReport(sessionId);
        return Result.success(report);
    }

    /**
     * 查找未完成的面试会话
     * GET /api/interview/sessions/unfinished/{resumeId}
     */
    @GetMapping("/api/interview/sessions/unfinished/{resumeId}")
    public Result<InterviewSessionDTO> findUnfinishedSession(@PathVariable Long resumeId) {
        return Result.success(sessionService.findUnfinishedSessionOrThrow(resumeId));
    }

    /**
     * 暂存答案（不进入下一题）
     */
    @PutMapping("/api/interview/sessions/{sessionId}/answers")
    public Result<Void> saveAnswer(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> body) {
        Integer questionIndex = (Integer) body.get("questionIndex");
        String answer = (String) body.get("answer");
        log.info("暂存答案: 会话{}, 问题{}", sessionId, questionIndex);
        SubmitAnswerRequest request = new SubmitAnswerRequest(sessionId, questionIndex, answer);
        sessionService.saveAnswer(request);
        return Result.success(null);
    }

    /**
     * 提前交卷
     */
    @PostMapping("/api/interview/sessions/{sessionId}/complete")
    public Result<Void> completeInterview(@PathVariable String sessionId) {
        log.info("提前交卷: {}", sessionId);
        sessionService.completeInterview(sessionId);
        return Result.success(null);
    }

    /**
     * 获取面试会话详情
     * GET /api/interview/sessions/{sessionId}/details
     */
    @GetMapping("/api/interview/sessions/{sessionId}/details")
    public Result<InterviewDetailDTO> getInterviewDetail(@PathVariable String sessionId) {
        InterviewDetailDTO detail = historyService.getInterviewDetail(sessionId);
        return Result.success(detail);
    }

    /**
     * 导出面试报告为PDF
     */
    @GetMapping("/api/interview/sessions/{sessionId}/export")
    public ResponseEntity<byte[]> exportInterviewPdf(@PathVariable String sessionId) {
        try {
            byte[] pdfBytes = historyService.exportInterviewPdf(sessionId);
            String filename = URLEncoder.encode("模拟面试报告_" + sessionId + ".pdf",
                    StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除面试会话
     */
    @DeleteMapping("/api/interview/sessions/{sessionId}")
    public Result<Void> deleteInterview(@PathVariable String sessionId) {
        log.info("删除面试会话: {}", sessionId);
        persistenceService.deleteSessionBySessionId(sessionId);
        return Result.success(null);
    }



  /*  @Value("${app.tts.api-key:}")
    private String apiKey;

    @Value("${app.tts.endpoint}")
    private String endpoint;


    *//*
    * 获取TTS语音
     *//*
    @PostMapping("/api/interview/sessions/tts")
    public ResponseEntity<Resource> getTtsMp3(@RequestBody TtsDTO req) {
        try {


            // 防乱码：清理特殊字符
            String safeText = req.getText().replace("\"", "\\\"").replace("\n", "").replace("\r", "");

            // 语速默认1.0
            double speed = req.getRate() == null ? 1.0 : req.getRate();
            // 音色默认值（可按前端传的voice覆盖）
            String voice = req.getVoice() == null ? "zh-CN-XiaoxiaoNeural" : req.getVoice();

            // 构造硅基TTS请求体
            String reqBody = String.format("""
                {
                    "model": "FunAudioLLM/CosyVoice2-0.5B",
                    "input": "%s",
                    "voice": "%s",
                    "response_format": "mp3",
                    "speed": %s
                }
                """, safeText, voice, speed);

            // 带超时 防请求卡死
            HttpResponse httpResponse = HttpRequest.post(endpoint)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(reqBody)
                    .setConnectionTimeout(10000)
                    .setReadTimeout(30000)
                    .execute();

            byte[] mp3Bytes = httpResponse.bodyBytes();
            ByteArrayResource resource = new ByteArrayResource(mp3Bytes);

            // 返回标准MP3
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            String fileName = URLEncoder.encode("voice.mp3", StandardCharsets.UTF_8);
            headers.add("Content-Disposition", "inline;filename*=UTF-8''" + fileName);

            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }*/
    private final TTSService textToSpeechService;
    /*
     * 获取 TTS 语音
     */
    @PostMapping("/api/interview/sessions/tts")
    public ResponseEntity<Resource> getTtsMp3(@RequestBody TtsDTO req) {
        try {
            log.info("收到 TTS 请求，文本长度：{}", req.getText().length());

            // 调用 TTS 服务生成语音
            byte[] mp3Bytes = textToSpeechService.generateSpeech(
                    req.getText(),
                    req.getVoice(),
                    req.getRate()
            );

            ByteArrayResource resource = new ByteArrayResource(mp3Bytes);

            // 返回标准 MP3
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            String fileName = URLEncoder.encode("voice.mp3", StandardCharsets.UTF_8);
            headers.add("Content-Disposition", "inline;filename*=UTF-8''" + fileName);

            return ResponseEntity.ok().headers(headers).body(resource);

        } catch (Exception e) {
            log.error("TTS 语音生成失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }



    private final VoiceRecognitionService voiceRecognitionService;

    /**
     * 语音识别 - 使用阿里云 Qwen3-Omni-Flash 模型（通过 ChatClient 调用）
     */
    @PostMapping("/api/interview/sessions/asr")
    public ResponseEntity<AsrVoiceResult> uploadVoice(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            AsrVoiceResult result = new AsrVoiceResult();
            result.setSuccess(false);
            result.setMsg("请上传音频文件");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            log.info("收到语音识别请求，文件名：{}, 大小：{} bytes", file.getOriginalFilename(), file.getSize());

            // 调用新的语音识别服务（使用 ChatClient）
            AsrVoiceResult result = voiceRecognitionService.recognizeVoice(file);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("语音识别处理失败", e);
            AsrVoiceResult result = new AsrVoiceResult();
            result.setSuccess(false);
            result.setMsg("服务异常：" + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}