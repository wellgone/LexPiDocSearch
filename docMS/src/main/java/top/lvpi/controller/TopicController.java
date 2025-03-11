package top.lvpi.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.lvpi.common.BaseResponse;
import top.lvpi.common.ResultUtils;
import top.lvpi.model.dto.topic.TopicImportDTO;
import top.lvpi.model.dto.topic.TopicPathDTO;
import top.lvpi.model.dto.topic.TopicTreeDTO;
import top.lvpi.model.entity.Topic;
import top.lvpi.service.TopicService;
import top.lvpi.service.DocTopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 主题管理
 * 注意：
 * 1. 主题的parentId为null、level为100时，表示该topics是书籍的自定义标签
 * 2. 主题的parentId为null、level为101时，表示该topics是书籍的书籍类型标签
 * 3. 主题的parentId为null、level为102时，表示该topics是书籍的实务专题标签
 * 4. 主题的parentId为null、level为103时，表示该topics是书籍的其他分类标签
 * 5. 主题的parentId不为null时，表示该topics是书籍带层级关系的主题标签
 */

@RestController
@RequestMapping("/topic")
@Tag(name = "主题管理", description = "主题相关接口")
public class TopicController {
    private static final Logger logger = LoggerFactory.getLogger(TopicController.class);

    @Autowired
    private TopicService topicService;

