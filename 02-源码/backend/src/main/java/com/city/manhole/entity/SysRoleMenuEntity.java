package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * sys_role_menu 实体（字段与库表 sys_role_menu 一一对应）
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenuEntity {
    /** 角色 */
    private Long roleId;
    /** 菜单 */
    private Long menuId;
}
