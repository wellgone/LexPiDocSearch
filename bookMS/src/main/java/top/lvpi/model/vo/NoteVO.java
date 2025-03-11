package top.lvpi.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "笔记视图对象")
public class NoteVO {
    @Schema(description = "笔记ID")
    private Long id;

    @Schema(description = "笔记内容")
    private String content;

    @Schema(description = "来源书籍名称")
    private String sourceName;

    @Schema(description = "出版社")
    private String sourcePress;

    @Schema(description = "作者")
    private String sourceAuthor;

    @Schema(description = "出版日期")
    private LocalDate sourcePublicationDate;

    @Schema(description = "页码")
    private Integer sourcePageSize;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime modifiedTime;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "父级笔记ID")
    private Long parentId;

    @Schema(description = "来源URL")
    private String sourceUrl;

    @Schema(description = "排序字段")
    private Integer orderNum;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "子笔记列表")
    private List<NoteVO> children;
} 