    @Autowired
    private DocTopicService docTopicService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "创建主题")
    public BaseResponse<Long> createTopic(@RequestBody Topic topic) {
        topicService.save(topic);
        return ResultUtils.success(topic.getId());
    }

    @PostMapping("/import")
    @Operation(summary = "批量导入主题，仅支持带层级的主题")
    public BaseResponse<Long> importTopics(@RequestBody String jsonString) {
        try {
            // 1. 移除可能存在的外层引号
            if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
                jsonString = jsonString.substring(1, jsonString.length() - 1);
            }
            
            // 2. 预处理JSON字符串
            jsonString = cleanJsonString(jsonString);
            
            logger.debug("处理后的JSON字符串: {}", jsonString);
            
            // 3. 解析为DTO对象
            TopicImportDTO topicImportDTO = objectMapper.readValue(jsonString, TopicImportDTO.class);
            
            // 4. 导入主题
            return ResultUtils.success(topicService.importTopics(topicImportDTO));
        } catch (Exception e) {
            logger.error("JSON解析错误: {}", jsonString, e);
            throw new IllegalArgumentException("JSON格式错误: " + e.getMessage());
        }
    }

    /**
     * 清理JSON字符串，将中文标点转换为英文标点
     */
    private String cleanJsonString(String json) {
        if (json == null) {
            return null;
        }
        
        // 处理转义字符
        json = json.replace("\\r", "")
                  .replace("\\n", "")
                  .replace("\\", "");
        
        return json.replace('\u201C', '"')  // 替换中文左双引号
                  .replace('\u201D', '"')   // 替换中文右双引号
                  .replace('\u2018', '\'')  // 替换中文左单引号
                  .replace('\u2019', '\'')  // 替换中文右单引号
                  .replace('：', ':')       // 替换中文冒号
                  .replace('，', ',')       // 替换中文逗号
                  .replace('【', '[')       // 替换中文方括号
                  .replace('】', ']')
                  .replace('｛', '{')       // 替换中文花括号
                  .replace('｝', '}')
                  .replace('（', '(')       // 替换中文圆括号
                  .replace('）', ')')
                  .replace('、', ',')       // 替换顿号为逗号
                  .replace("<EOL>", "")     // 移除EOL标记
                  .replaceAll("\\s+", " ")  // 替换多个空白字符为单个空格
                  .trim();                  // 移除首尾空白
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除主题，包括自定义标签、主题标签")
    public BaseResponse<Boolean> deleteTopic(@PathVariable Long id) {
        return ResultUtils.success(topicService.removeById(id));
    }

    @PutMapping
    @Operation(summary = "更新主题，包括自定义标签、主题标签")
    public BaseResponse<Boolean> updateTopic(@RequestBody Topic topic) {
        return ResultUtils.success(topicService.updateById(topic));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取主题详情（包含父级主题）,仅支持带层级的主题")
    public BaseResponse<List<Topic>> getTopic(@PathVariable Long id) {
        return ResultUtils.success(topicService.getTopicWithParents(id));
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有主题，不包括自定义标签，仅支持带层级的主题")
    public BaseResponse<List<Topic>> listTopics(@RequestParam(required = false) String name,
                                                @RequestParam(required = false) Integer level,
                                                @RequestParam(required = false) Long parentId) {
        // 构建查询条件
        QueryWrapper<Topic> queryWrapper = new QueryWrapper<>();
        if (level != null) {
            queryWrapper.eq("level", level);
        }else{
            queryWrapper.lt("level", 100);
        }   
        queryWrapper.like(name != null, "name", name)
                    .eq(parentId != null, "parent_id", parentId)
                    .orderByAsc("level");

        List<Topic> topics = topicService.list(queryWrapper);
        return ResultUtils.success(topics);
    }

    @GetMapping("/tags")
    @Operation(summary = "获取所有自定义标签，支持分页和条件查询")
    public BaseResponse<IPage<Topic>> listTags(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer level,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false, defaultValue = "create_time") String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        // 构建分页对象
        Page<Topic> page = new Page<>(current, pageSize);
        
        // 构建查询条件
        QueryWrapper<Topic> queryWrapper = new QueryWrapper<>();
        
        // 添加查询条件
        if (name != null && !name.trim().isEmpty()) {
            queryWrapper.like("name", name.trim());
        }
        
        // 处理标签类型查询
        if (level != null) {
            queryWrapper.eq("level", level);
        } else {
            // 只查询自定义标签（level >= 100）
            queryWrapper.ge("level", 100);
        }
        
        // 确保parentId为0（不带层级的标签）
        queryWrapper.eq("parent_id", 0);
        
        // 添加排序
        if ("asc".equalsIgnoreCase(sortOrder)) {
            queryWrapper.orderByAsc(sortField);
        } else {
            queryWrapper.orderByDesc(sortField);
        }
        
        // 执行分页查询
        IPage<Topic> topicPage = topicService.page(page, queryWrapper);
        return ResultUtils.success(topicPage);
    }

    @PostMapping("/doc/{docId}/topic/{topicId}")
    @Operation(summary = "为书籍添加主题标签、自定义标签")
    public BaseResponse<Long> addDocTopic(@PathVariable Long docId, @PathVariable Long topicId) {
        return ResultUtils.success(docTopicService.addDocTopic(docId, topicId));
    }

    @DeleteMapping("/doc/{docId}/topic/{topicId}")
    @Operation(summary = "删除书籍的指定主题标签、自定义标签")
    public BaseResponse<Boolean> removeDocTopic(@PathVariable Long docId, @PathVariable Long topicId) {
        return ResultUtils.success(docTopicService.removeDocTopic(docId, topicId));
    }

    @DeleteMapping("/doc/{docId}/topics")
    @Operation(summary = "删除书籍的所有主题标签、自定义标签")
    public BaseResponse<Boolean> removeAllDocTopics(@PathVariable Long docId) {
        return ResultUtils.success(docTopicService.removeAllDocTopics(docId));
    }

    @GetMapping("/doc/{docId}")
    @Operation(summary = "获取书籍的所有主题及其层级关系（树形结构）")
    public BaseResponse<List<TopicTreeDTO>> getDocTopics(@PathVariable Long docId) {
        return ResultUtils.success(topicService.getTopicTreeByDocId(docId));
    }

    @GetMapping("/doc/{docId}/paths")
    @Operation(summary = "获取书籍的所有主题及其层级关系（格式化）")
    public BaseResponse<List<TopicPathDTO>> getDocTopicPaths(@PathVariable Long docId) {
        return ResultUtils.success(topicService.getDocTopicPaths(docId));
    }
    
    @GetMapping("/doc/{docId}/tags")
    @Operation(summary = "获取书籍的所有自定义标签，不包括主题标签")
    public BaseResponse<List<Topic>> getDocTopicTags(@PathVariable Long docId) {
        return ResultUtils.success(topicService.getDocTopicTags(docId));
    }

    @PostMapping("/tag/import")
    @Operation(summary = "批量导入自定义标签")
    public BaseResponse<Integer> importTags(@RequestBody List<String> tags, @RequestParam(defaultValue = "100") Integer level) {
        // 验证level值是否合法
        if (level < 100 || level > 103) {
            throw new IllegalArgumentException("标签类型不合法，必须在100-103之间");
        }
        
        int count = 0;
        for (String tagName : tags) {
            Topic topic = new Topic();
            topic.setName(tagName);
            topic.setLevel(level);
            topic.setParentId(null);
            if (topicService.save(topic)) {
                count++;
            }
        }
        return ResultUtils.success(count);
    }

    @GetMapping("/types")
    @Operation(summary = "获取所有标签类型")
    public BaseResponse<List<Map<String, Object>>> getTopicTypes() {
        // 从数据库中查询所有不同的标签类型
        QueryWrapper<Topic> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("DISTINCT type, level")
                    .isNotNull("type")
                    .orderByAsc("level");
        
        List<Topic> topics = topicService.list(queryWrapper);
        
        // 转换为前端需要的格式
        List<Map<String, Object>> types = topics.stream()
            .map(topic -> {
                Map<String, Object> type = new HashMap<>();
                type.put("value", topic.getType());
                type.put("label", topic.getType());
                type.put("level", topic.getLevel());
                return type;
            })
            .collect(Collectors.toList());
        
        // 如果没有数据，返回默认的标签类型
        if (types.isEmpty()) {
            types = Arrays.asList(
                createTypeMap("通用标签", "自定义标签", 100)
            );
        }
        
        return ResultUtils.success(types);
    }

    private Map<String, Object> createTypeMap(String value, String label, Integer level) {
        Map<String, Object> type = new HashMap<>();
        type.put("value", value);
        type.put("label", label);
        type.put("level", level);
        return type;
    }


    @PostMapping("/doc/{docId}/parse-opac")
    @Operation(summary = "解析书籍OPAC主题词并建立关联")
    public BaseResponse<Boolean> parseDocOPACTopics(@PathVariable Long docId, @RequestParam String topicContent) {
        logger.info("解析书籍[{}]的OPAC主题词: {}", docId, topicContent);
        boolean result = topicService.parseDocOPACTopics(docId, topicContent);
        return ResultUtils.success(result);
    }

} 