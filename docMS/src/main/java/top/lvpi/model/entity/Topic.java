package top.lvpi.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

@Schema(name = "Topic", description = "主题分类表")
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("lp_topic")
public class Topic {
    
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主题ID")
    private Long id;

    @Schema(description = "主题名称")
    private String name;

    @Schema(description = "父主题ID")
    @TableField("parent_id")
    private Long parentId;

    @Schema(description = "层级")
    private Integer level;

    @Schema(description = "标签类型")
    private String type;

    @Schema(description = "创建时间")
    @TableField(value ="create_time",fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "修改时间")
    @TableField(value ="modified_time",fill = FieldFill.INSERT_UPDATE)
    private Date modifiedTime;

    @Schema(description = "是否删除")
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
} 