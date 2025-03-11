package top.lvpi.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户注册请求")
public class UserRegisterRequest {

    @Schema(description = "用户账号", required = true)
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 20, message = "账号长度必须在4-20之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String userAccount;

    @Schema(description = "用户密码", required = true)
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20之间")
    private String userPassword;

    @Schema(description = "确认密码", required = true)
    @NotBlank(message = "确认密码不能为空")
    private String checkPassword;

    @Schema(description = "用户名称")
    @Size(max = 50, message = "用户名称长度不能超过50")
    private String userName;

    @Schema(description = "用户邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100")
    private String userEmail;
} 