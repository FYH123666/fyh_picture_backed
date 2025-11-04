package com.fyh.controller;

import com.fyh.annotation.AuthCheck;
import com.fyh.common.BaseResponse;
import com.fyh.common.ResultUtils;
import com.fyh.constant.UserConstant;
import com.fyh.exception.ErrorCode;
import com.fyh.exception.ThrowUtils;
import com.fyh.model.User;
import com.fyh.model.dto.user.UserLoginRequest;
import com.fyh.model.dto.user.UserRegisterRequest;
import com.fyh.model.vo.LoginUserVO;
import com.fyh.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Api(tags = "用户接口")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    @ApiOperation("用户注册")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest)
    {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    @ApiOperation("用户登录")


    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request)
    {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount= userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO result=userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(result);
    }
    @GetMapping("/get/login")
    @ApiOperation("获取当前登录用户")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation("用户登出")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request)
    {
        ThrowUtils.throwIf(request==null,ErrorCode.OPERATION_ERROR);
        boolean result=userService.userLogout(request);
        return ResultUtils.success(result);
    }

}
