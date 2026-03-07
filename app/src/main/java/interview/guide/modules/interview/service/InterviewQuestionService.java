package interview.guide.modules.interview.service;

import interview.guide.common.ai.StructuredOutputInvoker;
import interview.guide.common.constant.JobConstants;
import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import interview.guide.modules.interview.Util.QuestionUtil;
import interview.guide.modules.interview.model.InterviewQuestionDTO;
import interview.guide.modules.interview.model.InterviewQuestionDTO.QuestionType;
import interview.guide.modules.interview.pojo.BackQuestionDistribution;
import interview.guide.modules.interview.pojo.FrontQuestionDistribution;
import interview.guide.modules.interview.pojo.QuestionDistribution;
import interview.guide.modules.interview.pojo.TestQuestionDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 面试问题生成服务
 * 基于简历内容生成针对性的面试问题
 */
@Service
public class InterviewQuestionService {
    
    private static final Logger log = LoggerFactory.getLogger(InterviewQuestionService.class);
    
    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final BeanOutputConverter<QuestionListDTO> outputConverter;
    private final StructuredOutputInvoker structuredOutputInvoker;
    private final int followUpCount;
    
    // 问题类型权重分配（按优先级）
    private static final double PROJECT_RATIO = 0.20;      // 20% 项目经历
    //后端

    private static final double Back_MYSQL_RATIO = 0.20;        // 20% MySQL
    private static final double Back_REDIS_RATIO = 0.20;        // 20% Redis
    private static final double Back_JAVA_BASIC_RATIO = 0.10;        // 10% Java基础
    private static final double Back_JAVA_COLLECTION_RATIO = 0.10; // 10% 集合
    private static final double Back_JAVA_CONCURRENT_RATIO = 0.10; // 10% 并发
    // 前端（核心考核项调整为前端专属，变量名加Front_前缀）
    private static final double Front_HTML_CSS_RATIO = 0.20;          // 20% HTML/CSS
    private static final double Front_JS_BASIC_RATIO = 0.20;          // 20% JavaScript基础
    private static final double Front_FRAMEWORK_RATIO = 0.15;         // 15% 前端框架(Vue/React)
    private static final double Front_BROWSER_NET_RATIO = 0.15;       // 15% 浏览器/网络
    private static final double Front_ENGINEERING_RATIO = 0.10;       // 10% 工程化(Webpack/TS)

    // 测试（核心考核项调整为测试专属，变量名加Test_前缀）
    private static final double Test_CASE_DESIGN_RATIO = 0.20;        // 20% 测试用例设计
    private static final double Test_AUTOMATION_RATIO = 0.20;         // 20% 自动化测试
    private static final double Test_PERFORMANCE_RATIO = 0.15;        // 15% 性能测试
    private static final double Test_DB_CHECK_RATIO = 0.15;           // 15% 数据库验证
    private static final double Test_BUG_MANAGE_RATIO = 0.10;         // 10% 缺陷管理
    //最大追问数
    private static final int MAX_FOLLOW_UP_COUNT = 2;



    // 中间DTO用于接收AI响应
    private record QuestionListDTO(
        List<QuestionDTO> questions
    ) {}
    
    private record QuestionDTO(
        String question,
        String type,
        String category,
        List<String> followUps
    ) {}
    
