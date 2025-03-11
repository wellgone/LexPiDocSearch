package top.lvpi.config;

import cn.dev33.satoken.exception.NotLoginException;
import top.lvpi.common.BaseResponse;
import top.lvpi.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandlerConfig {

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED) // 设置HTTP状态码为401
    public BaseResponse handleNotLoginException(NotLoginException e) {
        return BaseResponse.error(ErrorCode.NOT_LOGIN_ERROR, "未携带token，请登录后重试");
    }
} 