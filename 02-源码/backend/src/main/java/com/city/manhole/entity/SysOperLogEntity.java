package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_oper_log 实体（字段与库表 sys_oper_log 一一对应）
 */
@Data
@TableName("sys_oper_log")
public class SysOperLogEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 操作人 */
    private Long userId;
    /** 操作人姓名 */
    private String userName;
    /** 模块 */
    private String module;
    /** 动作 */
    private String action;
    /** 方法 */
    private String method;
    /** 入参摘要 */
    private String params;
    /** IP */
    private String ip;
    /** 耗时毫秒 */
    private Long costMs;
    /** 是否成功 */
    private Integer success;
    /** 异常信息 */
    private String errorMsg;
    /** 时间 */
    private LocalDateTime createTime;
}
