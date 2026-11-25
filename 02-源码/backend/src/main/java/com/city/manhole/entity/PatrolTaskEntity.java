package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * patrol_task 实体（字段与库表 patrol_task 一一对应）
 */
@Data
@TableName("patrol_task")
public class PatrolTaskEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 任务编号 */
    private String taskNo;
    /** 路线 */
    private Long routeId;
    /** 计划日期 */
    private LocalDate planDate;
    /** 计划开始 HH:mm */
    private String planStart;
    /** 巡检员 */
    private Long inspectorId;
    /** 待执行/进行中/已完成/已逾期 */
    private String taskStatus;
    /** 点位数 */
    private Integer totalCount;
    /** 已完成点位 */
    private Integer doneCount;
    /** 异常点位数 */
    private Integer abnormalCount;
    /** 完成时间 */
    private LocalDateTime finishTime;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
