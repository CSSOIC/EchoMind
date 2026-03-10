package interview.guide.modules.interview.Util;

public class QuestionUtil {

    // 后端类型表格
    public static final String BACKEND_TYPE_TABLE =
                    "| PROJECT | 项目经历 | 技术选型、架构设计、问题解决 |\n" +
                    "| JAVA_BASIC | Java 基础 | 面向对象、异常处理、JVM 原理 |\n" +
                    "| JAVA_COLLECTION | Java 集合 | 数据结构、源码实现、使用场景 |\n" +
                    "| JAVA_CONCURRENT | Java 并发 | 线程模型、锁机制、并发工具类 |\n" +
                    "| MYSQL | 数据库 | 索引优化、事务隔离、SQL 调优 |\n" +
                    "| REDIS | 缓存 | 数据结构、持久化、分布式锁 |\n" +
                    "| SPRING | Spring 框架 | IoC/AOP 原理、Bean 生命周期 |\n" +
                    "| SPRING_BOOT | Spring Boot | 自动配置、Starter 机制 |";
    public static final String BACKEND_Category ="PROJECT, JAVA_BASIC, JAVA_COLLECTION, JAVA_CONCURRENT, MYSQL, REDIS, SPRING, SPRING_BOOT";
    // 前端类型表格
    public static final String FRONTEND_TYPE_TABLE =
                    "| PROJECT | 项目经历 | 技术选型、架构设计、性能优化、跨端方案 |\n" +
                    "| HTML_CSS | HTML/CSS | 布局原理、盒模型、响应式、CSS3 特性、浏览器兼容性 |\n" +
                    "| JS_BASIC | JavaScript 基础 | 原型链、闭包、异步编程、作用域、类型转换 |\n" +
                    "| FRAMEWORK | 前端框架 | Vue/React 核心原理、组件通信、虚拟 DOM、状态管理 |\n" +
                    "| BROWSER_NET | 浏览器/网络 | 渲染机制、缓存策略、跨域解决方案、HTTP/HTTPS、WebSocket |\n" +
                    "| ENGINEERING | 工程化 | Webpack/Vite 配置、TS 类型系统、模块化、CI/CD、前端规范 |\n" +
                    "| PERFORMANCE | 性能优化 | 首屏加载、懒加载、大数据渲染、内存泄漏排查 |";
    public static final String FRONTEND_Category = "PROJECT, HTML_CSS, JS_BASIC, FRAMEWORK, BROWSER_NET, ENGINEERING, PERFORMANCE";    // 测试类型表格
    public static final String TEST_TYPE_TABLE =
                    "| PROJECT | 项目经历 | 测试策略、框架选型、问题定位、质量保障体系 |\n" +
                    "| CASE_DESIGN | 用例设计 | 等价类划分、边界值分析、场景法、异常场景覆盖 |\n" +
                    "| AUTOMATION | 自动化测试 | 接口自动化、UI 自动化、测试框架（Selenium/Appium/Pytest）、脚本设计 |\n" +
                    "| PERFORMANCE | 性能测试 | 压测场景设计、性能指标监控、瓶颈分析与优化 |\n" +
                    "| DB_CHECK | 数据库验证 | 测试数据准备、SQL 验证、数据一致性校验 |\n" +
                    "| BUG_MANAGE | 缺陷管理 | 缺陷生命周期、精准定位、复现步骤设计、复盘优化 |\n" +
                    "| TEST_TOOLS | 测试工具 | 接口测试工具、抓包工具、持续集成工具、缺陷管理工具 |";
    public static final String TEST_Category = "PROJECT, CASE_DESIGN, AUTOMATION, PERFORMANCE, DB_CHECK, BUG_MANAGE, TEST_TOOLS";

}
