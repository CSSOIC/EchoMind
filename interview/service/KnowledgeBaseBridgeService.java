
package interview.guide.modules.interview.service;

import interview.guide.modules.knowledgebase.service.KnowledgeBaseVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库桥接服务
 * 用于面试模块与知识库向量检索模块之间的解耦和适配
 *
 * 设计目的：
 * 1. 避免面试模块直接依赖知识库的问答服务（answerQuestion）
 * 2. 提供纯粹的文档片段检索能力，而不是 AI 生成的答案
 * 3. 支持动态出题场景下的上下文知识检索
 *
 * 使用场景：
 * - 动态面试出题时，根据问题类别从知识库检索相关背景材料
 * - 为 AI 出题提供领域知识的上下文参考
 * - 基于关键词快速定位知识库中的相关内容片段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseBridgeService {

    private final KnowledgeBaseVectorService vectorService;

    /**
     * 按关键词检索 TopK 个知识片段
     * 直接从向量数据库检索原始文档片段，不经过 AI 生成答案
     *
     * @param knowledgeBaseIds 知识库 ID 列表（指定检索范围）
     * @param query 查询关键词（如"Java 集合"、"MySQL 索引"等）
     * @param topK 返回的 TopK 个结果（通常设置为 3-5 个片段）
     * @return 知识片段文本列表（每个片段约 500 tokens）
     */
    public List<String> searchTopK(List<Long> knowledgeBaseIds, String query, int topK) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            log.debug("知识库 ID 列表为空，跳过检索");
            return List.of();
        }

        if (query == null || query.trim().isBlank()) {
            log.debug("查询关键词为空，跳过检索");
            return List.of();
        }

        try {
            // 使用向量相似度搜索检索相关文档片段
            // 参数说明：
            // - query: 查询文本（会自动调用 Embedding 模型转为向量）
            // - knowledgeBaseIds: 过滤条件（只搜索指定的知识库）
            // - topK: 返回最相关的 K 个片段
            // - minScore: 最小相似度阈值 0.28（过滤低质量结果）
            List<Document> documents = vectorService.similaritySearch(
                    query.trim(),
                    knowledgeBaseIds,
                    topK,
                    0.28  // 默认最小相似度阈值
            );

            if (documents == null || documents.isEmpty()) {
                log.debug("未检索到相关知识片段：query={}, kbIds={}", query, knowledgeBaseIds);
                return List.of();
            }

            // 提取文档内容并过滤空值
            List<String> fragments = documents.stream()
                    .map(Document::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.toList());

            log.info("知识库检索完成：query={}, kbIds={}, found={} fragments",
                    query, knowledgeBaseIds, fragments.size());

            return fragments;

        } catch (Exception e) {
            log.error("知识库检索失败：query={}, kbIds={}, error={}",
                    query, knowledgeBaseIds, e.getMessage(), e);
            // 返回空列表，允许业务降级处理（不影响面试流程）
            return List.of();
        }
    }

    /**
     * 按关键词检索 TopK 个知识片段（带自定义相似度阈值）
     * 适用于需要更精细控制检索质量的场景
     *
     * @param knowledgeBaseIds 知识库 ID 列表
     * @param query 查询关键词
     * @param topK 返回数量
     * @param minScore 最小相似度阈值（0.0-1.0，建议范围 0.18-0.28）
     *                  - 短查询（≤4 字）：建议 0.18（降低阈值提高召回率）
     *                  - 中等查询（5-12 字）：建议 0.28（默认值）
     *                  - 长查询（>12 字）：建议 0.28（保持默认）
     * @return 知识片段文本列表
     */
    public List<String> searchTopKWithThreshold(
            List<Long> knowledgeBaseIds,
            String query,
            int topK,
            double minScore) {

        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            log.debug("知识库 ID 列表为空，跳过检索");
            return List.of();
        }

        if (query == null || query.trim().isBlank()) {
            log.debug("查询关键词为空，跳过检索");
            return List.of();
        }

        try {
            List<Document> documents = vectorService.similaritySearch(
                    query.trim(),
                    knowledgeBaseIds,
                    topK,
                    minScore
            );

            if (documents == null || documents.isEmpty()) {
                log.debug("未检索到相关知识片段：query={}, kbIds={}, minScore={}",
                        query, knowledgeBaseIds, minScore);
                return List.of();
            }

            List<String> fragments = documents.stream()
                    .map(Document::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.toList());

            log.info("知识库检索完成（自定义阈值）：query={}, kbIds={}, minScore={}, found={} fragments",
                    query, knowledgeBaseIds, minScore, fragments.size());

            return fragments;

        } catch (Exception e) {
            log.error("知识库检索失败：query={}, kbIds={}, error={}",
                    query, knowledgeBaseIds, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 检查知识库是否包含特定主题的内容
     * 用于快速判断是否需要在出题时引入知识库上下文
     *
     * @param knowledgeBaseIds 知识库 ID 列表
     * @param topic 主题关键词
     * @return true 表示包含相关内容，false 表示不包含或检索失败
     */
    public boolean containsTopic(List<Long> knowledgeBaseIds, String topic) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()
                || topic == null || topic.trim().isBlank()) {
            return false;
        }

        try {
            // 使用较低的阈值进行快速检查
            List<String> fragments = searchTopK(knowledgeBaseIds, topic, 1);
            boolean hasContent = !fragments.isEmpty();

            log.debug("主题检查：topic={}, found={}", topic, hasContent);
            return hasContent;

        } catch (Exception e) {
            log.warn("主题检查失败：topic={}, error={}", topic, e.getMessage());
            return false;
        }
    }
}
