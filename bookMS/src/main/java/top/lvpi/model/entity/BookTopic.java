package top.lvpi.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

@Schema(name = "BookTopic", description = "书籍与主题关联表")
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("lp_book_topic")
public class BookTopic {
    
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "书籍与主题关联表ID")
    private Long id;

    @Schema(description = "书籍ID")
    @TableField("book_id")
    private Long bookId;

    @Schema(description = "主题ID")
    @TableField("topic_id")
    private Long topicId;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "修改时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date modifiedTime;

    @Schema(description = "是否删除")
    @TableLogic
    private Integer isDeleted;
} 