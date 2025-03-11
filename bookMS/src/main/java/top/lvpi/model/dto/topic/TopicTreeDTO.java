package top.lvpi.model.dto.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@Schema(description = "主题树形结构DTO")
public class TopicTreeDTO {
    @Schema(description = "主题ID")
    private Long id;

    @Schema(description = "主题名称")
    private String name;

    @Schema(description = "父主题ID")
    private Long parentId;

    @Schema(description = "层级")
    private Integer level;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "修改时间")
    private Date modifiedTime;

    @Schema(description = "是否删除")
    private Integer isDeleted;

    @Schema(description = "子主题列表")
    private List<TopicTreeDTO> children;
} 