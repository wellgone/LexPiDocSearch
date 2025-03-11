package top.lvpi.model.es;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
public class TopicLevel {
    @Field(name = "topic_id", type = FieldType.Long)
    private Long topicId;

    @Field(name = "parent_id", type = FieldType.Long)
    private Long parentId;

    @Field(type = FieldType.Integer)
    private Integer level;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name;

    @Field(name = "lvl0", type = FieldType.Text, analyzer = "ik_max_word")
    private String[] lvl0;

    @Field(name = "lvl1", type = FieldType.Text, analyzer = "ik_max_word")
    private String[] lvl1;

    @Field(name = "lvl2", type = FieldType.Text, analyzer = "ik_max_word")
    private String[] lvl2;

    @Field(name = "lvl3", type = FieldType.Text, analyzer = "ik_max_word")
    private String[] lvl3;

    @Field(name = "lvl4", type = FieldType.Text, analyzer = "ik_max_word")
    private String[] lvl4;
} 