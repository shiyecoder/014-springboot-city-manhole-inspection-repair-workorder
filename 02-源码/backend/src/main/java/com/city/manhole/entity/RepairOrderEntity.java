package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * repair_order 实体（字段与库表 repair_order 一一对应）
 */
@Data
@TableName("repair_order")
public class RepairOrderEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 工单编号 */
    private String orderNo;
    /** 关联问题 */
    private Long issueId;
    /** 井盖 */
    private Long coverId;
    /** 维修队伍 */
    private String teamName;
    /** 维修人员 */
    private Long repairerId;
    /** 派单人 */
    private Long dispatchUserId;
    /** 要求到场时间 */
    private LocalDateTime requireArrive;
    /** 实际到场时间 */
    private LocalDateTime arriveTime;
    /** 完成时间 */
    private LocalDateTime finishTime;
    /** 安全措施 */
    private String safetyMeasure;
    /** 处理方案 */
    private String planDesc;
    /** 处理结果 */
    private String resultDesc;
    /** 维修前照片 */
    private String beforeImage;
    /** 维修后照片 */
    private String afterImage;
    /** 待接单/处理中/待验收/返工中/已完成/已作废 */
    private String orderStatus;
    /** 备件费用 */
    private BigDecimal materialCost;
    /** 返工次数 */
    private Integer reworkCount;
    /** 催办次数 */
    private Integer urgeCount;
    /** 作废原因 */
    private String cancelReason;
    /** 逻辑删除 */
    private Integer deleted;
    /** 乐观锁版本 */
    private Integer version;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
