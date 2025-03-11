package top.lvpi.model.dto.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "主题导入DTO")
public class TopicImportDTO {
    @Schema(description = "主题名称")
    private String title;

    @Schema(description = "子主题列表")
    private List<TopicImportDTO> children;
} 