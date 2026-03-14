package interview.guide.modules.interview.model;

import lombok.Builder;

/**
 * 面试问题DTO
 */
@Builder
public record InterviewQuestionDTO(
    int questionIndex,
    String question,
    QuestionType type,
    String category,      // 问题类别：项目经历、Java基础、集合、并发、MySQL、Redis、Spring、SpringBoot
    String userAnswer,    // 用户回答
    Integer score,        // 单题得分 (0-100)
    String feedback,      // 单题反馈
    boolean isFollowUp,   // 是否为追问
    int addQuestionIndex, //第几个追问
    Integer parentQuestionIndex // 追问关联的主问题索引
) {
    public enum QuestionType {
        // ==================== 通用类型 ====================
        PROJECT,          // 项目经历（所有岗位通用）
        // ==================== 后端专属 ====================
        JAVA_BASIC,       // Java基础
        JAVA_COLLECTION,  // Java集合
        JAVA_CONCURRENT,  // Java并发
        MYSQL,            // MySQL
        REDIS,            // Redis
        SPRING,           // Spring
        SPRING_BOOT,      // Spring Boot

        // ==================== 前端专属 ====================
        HTML_CSS,         // HTML/CSS
        JS_BASIC,         // JavaScript基础
        FRONT_FRAMEWORK,  // 前端框架（Vue/React）
        BROWSER_NET,      // 浏览器/网络（EventLoop/HTTP/跨域等）
        FRONT_ENGINEERING,// 前端工程化（Webpack/Vite/TS/ESLint等）

        // ==================== 测试专属 ====================
        TEST_CASE_DESIGN, // 测试用例设计
        TEST_AUTOMATION,  // 自动化测试（接口/UI）
        TEST_PERFORMANCE, // 性能测试（JMeter/Locust）
        TEST_DB_CHECK,    // 数据库验证（查库/数据校验）
        TEST_BUG_MANAGE   // 缺陷管理（BUG生命周期/提测规范）
    }
    
    /**
     * 创建新问题（未回答状态）
     */
    public static InterviewQuestionDTO create(int index, String question, QuestionType type, String category) {
        return new InterviewQuestionDTO(index, question, type, category, null, null, null, false, 0,null);
    }

    /**
     * 创建新问题（支持追问标记）
     */
    public static InterviewQuestionDTO create(
            int index,
            String question,
            QuestionType type,
            String category,
            boolean isFollowUp,
            Integer parentQuestionIndex) {
        return new InterviewQuestionDTO(index, question, type, category, null, null, null, isFollowUp,0, parentQuestionIndex);
    }
    
    /**
     * 添加用户回答
     */
    public InterviewQuestionDTO withAnswer(String answer) {
        return new InterviewQuestionDTO(
            questionIndex, question, type, category, answer, score, feedback, isFollowUp, 0,parentQuestionIndex);
    }
    
    /**
     * 添加评分和反馈
     */
    public InterviewQuestionDTO withEvaluation(int score, String feedback) {
        return new InterviewQuestionDTO(
            questionIndex, question, type, category, userAnswer, score, feedback, isFollowUp, 0,parentQuestionIndex);
    }
}
