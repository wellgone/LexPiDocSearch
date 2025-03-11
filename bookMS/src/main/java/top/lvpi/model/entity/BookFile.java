package top.lvpi.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

@Schema(name = "LpBookFile", description = "书籍文件关联表")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@TableName("lp_book_file")
public class BookFile {
    
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "书籍ID")
    @TableField("book_id")
    private Long bookId;

    @Schema(description = "文件ID")
    @TableField("file_id")
    private Long fileId;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @Schema(description = "最后更新时间")
    @TableField(value = "modified_time", fill = FieldFill.INSERT_UPDATE)
    private Date modifiedTime;

    @Schema(description = "是否已删除")
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;
} 