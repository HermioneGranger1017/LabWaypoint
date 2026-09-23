package com.itheima.interceptor;

import com.itheima.anno.RequireAdmin;
import com.itheima.anno.RequireSuperAdmin;
import com.itheima.utils.JwtUtil;
import com.itheima.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        try {
            Map<String, Object> claims = jwtUtil.parseToken(token);
            ThreadLocalUtil.set(claims);

            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                Method method = handlerMethod.getMethod();
                String role = (String) claims.get("role");

                //检查是否有@RequireSuperAdmin注解（仅超级管理员）
                if (method.isAnnotationPresent(RequireSuperAdmin.class) ||
                        method.getDeclaringClass().isAnnotationPresent(RequireSuperAdmin.class)) {
                    if (!"super_admin".equals(role)) {
                        response.setStatus(403);
                        response.getWriter().write("{\"code\":1,\"message\":\"仅超级管理员可操作\",\"data\":null}");
                        response.setContentType("application/json;charset=UTF-8");
                        return false;
                    }
                }

                //检查是否有@RequireAdmin注解（超级管理员或管理员）
                if (method.isAnnotationPresent(RequireAdmin.class) ||
                        method.getDeclaringClass().isAnnotationPresent(RequireAdmin.class)) {
                    if (!"admin".equals(role) && !"super_admin".equals(role)) {
                        response.setStatus(403);
                        response.getWriter().write("{\"code\":1,\"message\":\"仅管理员可操作\",\"data\":null}");
                        response.setContentType("application/json;charset=UTF-8");
                        return false;
                    }
                }
            }

            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalUtil.remove();
    }
}
