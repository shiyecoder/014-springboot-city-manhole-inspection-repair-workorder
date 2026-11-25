package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_role 实体（字段与库表 sys_role 一一对应）
 */
@Data
@TableName("sys_role")
public class SysRoleEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 角色名称 */
    private String roleName;
    /** 角色编码 */
    private String roleCode;
    /** 数据范围 ALL/DEPT/SELF */
    private String dataScope;
    /** 说明 */
    private String remark;
    /** 1 启用 0 停用 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
}
