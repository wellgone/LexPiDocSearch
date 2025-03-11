package top.lvpi.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 创建时间
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        // 修改时间
        this.strictInsertFill(metaObject, "modifiedTime", Date.class, new Date());
        // 是否删除
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
        // 是否索引
        this.strictInsertFill(metaObject, "isIndexed", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时只需要更新修改时间
        this.strictUpdateFill(metaObject, "modifiedTime", Date.class, new Date());
    }
} 