    public InterviewQuestionService(
            ChatClient.Builder chatClientBuilder,
            StructuredOutputInvoker structuredOutputInvoker,
            @Value("classpath:prompts/interview-question-system.st") Resource systemPromptResource,
            @Value("classpath:prompts/interview-question-user.st") Resource userPromptResource,
            @Value("${app.interview.follow-up-count:1}") int followUpCount) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.systemPromptTemplate = new PromptTemplate(systemPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(userPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.outputConverter = new BeanOutputConverter<>(QuestionListDTO.class);
        this.followUpCount = Math.max(0, Math.min(followUpCount, MAX_FOLLOW_UP_COUNT));
    }
    
    /**
     * 生成面试问题
     * 
     * @param resumeText 简历文本
     * @param questionCount 问题数量
     * @param historicalQuestions 历史问题列表（可选）
     * @return 面试问题列表
     */
    public List<InterviewQuestionDTO> generateQuestions(int jobId,String resumeText, int questionCount, List<String> historicalQuestions) {
        log.info("开始生成面试问题，面试岗位：{},简历长度: {},问题数量: {}, 历史问题数: {}",
            jobId, resumeText.length(), questionCount, historicalQuestions != null ? historicalQuestions.size() : 0);
        
        // 计算各类型问题数量
        QuestionDistribution distribution = calculateDistribution(questionCount,jobId);
        
        try {
            Map<String, Object> variables1 = new HashMap<>();
            // 加载用户提示词并填充变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("questionCount", questionCount);
// 按岗位类型强转并封装变量（修正后）
            if (jobId == JobConstants.BackJob) {
                BackQuestionDistribution backQuestionDistribution = (BackQuestionDistribution) distribution;
                variables1.put("jobType","后端");
                variables1.put("projectQuestionExample","针对你项目中的分布式锁实现，如何处理锁续期问题？");
                variables1.put("techChainExample","HashMap 使用 → 红黑树转换机制 → 并发安全问题");
                variables1.put("questions",QuestionUtil.BACKEND_TYPE_TABLE);
                variables1.put("counts","8");
                variables1.put("categoryExample",QuestionUtil.BACKEND_Category);

                variables.put("projectCount", backQuestionDistribution.getProject());
                variables.put("mysqlCount", backQuestionDistribution.getMysql());
                variables.put("redisCount", backQuestionDistribution.getRedis());
                variables.put("javaBasicCount", backQuestionDistribution.getJavaBasic());
                variables.put("javaCollectionCount", backQuestionDistribution.getJavaCollection());
                variables.put("javaConcurrentCount", backQuestionDistribution.getJavaConcurrent());
                variables.put("springCount", backQuestionDistribution.getSpring());
            } else if (jobId == JobConstants.FrontJob) {
                // 1. 强转为前端专属子类（替换错误的backQuestionDistribution）
                FrontQuestionDistribution frontQuestionDistribution = (FrontQuestionDistribution) distribution;
                // 2. 封装前端专属字段（对应前端的getter方法）
                variables1.put("jobType","前端");
                variables1.put("projectQuestionExample","针对你项目中 Vue 项目的性能优化，具体做了哪些落地措施？");
                variables1.put("techChainExample","Vue 组件通信使用 → 响应式原理 → 大数据渲染性能优化");
                variables1.put("questions", QuestionUtil.FRONTEND_TYPE_TABLE);
                variables1.put("counts","7");
                variables1.put("categoryExample",QuestionUtil.FRONTEND_Category);

                variables.put("projectCount", frontQuestionDistribution.getProject()); // 公共字段
                variables.put("htmlCssCount", frontQuestionDistribution.getHtmlCss());
                variables.put("jsBasicCount", frontQuestionDistribution.getJsBasic()); // 修正笔误：jaBasic → jsBasic
                variables.put("frameworkCount", frontQuestionDistribution.getFramework());
                variables.put("browserNetCount", frontQuestionDistribution.getBrowserNet());
                variables.put("engineeringCount", frontQuestionDistribution.getEngineering());
                // 前端无java/spring相关字段，删除错误的后端字段封装
            } else {
                // 1. 强转为测试专属子类（修正错误的FrontQuestionDistribution强转）
                TestQuestionDistribution testQuestionDistribution = (TestQuestionDistribution) distribution;
                // 2. 封装测试专属字段（对应测试的getter方法）
                variables1.put("jobType","测试");
                variables1.put("projectQuestionExample","针对你项目中的接口自动化测试框架，如何保证用例的稳定性和可维护性？");
                variables1.put("techChainExample","接口自动化用例编写 → 测试数据管理 → 失败用例自动重试机制");
                variables1.put("questions",QuestionUtil.TEST_TYPE_TABLE);
                variables1.put("counts","7");
                variables1.put("categoryExample",QuestionUtil.TEST_Category);

                variables.put("projectCount", testQuestionDistribution.getProject()); // 公共字段
                variables.put("caseDesignCount", testQuestionDistribution.getCaseDesign());
                variables.put("automationCount", testQuestionDistribution.getAutomation());
                variables.put("performanceCount", testQuestionDistribution.getPerformance());
                variables.put("dbCheckCount", testQuestionDistribution.getDbCheck());
                variables.put("bugManageCount", testQuestionDistribution.getBugManage());
                // 测试无java/spring相关字段，删除错误的后端字段封装
            }

            variables.put("followUpCount", followUpCount);
            variables.put("resumeText", resumeText);
            
            // 添加历史问题
            if (historicalQuestions != null && !historicalQuestions.isEmpty()) {
                String historicalText = String.join("\n", historicalQuestions);
                variables.put("historicalQuestions", historicalText);
            } else {
                variables.put("historicalQuestions", "暂无历史提问");
            }
            //加载系统提示词
            String systemPrompt = systemPromptTemplate.render(variables1);

            String userPrompt = userPromptTemplate.render(variables);


            // 添加格式指令到系统提示词
            String systemPromptWithFormat = systemPrompt + "\n\n" + outputConverter.getFormat();
            
            // 调用AI
            QuestionListDTO dto;
            try {
                dto = structuredOutputInvoker.invoke(
                    chatClient,
                    systemPromptWithFormat,
                    userPrompt,
                    outputConverter,
                    ErrorCode.INTERVIEW_QUESTION_GENERATION_FAILED,
                    "面试问题生成失败：",
                    "结构化问题生成",
                    log
                );
                log.debug("AI响应解析成功: questions count={}", dto.questions().size());
            } catch (Exception e) {
                log.error("面试问题生成AI调用失败: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.INTERVIEW_QUESTION_GENERATION_FAILED, 
                    "面试问题生成失败：" + e.getMessage());
            }
            
            // 转换为业务对象
            List<InterviewQuestionDTO> questions = convertToQuestions(dto);
            log.info("成功生成 {} 个面试问题", questions.size());
            
            return questions;
            
        } catch (Exception e) {
            log.error("生成面试问题失败: {}", e.getMessage(), e);
            // 返回默认问题集
            return generateDefaultQuestions(questionCount);
        }
    }

    /**
     * 生成面试问题（不带历史问题）
     */
    public List<InterviewQuestionDTO> generateQuestions( int jobId,String resumeText, int questionCount) {
        return generateQuestions(jobId,resumeText, questionCount, null);
    }
    
    /**
     * 计算各类型问题分布
     */
    public QuestionDistribution calculateDistribution(int total,int jobId) {
        int project = Math.max(1, (int) Math.round(total * PROJECT_RATIO));
        if(jobId== JobConstants.BackJob){
            int mysql = Math.max(1, (int) Math.round(total * Back_MYSQL_RATIO));
            int redis = Math.max(1, (int) Math.round(total * Back_REDIS_RATIO));
            int javaBasic = Math.max(1, (int) Math.round(total * Back_JAVA_BASIC_RATIO));
            int javaCollection = (int) Math.round(total * Back_JAVA_COLLECTION_RATIO);
            int javaConcurrent = (int) Math.round(total * Back_JAVA_CONCURRENT_RATIO);
            int spring = total - project - mysql - redis - javaBasic - javaCollection - javaConcurrent;
            // 确保至少有1个
            spring = Math.max(0, spring);
            return new BackQuestionDistribution(project, mysql, redis, javaBasic, javaCollection, javaConcurrent, spring);
        } else if (jobId==JobConstants.FrontJob) {
            int htmlCss = Math.max(1, (int) Math.round(total * Front_HTML_CSS_RATIO));
            int jsBasic = Math.max(1, (int) Math.round(total * Front_JS_BASIC_RATIO));
            int framework = Math.max(1, (int) Math.round(total * Front_FRAMEWORK_RATIO));
            int browserNet = (int) Math.round(total * Front_BROWSER_NET_RATIO);
            int engineering = (int) Math.round(total * Front_ENGINEERING_RATIO);
            return new FrontQuestionDistribution(project,htmlCss,jsBasic,framework,browserNet,engineering);
        }else {
            int caseDesign = Math.max(1, (int) Math.round(total * Test_CASE_DESIGN_RATIO));
            int automation = Math.max(1, (int) Math.round(total * Test_AUTOMATION_RATIO));
            int performance = Math.max(1, (int) Math.round(total * Test_PERFORMANCE_RATIO));
            int dbCheck = (int) Math.round(total * Test_DB_CHECK_RATIO);
            int bugManage = (int) Math.round(total * Test_BUG_MANAGE_RATIO);
            return new TestQuestionDistribution(project,caseDesign,automation,performance,dbCheck,bugManage);
        }
    }

    
    /**
     * 转换DTO为业务对象
     */
    private List<InterviewQuestionDTO> convertToQuestions(QuestionListDTO dto) {
        List<InterviewQuestionDTO> questions = new ArrayList<>();
        int index = 0;

        if (dto == null || dto.questions() == null) {
            return questions;
        }

        for (QuestionDTO q : dto.questions()) {
            if (q == null || q.question() == null || q.question().isBlank()) {
                continue;
            }
            QuestionType type = parseQuestionType(q.type());
            int mainQuestionIndex = index;
            questions.add(InterviewQuestionDTO.create(index++, q.question(), type, q.category(), false, null));

            List<String> followUps = sanitizeFollowUps(q.followUps());
            for (int i = 0; i < followUps.size(); i++) {
                questions.add(InterviewQuestionDTO.create(
                    index++,
                    followUps.get(i),
                    type,
                    buildFollowUpCategory(q.category(), i + 1),
                    true,
                    mainQuestionIndex
                ));
            }
        }
        
        return questions;
    }
    
    private QuestionType parseQuestionType(String typeStr) {
        try {
            return QuestionType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            return QuestionType.JAVA_BASIC;
        }
    }
    
    /**
     * 生成默认问题（备用）
     */
    private List<InterviewQuestionDTO> generateDefaultQuestions(int count) {
        List<InterviewQuestionDTO> questions = new ArrayList<>();
        
        String[][] defaultQuestions = {
            {"请介绍一下你在简历中提到的最重要的项目，你在其中承担了什么角色？", "PROJECT", "项目经历"},
            {"MySQL的索引有哪些类型？B+树索引的原理是什么？", "MYSQL", "MySQL"},
            {"Redis支持哪些数据结构？各自的使用场景是什么？", "REDIS", "Redis"},
            {"Java中HashMap的底层实现原理是什么？JDK8做了哪些优化？", "JAVA_COLLECTION", "Java集合"},
            {"synchronized和ReentrantLock有什么区别？", "JAVA_CONCURRENT", "Java并发"},
            {"Spring的IoC和AOP原理是什么？", "SPRING", "Spring"},
            {"MySQL事务的ACID特性是什么？隔离级别有哪些？", "MYSQL", "MySQL"},
            {"Redis的持久化机制有哪些？RDB和AOF的区别？", "REDIS", "Redis"},
            {"Java的垃圾回收机制是怎样的？常见的GC算法有哪些？", "JAVA_BASIC", "Java基础"},
            {"线程池的核心参数有哪些？如何合理配置？", "JAVA_CONCURRENT", "Java并发"},
        };
        
        int index = 0;
        for (int i = 0; i < Math.min(count, defaultQuestions.length); i++) {
            String mainQuestion = defaultQuestions[i][0];
            QuestionType type = QuestionType.valueOf(defaultQuestions[i][1]);
            String category = defaultQuestions[i][2];
            questions.add(InterviewQuestionDTO.create(
                index++,
                mainQuestion,
                type,
                category,
                false,
                null
            ));

            int mainQuestionIndex = index - 1;
            for (int j = 0; j < followUpCount; j++) {
                questions.add(InterviewQuestionDTO.create(
                    index++,
                    buildDefaultFollowUp(mainQuestion, j + 1),
                    type,
                    buildFollowUpCategory(category, j + 1),
                    true,
                    mainQuestionIndex
                ));
            }
        }
        
        return questions;
    }

    private List<String> sanitizeFollowUps(List<String> followUps) {
        if (followUpCount == 0 || followUps == null || followUps.isEmpty()) {
            return List.of();
        }
        return followUps.stream()
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .limit(followUpCount)
            .collect(Collectors.toList());
    }

    private String buildFollowUpCategory(String category, int order) {
        String baseCategory = (category == null || category.isBlank()) ? "追问" : category;
        return baseCategory + "（追问" + order + "）";
    }

    private String buildDefaultFollowUp(String mainQuestion, int order) {
        if (order == 1) {
            return "基于“" + mainQuestion + "”，请结合你亲自做过的一个真实场景展开说明。";
        }
        return "基于“" + mainQuestion + "”，如果线上出现异常，你会如何定位并给出修复方案？";
    }
}
