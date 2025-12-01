package com.fyh.common;

import com.fyh.api.imagesearch.model.ImageSearchResult;
import com.fyh.exception.ErrorCode;
import com.fyh.model.vo.SpaceLevel;

import java.util.List;

public class ResultUtils {

    /**
     * 成功
     *
     * @param <T>  数据类型
     * @param data 数据
     * @param s
     * @return 响应
     */
    public static <T> BaseResponse<T> success(T data, String s) {
        return new BaseResponse<>(0, data, "ok");
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data);
    }
    /**
     * 失败
     *
     * @param errorCode 错误码
     * @return 响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     *
     * @param code    错误码
     * @param message 错误信息
     * @return 响应
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败
     *
     * @param errorCode 错误码
     * @return 响应
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }

    public static BaseResponse<Boolean> success(boolean b) {
        return new BaseResponse<>(0, b, "ok");
    }

    public static BaseResponse<List<SpaceLevel>> success(List<SpaceLevel> spaceLevelList) {
//        return new BaseResponse<>(0, spaceLevelList, "ok");
        return ResultUtils.success(spaceLevelList, "ok");
    }


//    public static BaseResponse<List<ImageSearchResult>> success(List<ImageSearchResult> resultList) {
//        return ResultUtils.success(resultList);
//    }
}
