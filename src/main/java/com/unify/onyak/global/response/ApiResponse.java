package com.unify.onyak.global.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.http.HttpStatus;
import org.jspecify.annotations.Nullable;

public record ApiResponse<T> (
        @JsonIgnore
        HttpStatus status,

        boolean success,

        @Nullable
        T data,

        @Nullable String message
) {

    public static <T> ApiResponse<T> ok(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.OK, true, data, null);
    }

    public static <T> ApiResponse<T> created(@Nullable final T data) {
        return new ApiResponse<>(HttpStatus.CREATED, true, data, null);
    }

    public static <T> ApiResponse<T> of(final HttpStatus status, @Nullable final T data) {
        return new ApiResponse<>(status, true, data, null);
    }

    public static <T> ApiResponse<T> of(final HttpStatus status, @Nullable final T data, final String message) {
        return new ApiResponse<>(status, true, data, message);
    }
}
