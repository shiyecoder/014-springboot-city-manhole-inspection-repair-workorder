package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * acceptance_record 实体（字段与库表 acceptance_record 一一对应）
 */
@Data
@TableName("acceptance_record")
public class AcceptanceRecordEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 工单 */
    private Long orderId;
    /** 问题 */
    private Long issueId;
    /** 验收人 */
    private Long acceptorId;
    /** 合格/不合格 */
    private String checkResult;
    /** 路面恢复 */
    private Integer surfaceOk;
    /** 编号标识 */
    private Integer signOk;
    /** 围挡撤除 */
    private Integer barrierOk;
    /** 周边清洁 */
    private Integer cleanOk;
    /** 验收评分 */
    private Integer score;
    /** 说明 */
    private String remark;
    /** 返工原因 */
    private String reworkReason;
    /** 验收时间 */
    private LocalDateTime acceptTime;
}
