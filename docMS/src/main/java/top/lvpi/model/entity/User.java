package top.lvpi.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@TableName("lp_user")
public class User {
    /**
     * 主键ID，使用数据库自增
     */
    @TableId(value = "user_id",type = IdType.ASSIGN_ID)
    private Long userId;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    @JsonIgnore
    private String userPassword;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户邮箱
     */
    private String userEmail;

    /**
     * 用户角色
     */
    private String userRole;

    /**
     * 用户状态
     */
    private Integer userState;

    /**
     * 登录次数
     */
    private Integer userLoginNum;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    
    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date modifiedTime;
    
    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDeleted;
} 