package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_menu 实体（字段与库表 sys_menu 一一对应）
 */
@Data
@TableName("sys_menu")
public class SysMenuEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 上级菜单 */
    private Long parentId;
    /** 菜单名称 */
    private String menuName;
    /** 路由地址 */
    private String path;
    /** 组件 */
    private String component;
    /** 图标标识 */
    private String icon;
    /** DIR 目录 / MENU 菜单 / BUTTON 按钮 */
    private String menuType;
    /** 权限标识 */
    private String perm;
    /** 排序 */
    private Integer sort;
    /** 是否显示 */
    private Integer visible;
    /** 1 启用 0 停用 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
}
