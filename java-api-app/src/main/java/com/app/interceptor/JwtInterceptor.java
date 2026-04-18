package com.app.interceptor;

import com.app.service.JwtService;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import io.jsonwebtoken.Claims;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts2.ServletActionContext;

import java.util.HashMap;
import java.util.Map;

public class JwtInterceptor extends AbstractInterceptor {

    private final JwtService jwtService = new JwtService();

    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", "Authorization header with Bearer token required");
            ActionContext.getContext().getValueStack().set("responseData", errorData);
            return "unauthorized";
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.validateToken(token);

            // Store claims in the request so actions can access them
            request.setAttribute("jwtClaims", claims);
            request.setAttribute("userId", claims.getSubject());
            request.setAttribute("userEmail", claims.get("email", String.class));
            request.setAttribute("userName", claims.get("name", String.class));

        } catch (Exception e) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", "Invalid or expired token: " + e.getMessage());
            ActionContext.getContext().getValueStack().set("responseData", errorData);
            return "unauthorized";
        }

        return invocation.invoke();
    }
}
