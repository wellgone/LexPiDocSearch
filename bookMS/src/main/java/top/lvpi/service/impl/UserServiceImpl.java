package top.lvpi.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import top.lvpi.mapper.UserMapper;
import top.lvpi.model.dto.user.UserLoginRequest;
import top.lvpi.model.dto.user.UserRegisterRequest;
import top.lvpi.model.dto.user.UserQueryRequest;
import top.lvpi.model.dto.user.ChangePasswordRequest;
import top.lvpi.model.entity.User;
import top.lvpi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public User userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String userName = userRegisterRequest.getUserName();
        String userEmail = userRegisterRequest.getUserEmail();
        
        // 1. 校验参数
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4 || userAccount.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度应在4-20位之间");
        }
        if (userPassword.length() < 8 || userPassword.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度应在8-20位之间");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        
        // 2. 使用BCrypt加密密码
        String encryptedPassword = BCrypt.hashpw(userPassword);
        
        // 3. 账户不能重复
        long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
            .eq(User::getUserAccount, userAccount));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_ERROR, "账号已存在");
        }
        
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptedPassword);
        user.setUserName(userName != null ? userName : userAccount);
        user.setUserEmail(userEmail);
        user.setUserRole("user");
        user.setUserState(0);
        user.setUserLoginNum(0);
        
        boolean result = userMapper.insert(user) > 0;
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user;
    }

    @Override
    public User userLogin(UserLoginRequest userLoginRequest) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        
        // 1. 校验参数
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        if (userAccount.length() < 4 || userAccount.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度应在4-20位之间");
        }
        if (userPassword.length() < 8 || userPassword.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度应在8-20位之间");
        }
        
        // 2. 查询用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
            .eq(User::getUserAccount, userAccount)
            .eq(User::getIsDeleted, 0));
            
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        // 3. 使用BCrypt验证密码
        if (!BCrypt.checkpw(userPassword, user.getUserPassword())) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        if (user.getUserState() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被禁用");
        }
        
        // 4. 更新登录次数
        Date lastModifiedTime = user.getModifiedTime();
        LocalDate today = LocalDate.now();
        LocalDate lastModifiedDate = lastModifiedTime != null ? 
            lastModifiedTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;

        // 如果最后修改时间不是今天，重置登录次数为1
        if (lastModifiedDate == null || !lastModifiedDate.equals(today)) {
            user.setUserLoginNum(1);
        } else {
            // 如果是今天，递增登录次数
            user.setUserLoginNum(user.getUserLoginNum() + 1);
        }
        
        // 更新最后修改时间为当前时间
        user.setModifiedTime(new Date());
        userMapper.updateById(user);
        
        // 5. 记录登录状态
        StpUtil.login(user.getUserId());
        
        // 6. 返回脱敏后的用户信息
        user.setUserPassword(null);
        return user;
    }

    @Override
    public User getLoginUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    @Override
    public boolean userLogout() {
        try {
            StpUtil.logout();
            return true;
        } catch (Exception e) {
            log.error("登出失败", e);
            return false;
        }
    }

    @Override
    public IPage<User> listUsers(UserQueryRequest userQueryRequest) {
        // 创建查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(userQueryRequest.getUserAccount())) {
            queryWrapper.like(User::getUserAccount, userQueryRequest.getUserAccount());
        }
        if (StringUtils.isNotBlank(userQueryRequest.getUserName())) {
            queryWrapper.like(User::getUserName, userQueryRequest.getUserName());
        }
        if (StringUtils.isNotBlank(userQueryRequest.getUserEmail())) {
            queryWrapper.like(User::getUserEmail, userQueryRequest.getUserEmail());
        }
        if (StringUtils.isNotBlank(userQueryRequest.getUserRole())) {
            queryWrapper.eq(User::getUserRole, userQueryRequest.getUserRole());
        }
        if (userQueryRequest.getUserState() != null) {
            queryWrapper.eq(User::getUserState, userQueryRequest.getUserState());
        }

        // 创建分页对象
        Page<User> page = new Page<>(userQueryRequest.getCurrent(), userQueryRequest.getPageSize());

        // 执行查询
        return userMapper.selectPage(page, queryWrapper);
    }

    @Override
    public boolean deleteUser(Long id) {
        // 不能删除自己
        User loginUser = getLoginUser();
        if (loginUser.getUserId().equals(id)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能删除自己");
        }

        // 检查用户是否存在
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 执行删除
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public boolean updateUser(User user) {
        // 不能修改自己的角色
        User loginUser = getLoginUser();
        if (loginUser.getUserId().equals(user.getUserId()) && 
            !loginUser.getUserRole().equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "不能修改自己的角色");
        }

        // 检查用户是否存在
        User oldUser = userMapper.selectById(user.getUserId());
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 不允许修改密码，密码修改应该走专门的接口
        user.setUserPassword(null);

        // 执行更新
        return userMapper.updateById(user) > 0;
    }

    @Override
    public boolean changePassword(ChangePasswordRequest request) {
        // 获取当前登录用户
        User loginUser = getLoginUser();
        
        // 验证旧密码
        if (!BCrypt.checkpw(request.getOldPassword(), loginUser.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码错误");
        }
        
        // 使用BCrypt加密新密码
        String encryptedPassword = BCrypt.hashpw(request.getNewPassword());
        
        // 更新密码
        loginUser.setUserPassword(encryptedPassword);
        return userMapper.updateById(loginUser) > 0;
    }
} 