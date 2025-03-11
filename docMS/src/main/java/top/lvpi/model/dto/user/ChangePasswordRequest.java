package top.lvpi.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    @Schema(description = "旧密码", required = true)
    @NotBlank(message = "旧密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20之间")
    private String oldPassword;

    @Schema(description = "新密码", required = true)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20之间")
    private String newPassword;
} 