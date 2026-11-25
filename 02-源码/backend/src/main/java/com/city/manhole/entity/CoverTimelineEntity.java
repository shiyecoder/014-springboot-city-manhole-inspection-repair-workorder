package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * cover_timeline 实体（字段与库表 cover_timeline 一一对应）
 */
@Data
@TableName("cover_timeline")
public class CoverTimelineEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 井盖 */
    private Long coverId;
    /** 问题 */
    private Long issueId;
    /** 工单 */
    private Long orderId;
    /** 事件类型 */
    private String eventType;
    /** 变更前状态 */
    private String fromStatus;
    /** 变更后状态 */
    private String toStatus;
    /** 说明 */
    private String content;
    /** 操作人 */
    private Long operatorId;
    /** 操作人姓名 */
    private String operatorName;
    /** 时间 */
    private LocalDateTime createTime;
}
