package top.lvpi.model.dto.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "图书查询请求")
public class DocQueryRequest {
    @Schema(description = "当前页码", defaultValue = "1")
    private long current = 1;

    @Schema(description = "每页大小", defaultValue = "10")
    private long size = 10;

    @Schema(description = "书名")
    private String title;

    @Schema(description = "作者")
    private String author;

    @Schema(description = "分类（单个类目或逗号分隔的多个类目）")
    private String category;

    @Schema(description = "分类列表（用于多选）")
    private List<String> categoryList;

    @Schema(description = "ISBN")
    private String isbn;

    @Schema(description = "出版年份")
    private String publicationYear;

    @Schema(description = "出版社")
    private String publisher;

    @Schema(description = "来源")
    private String source;

    @Schema(description = "文件类型")
    private Integer type;
} 