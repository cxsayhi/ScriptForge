package com.scriptforge.novelscript.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final int code;
    private final HttpStatus status;

    public BusinessException(String message) {
        this(400, HttpStatus.BAD_REQUEST, message);
    }

    public BusinessException(int code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
