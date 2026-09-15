package com.pet.petmarket.common;

import lombok.Getter;

/**
 * 业务异常
 *
 * <p>相比直接抛 {@link RuntimeException}，它携带明确的状态码，
 * 由 {@link GlobalExceptionHandler} 转成对应的 HTTP 状态码与统一 JSON 结构。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
