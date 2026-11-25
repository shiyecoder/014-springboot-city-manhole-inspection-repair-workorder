package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * risk_rule 实体（字段与库表 risk_rule 一一对应）
 */
@Data
@TableName("risk_rule")
public class RiskRuleEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 版本号 */
    private String ruleVersion;
    /** 规则名称 */
    private String ruleName;
    /** 道路等级权重（%） */
    private Integer wRoad;
    /** 缺损严重度权重（%） */
    private Integer wSeverity;
    /** 历史维修次数权重（%） */
    private Integer wRepair;
    /** 人流等级权重（%） */
    private Integer wCrowd;
    /** 未处理时长权重（%） */
    private Integer wDuration;
    /** 重复报修次数权重（%） */
    private Integer wRepeat;
    /** 重大下限 */
    private Integer redThreshold;
    /** 紧急下限 */
    private Integer yellowThreshold;
    /** 是否启用 */
    private Integer enabled;
    /** 生效时间 */
    private LocalDateTime effectTime;
    /** 发布人 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
}
