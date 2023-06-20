package com.hmdp.utils;

import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //1. 判断是否需要拦截【ThreadLocal中是否存在用户】
        if (UserHolder.getUser() == null) {
            //没有用户 需要拦截，设置状态码
            response.setStatus(401);
            return false;
        }
        //有用户 放行
        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户，不然可能会OOM
        UserHolder.removeUser();
    }
}
