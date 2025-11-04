package com.fyh.aop;

import com.fyh.annotation.AuthCheck;
import com.fyh.exception.BusinessException;
import com.fyh.exception.ErrorCode;
import com.fyh.model.User;
import com.fyh.model.enums.UserRoleEnum;
import com.fyh.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes =RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        //当前 用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        //不需要权限,放行
        if(mustRoleEnum == null)
        {
            return joinPoint.proceed();
        }
        //以下为需要权限
        //获取当前用户具有的权限
        UserRoleEnum currentUserRole = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        //没有权限，拒绝
        if(currentUserRole == null)
        {
            throw  new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //要求必须有管理员权限，用户没有权限，拒绝
        if(UserRoleEnum.ADMIN.equals(mustRoleEnum)&& !UserRoleEnum.ADMIN.equals(currentUserRole))
        {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //通过权限校验，放行
        return joinPoint.proceed();

    }
}
