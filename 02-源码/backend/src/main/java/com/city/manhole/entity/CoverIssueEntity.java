package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * cover_issue 实体（字段与库表 cover_issue 一一对应）
 */
@Data
@TableName("cover_issue")
public class CoverIssueEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 问题编号 */
    private String issueNo;
    /** 井盖 */
    private Long coverId;
    /** 巡检发现/人工上报/市民热线 */
    private String sourceType;
    /** 缺损类型：破损/松动/丢失/沉降/异响/井圈破损 */
    private String damageType;
    /** 轻微/一般/严重 */
    private String severity;
    /** 计算时道路等级快照 */
    private String roadLevel;
    /** 计算时人流等级快照 */
    private String crowdLevel;
    /** 重大/紧急/一般 */
    private String riskLevel;
    /** 风险总分快照 */
    private Integer riskScore;
    /** 待确认/待派单/处理中/待验收/返工中/已关闭/已取消 */
    private String issueStatus;
    /** 上报人 */
    private Long reporterId;
    /** 上报时间 */
    private LocalDateTime reportTime;
    /** 现场描述 */
    private String description;
    /** 隐患照片 */
    private String imageUrl;
    /** 处置截止时间 */
    private LocalDateTime handleDeadline;
    /** 关闭时间 */
    private LocalDateTime closeTime;
    /** 是否复发井盖 */
    private Integer repeatFlag;
    /** 30 天内重复报修次数 */
    private Integer repeatCount;
    /** 历史维修次数 */
    private Integer repairCount;
    /** 取消原因 */
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
