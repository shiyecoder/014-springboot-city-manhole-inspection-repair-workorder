package com.city.manhole.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {
    private Long id;
    private String username;
    private String realName;
    private String roleCode;
    private Long deptId;
    private String teamName;

    /** 管理员 / 验收 / 调度 可见全部数据 */
    public boolean isAllScope() {
        return "ADMIN".equals(roleCode) || "DISPATCHER".equals(roleCode) || "ACCEPTOR".equals(roleCode);
    }
}
