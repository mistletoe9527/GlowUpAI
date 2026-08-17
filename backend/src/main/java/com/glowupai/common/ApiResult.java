package com.glowupai.common;

/**
 * API 统一返回结构。
 *
 * @param code 业务状态码
 * @param message 业务提示
 * @param data 响应数据
 * @param <T> 响应数据类型
 */
public record ApiResult<T>(
        int code,
        String message,
        T data
) {

    /**
     * 成功状态码。
     */
    private static final int SUCCESS_CODE = 0;

    /**
     * 失败状态码。
     */
    private static final int ERROR_CODE = 500;

    /**
     * 构造普通成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(SUCCESS_CODE, "success", data);
    }

    /**
     * 构造分页成功响应。
     *
     * @param data 分页数据
     * @param <T> 分页元素类型
     * @return 分页成功响应
     */
    public static <T> ApiResult<PageResult<T>> success4Page(PageResult<T> data) {
        return new ApiResult<>(SUCCESS_CODE, "success", data);
    }

    /**
     * 构造失败响应。
     *
     * @param message 失败提示
     * @return 失败响应
     */
    public static ApiResult<Void> error(String message) {
        return new ApiResult<>(ERROR_CODE, message, null);
    }
}
