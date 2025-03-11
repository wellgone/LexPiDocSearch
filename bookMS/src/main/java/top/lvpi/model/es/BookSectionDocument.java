package top.lvpi.model.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Data
@Document(indexName = "books")
public class BookSectionDocument {
    @Id
    private String id;

    @Field(name = "@timestamp", type = FieldType.Date)
    private Date timestamp;

    @Field(name = "@version", type = FieldType.Text)
    private String version;

    @Field(type = FieldType.Keyword)
    private String author;

    @Field(name = "book_id", type = FieldType.Keyword)
    private String bookId;

    @Field(name = "book_title", type = FieldType.Text, analyzer = "ik_max_word")
    private String bookTitle;

    @Field(name = "file_name", type = FieldType.Keyword)
    private String fileName;

    @Field(type = FieldType.Keyword)
    private String isbn;

    @Field(name = "page_num", type = FieldType.Integer)
    private Integer pageNum;

    @Field(name = "pic_url", type = FieldType.Text)
    private String picUrl;

    @Field(name = "publication_year", type = FieldType.Integer)
    private Integer publicationYear;

    @Field(type = FieldType.Keyword)
    private String publisher;

    @Field(name = "section_text", type = FieldType.Text, analyzer = "ik_max_word")
    private String sectionText;

    @Field(type = FieldType.Nested)
    private TopicLevel topicLevels;

    @Field(name = "series", type = FieldType.Text, analyzer = "ik_max_word")
    private String series;

    @Field(name = "tags", type = FieldType.Text, analyzer = "ik_max_word")
    private String[] tags;

    @Field(name = "topic_series", type = FieldType.Text, analyzer = "ik_max_word")
    private String topicSeries;

    @Field(name = "opac_series", type = FieldType.Text, analyzer = "ik_max_word")
    private String opacSeries;

    @Field(name = "type", type = FieldType.Integer)
    private Integer type;

    @Field(name = "category", type = FieldType.Keyword)
    private String category;


} 