package top.lvpi.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }

    /**
     * 成功
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败
     */
    public static BaseResponse error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     */
    public static BaseResponse error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败
     */
    public static BaseResponse error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
    
    //自定义状态码
    public static BaseResponse success(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }   
} 