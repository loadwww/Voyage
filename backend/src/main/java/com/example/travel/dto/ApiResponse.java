package com.example.travel.dto;

public record ApiResponse<T>(boolean success, String code, T data, String message) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, "ok");
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, "INTERNAL_ERROR", null, message);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, code, null, message);
    }
}
