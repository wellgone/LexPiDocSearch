package top.lvpi.config;

import cn.dev33.satoken.stp.StpInterface;
import top.lvpi.model.entity.User;
import top.lvpi.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限验证接口扩展
 */
@Component
public class StpInterfaceImpl implements StpInterface {
    
    @Autowired
    private UserMapper userMapper;

    /**
     * 返回一个用户所拥有的角色标识列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<String> roles = new ArrayList<>();
        try {
            User user = userMapper.selectById(Long.parseLong(loginId.toString()));
            if (user != null) {
                // 添加用户角色
                roles.add(user.getUserRole());
                // 如果是管理员，添加admin角色
                if ("admin".equals(user.getUserRole())) {
                    roles.add("admin");
                }
            }
        } catch (Exception e) {
            // 处理异常情况
            e.printStackTrace();
        }
        return roles;
    }

    /**
     * 返回一个用户所拥有的权限码列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 暂时不实现具体的权限码
        return new ArrayList<>();
    }
} 