package interview.guide.modules.interview.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 第三步：前端专属的子类（扩展前端核心字段）
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FrontQuestionDistribution extends QuestionDistribution {
    // 前端专属字段
    private final int project;
    private final int htmlCss;
    private final int jsBasic;
    private final int framework; // Vue/React等框架
    private final int browserNet; // 浏览器/网络
    private final int engineering; // 工程化（Webpack/TS等）


}
