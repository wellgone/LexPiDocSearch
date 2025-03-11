package top.lvpi.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return BaseResponse.error(ErrorCode.SYSTEM_ERROR.getCode(), 
            ErrorCode.SYSTEM_ERROR.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public BaseResponse<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("File upload error: {}", e.getMessage());
        return BaseResponse.error(ErrorCode.PARAMS_ERROR, "上传文件过大，单个文件最大支持100MB");
    }
} 