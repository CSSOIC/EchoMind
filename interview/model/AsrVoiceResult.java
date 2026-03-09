package interview.guide.modules.interview.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class AsrVoiceResult implements Serializable {
    private Boolean success;
    private String text; // 识别文本
    private Double speedScore; // 语速0-10
    private Double confidenceScore; // 自信度0-10
    private Double nervousScore; // 紧张感0-10
    private String msg;
}
