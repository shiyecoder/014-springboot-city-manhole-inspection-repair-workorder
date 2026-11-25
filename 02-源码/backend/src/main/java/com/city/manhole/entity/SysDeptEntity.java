package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_dept 实体（字段与库表 sys_dept 一一对应）
 */
@Data
@TableName("sys_dept")
public class SysDeptEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 部门名称 */
    private String deptName;
    /** 部门编码 */
    private String deptCode;
    /** 类型：DEPT 科室 / TEAM 班组 / UNIT 权属单位 */
    private String deptType;
    /** 负责人 */
    private String leader;
    /** 联系电话 */
    private String phone;
    /** 排序 */
    private Integer sort;
    /** 1 启用 0 停用 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
}
