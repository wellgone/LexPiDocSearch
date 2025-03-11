package top.lvpi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.lvpi.common.BaseResponse;
import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import top.lvpi.model.dto.user.UserLoginRequest;
import top.lvpi.model.dto.user.UserRegisterRequest;
import top.lvpi.model.dto.user.UserQueryRequest;
import top.lvpi.model.entity.User;
import top.lvpi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.dev33.satoken.stp.StpUtil;
import top.lvpi.model.dto.user.UserLoginResponse;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import top.lvpi.model.dto.user.ChangePasswordRequest;

@Tag(name = "用户管理", description = "用户相关接口")
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "用户注册", description = "注册新用户")
    @PostMapping("/register")
    public BaseResponse<User> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        try {
            User user = userService.userRegister(userRegisterRequest);
            user.setUserPassword(null);
            return new BaseResponse<>(0, user, "注册成功！欢迎 " + user.getUserName());
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "用户登录", description = "用户登录并返回用户信息和token")
    @PostMapping("/login")
    public BaseResponse<UserLoginResponse> userLogin(@RequestBody UserLoginRequest userLoginRequest) {
        if (userLoginRequest == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        try {
            // 调用登录服务
            User user = userService.userLogin(userLoginRequest);
            // 获取token
            String token = StpUtil.getTokenValue();
            // 构建登录响应
            UserLoginResponse loginResponse = new UserLoginResponse(user, token);
            return new BaseResponse<>(0, loginResponse, 
                String.format("登录成功！欢迎回来，%s。这是您第 %d 次登录。", 
                    user.getUserName(), user.getUserLoginNum()));
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "获取当前用户", description = "获取当前登录用户信息")
    @GetMapping("/current")
    @SaCheckLogin
    public BaseResponse<User> getCurrentUser() {
        User loginUser = userService.getLoginUser();
        loginUser.setUserPassword(null);
        return BaseResponse.success(loginUser);
    }

    @Operation(summary = "用户登出", description = "退出登录")
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout() {
        boolean result = userService.userLogout();
        return BaseResponse.success(result);
    }

    @Operation(summary = "判断是否为管理员", description = "检查当前用户是否具有管理员权限")
    @GetMapping("/admin")
    public BaseResponse<Boolean> isAdmin() {
        boolean result = userService.isAdmin();
        return BaseResponse.success(result);
    }

    @Operation(summary = "获取用户列表", description = "分页获取用户列表")
    @GetMapping("/list")
    @SaCheckRole("admin")
    public BaseResponse<IPage<User>> listUsers(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        try {
            IPage<User> userPage = userService.listUsers(userQueryRequest);
            // 脱敏处理
            userPage.getRecords().forEach(user -> user.setUserPassword(null));
            return BaseResponse.success(userPage);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "删除用户", description = "根据ID删除用户")
    @DeleteMapping("/delete/{id}")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> deleteUser(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "ID不合法");
        }
        try {
            boolean result = userService.deleteUser(id);
            return BaseResponse.success(result);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "更新用户", description = "更新用户信息")
    @PutMapping("/update")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> updateUser(@RequestBody User user) {
        if (user == null || user.getUserId() == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        try {
            boolean result = userService.updateUser(user);
            return BaseResponse.success(result);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "修改密码", description = "修改用户密码")
    @PostMapping("/change-password")
    @SaCheckLogin
    public BaseResponse<Boolean> changePassword(@RequestBody ChangePasswordRequest request) {
        if (request == null || StringUtils.isAnyBlank(request.getOldPassword(), request.getNewPassword())) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        try {
            boolean result = userService.changePassword(request);
            return BaseResponse.success(result);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }
} 