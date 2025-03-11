package top.lvpi.model.dto.user;

import lombok.Data;

@Data
public class UserQueryRequest {
    /**
     * 当前页码
     */
    private Integer current = 1;

    /**
     * 页面大小
     */
    private Integer pageSize = 10;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户名
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
} 