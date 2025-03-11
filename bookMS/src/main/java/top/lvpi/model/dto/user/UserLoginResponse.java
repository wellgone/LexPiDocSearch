package top.lvpi.model.dto.user;

import top.lvpi.model.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import cn.dev33.satoken.stp.StpUtil;

@Data
@Schema(description = "用户登录响应")
public class UserLoginResponse {
    
    @Schema(description = "用户信息")
    private User user;
    
    @Schema(description = "登录token")
    private String token;
    
    @Schema(description = "token有效期（秒）")
    private long tokenTimeout;
    
    @Schema(description = "登录时间")
    private String loginTime;
    
    public UserLoginResponse(User user, String token) {
        this.user = user;
        this.token = token;
        this.tokenTimeout = StpUtil.getTokenTimeout();
        this.loginTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date());
    }
} 