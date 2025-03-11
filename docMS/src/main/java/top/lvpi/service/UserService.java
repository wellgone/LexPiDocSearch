package top.lvpi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.lvpi.model.dto.user.UserLoginRequest;
import top.lvpi.model.dto.user.UserRegisterRequest;
import top.lvpi.model.dto.user.UserQueryRequest;
import top.lvpi.model.dto.user.ChangePasswordRequest;
import top.lvpi.model.entity.User;

public interface UserService {
    /**
     * 用户注册
     * @return 注册成功的用户信息（脱敏）
     */
    User userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     */
    User userLogin(UserLoginRequest userLoginRequest);

    /**
     * 获取当前登录用户
     */
    User getLoginUser();

    /**
     * 用户登出
     */
    boolean userLogout();

    /**
     * 判断是否为管理员
     */
    default boolean isAdmin() {
        User user = getLoginUser();
        return "admin".equals(user.getUserRole());
    }

    /**
     * 获取用户列表
     */
    IPage<User> listUsers(UserQueryRequest userQueryRequest);

    /**
     * 删除用户
     */
    boolean deleteUser(Long id);

    /**
     * 更新用户
     */
    boolean updateUser(User user);

    /**
     * 修改密码
     * @param request 修改密码请求
     * @return 是否成功
     */
    boolean changePassword(ChangePasswordRequest request);
} 