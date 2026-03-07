package interview.guide.modules.interview.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 第四步：测试专属的子类（扩展测试核心字段）
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TestQuestionDistribution extends QuestionDistribution {
    // 测试专属字段
    private final int project;
    private final int caseDesign; // 用例设计
    private final int automation; // 自动化测试
    private final int performance; // 性能测试
    private final int dbCheck; // 数据库验证
    private final int bugManage; // 缺陷管理

  }
