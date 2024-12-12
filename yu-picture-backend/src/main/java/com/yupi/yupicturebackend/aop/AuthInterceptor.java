package com.yupi.yupicturebackend.aop;

import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.UserRoleEnum;
import com.yupi.yupicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.condition.RequestConditionHolder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 权限验证拦截器
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable{
        // 1.获取上下文请求request
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 2.获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 3.获取当前要求验证的权限枚举
        String mustRole = authCheck.mustRole();
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 3.1如果不需要权限验证，直接放行
        if(mustRoleEnum == null){
            return joinPoint.proceed();
        }
        // 3.2需要权限验证：获取当前用户权限
        String userRole = loginUser.getUserRole();
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userRole);
        // 3.2.1 当前用户权限无权限，异常
        if(userRoleEnum == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 3.2.2 当前用户权限与要求权限不一致(要求管理员权限，单用户没有管理员权限)，报异常
        if(UserRoleEnum.ADMIN.equals(mustRoleEnum) && !userRoleEnum.equals(mustRoleEnum)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 3.2.3 其他，放行
        return joinPoint.proceed();
    }
}
