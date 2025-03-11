package top.lvpi.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@TableName("lp_book_section")
public class BookSection {

    @TableId(type = IdType.AUTO)
    @Schema(description = "章节ID", name = "id", type = "Long")
    private Long id;

    @TableField(value = "book_id")
    @Schema(description = "图书ID", name = "bookId", type = "Long")
    private Long bookId;

    @TableField(value = "page_num")
    @Schema(description = "页码", name = "pageNum", type = "Integer")
    private Integer pageNum;

    @TableField(value = "title")
    @Schema(description = "文档名称", name = "title", type = "String")
    private String title;

    @TableField(value = "section_text")
    @Schema(description = "章节内容", name = "sectionText", type = "String")
    private String content;

    //创建时间
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间", name = "createTime", type = "Date")
    private Date createTime;
    //最后更新时间
    @TableField(value = "modified_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "最后更新时间", name = "modifiedTime", type = "Date")
    private Date modifiedTime;
    //是否删除
    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    @Schema(description = "是否删除", name = "isDelete", type = "Integer")
    private Integer isDeleted;

} 