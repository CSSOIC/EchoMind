# Input Data
请根据以下候选人简历内容，生成共 {questionCount} 个问题。

## 问题分布要求
| 类型 | 数量 | 说明 |
|------|------|------|
| 项目经历 (PROJECT) | {projectCount} 题 | 基于简历中的具体项目深度提问 |
| MySQL | {mysqlCount} 题 | 索引、事务、SQL 优化等 |
| Redis | {redisCount} 题 | 数据结构、缓存策略、分布式锁等 |
| Java 基础 (JAVA_BASIC) | {javaBasicCount} 题 | 面向对象、JVM、异常处理等 |
| Java 集合 (JAVA_COLLECTION) | {javaCollectionCount} 题 | 集合框架、源码原理等 |
| Java 并发 (JAVA_CONCURRENT) | {javaConcurrentCount} 题 | 线程、锁、并发工具类等 |
| Spring/Spring Boot | {springCount} 题 | IoC/AOP、自动配置等 |
| HTML/CSS | {htmlCssCount} 题 | 布局、盒模型、响应式、CSS3 特性等 |
| JS 基础 (JS_BASIC) | {jsBasicCount} 题 | 原型、闭包、异步、作用域等 |
| 前端框架 (FRAMEWORK) | {frameworkCount} 题 | Vue/React 核心原理、组件通信、性能优化等 |
| 浏览器/网络 (BROWSER_NET) | {browserNetCount} 题 | 浏览器渲染、缓存、跨域、HTTP/HTTPS 等 |
| 工程化 (ENGINEERING) | {engineeringCount} 题 | Webpack/Vite、TS、模块化、CI/CD 等 |
| 用例设计 (CASE_DESIGN) | {caseDesignCount} 题 | 等价类、边界值、场景法等设计思路 |
| 自动化测试 (AUTOMATION) | {automationCount} 题 | 接口自动化、UI 自动化、测试框架等 |
| 性能测试 (PERFORMANCE) | {performanceCount} 题 | 压测、瓶颈分析、性能指标等 |
| 数据库验证 (DB_CHECK) | {dbCheckCount} 题 | 测试数据准备、SQL 验证、数据一致性等 |
| 缺陷管理 (BUG_MANAGE) | {bugManageCount} 题 | 缺陷生命周期、定位、复盘等 |
## 候选人简历
---简历内容开始---
{resumeText}
---简历内容结束---

## 历史提问记录
以下是该候选人已经回答过的问题列表，请在生成新问题时**尽可能避免重复**（除非是需要从不同维度深度追问同一主题）：
---历史提问开始---
{historicalQuestions}
---历史提问结束---

## 输出要求
请严格按照 JSON 格式输出问题列表，直接输出 JSON 对象，不要包含任何 Markdown 代码块标签。
