package top.lvpi.model.vo;

import lombok.Data;

import java.util.Date;

@Data
public class BookSectionVO {
    // BookSection字段
    private Long id;
    private Long bookId;
    private Integer pageNum;
    private String sectionText;
    private Date createTime;
    private Date modifiedTime;
    private Integer isDeleted;
    private Integer isIndexed;
    
    // Book字段
    private String title;
    private String subTitle;
    private String author;
    private String publisher;
    private Integer publicationYear;
    private String isbn;
    private String category;
    private String keyWord;
    private String bookSummary;
    private String fileName;
    private String picUrl;
} 