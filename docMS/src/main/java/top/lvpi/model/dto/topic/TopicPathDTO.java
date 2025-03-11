package top.lvpi.model.dto.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "主题路径DTO")
public class TopicPathDTO {
    @Schema(description = "主题ID")
    private Long topicId;

    @Schema(description = "主题级别")
    private Integer levelSize;

    @Schema(description = "格式化的主题路径，例如：'劳动法 > 劳动合同'")
    private String path;
} 