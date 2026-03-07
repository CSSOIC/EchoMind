package interview.guide.modules.interview.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 第二步：后端专属的子类（扩展后端核心字段）
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BackQuestionDistribution extends QuestionDistribution {


    // 后端专属字段（私有+final保证不可变，符合常量语义）
    private final int project;
    private final int mysql;
    private final int redis;
    private final int javaBasic;
    private final int javaCollection;
    private final int javaConcurrent;
    private final int spring;

}
