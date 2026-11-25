package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_user 实体（字段与库表 sys_user 一一对应）
 */
@Data
@TableName("sys_user")
public class SysUserEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 登录名 */
    private String username;
    /** BCrypt 密码 */
    private String password;
    /** 姓名 */
    private String realName;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 头像相对路径 */
    private String avatar;
    /** 部门 */
    private Long deptId;
    /** 角色 */
    private Long roleId;
    /** 所属队伍（维修人员） */
    private String teamName;
    /** 1 启用 0 停用 */
    private Integer status;
    /** 连续登录失败次数 */
    private Integer failCount;
    /** 锁定截止时间 */
    private LocalDateTime lockTime;
    /** 最近登录时间 */
    private LocalDateTime lastLogin;
    /** 逻辑删除 */
    private Integer deleted;
    /** 版本号 */
    private Integer version;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
