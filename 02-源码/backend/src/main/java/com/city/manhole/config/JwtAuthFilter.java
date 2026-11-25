package com.city.manhole.config;

import com.city.manhole.common.JwtUtil;
import com.city.manhole.common.LoginUser;
import com.city.manhole.common.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 解析 Authorization 头中的 JWT，写入线程上下文与 Security 上下文
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims c = jwtUtil.parse(token);
                LoginUser user = new LoginUser();
                user.setId(((Number) c.get("uid")).longValue());
                user.setUsername((String) c.get("username"));
                user.setRealName((String) c.get("realName"));
                user.setRoleCode((String) c.get("role"));
                Object dept = c.get("deptId");
                if (dept != null) {
                    user.setDeptId(((Number) dept).longValue());
                }
                UserContext.set(user);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRoleCode())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // token 失效时保持匿名，由 Security 返回 401
                UserContext.clear();
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
