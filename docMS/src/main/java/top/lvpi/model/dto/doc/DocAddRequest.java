package top.lvpi.model.dto.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "添加图书请求")
public class DocAddRequest {
    
    @Schema(description = "书名", required = true)
    @NotBlank(message = "书名不能为空")
    @Size(max = 100, message = "书名长度不能超过100")
    private String title;

    @Schema(description = "副标题")
    @Size(max = 200, message = "副标题长度不能超过200")
    private String subTitle;

    @Schema(description = "作者")
    @Size(max = 50, message = "作者长度不能超过50")
    private String author;

    @Schema(description = "出版社")
    @Size(max = 50, message = "出版社长度不能超过50")
    private String publisher;

    @Schema(description = "出版年份")
    private Integer publicationYear;

    @Schema(description = "ISBN")
    @Size(max = 20, message = "ISBN长度不能超过20")
    private String isbn;

    @Schema(description = "分类")
    @Size(max = 50, message = "分类长度不能超过50")
    private String category;

    @Schema(description = "关键词")
    @Size(max = 200, message = "关键词长度不能超过200")
    private String keyWord;

    @Schema(description = "摘要")
    @Size(max = 500, message = "摘要长度不能超过500")
    private String summary;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "封面图片URL")
    private String picUrl;

    @Schema(description = "页数")
    private Integer pageSize;

    @Schema(description = "文件类型")
    @NotNull(message = "文件类型不能为空")
    private Integer type;
} 