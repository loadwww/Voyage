package com.example.travel.exception;

public class NotFoundException extends BusinessException {
    public NotFoundException(String resource, long id) {
        super(ErrorCode.NOT_FOUND, resource + " 不存在: " + id);
    }

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
