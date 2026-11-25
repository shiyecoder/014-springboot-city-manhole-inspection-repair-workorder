package com.city.manhole.common;

/**
 * 线程内保存当前登录用户，供 Service 做数据范围判断
 */
public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            throw new BizException(401, "登录已失效，请重新登录");
        }
        return u;
    }

    public static Long id() {
        return require().getId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
