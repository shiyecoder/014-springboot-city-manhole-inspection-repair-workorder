package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * risk_score 实体（字段与库表 risk_score 一一对应）
 */
@Data
@TableName("risk_score")
public class RiskScoreEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 问题 */
    private Long issueId;
    /** 井盖 */
    private Long coverId;
    /** 规则 */
    private Long ruleId;
    /** 规则版本快照 */
    private String ruleVersion;
    /** 输入快照 JSON */
    private String inputSnapshot;
    /** 道路等级得分 */
    private Integer roadScore;
    /** 缺损严重度得分 */
    private Integer severityScore;
    /** 历史维修得分 */
    private Integer repairScore;
    /** 人流等级得分 */
    private Integer crowdScore;
    /** 未处理时长得分 */
    private Integer durationScore;
    /** 重复报修得分 */
    private Integer repeatScore;
    /** 总分 */
    private Integer totalScore;
    /** 重大/紧急/一般 */
    private String riskLevel;
    /** 结果解释 */
    private String explainText;
    /** 未满足条件 */
    private String unmetJson;
    /** 可改进项 */
    private String improveJson;
    /** 人工调整后等级 */
    private String adjustLevel;
    /** 人工调整原因 */
    private String adjustReason;
    /** 调整人 */
    private Long adjustUserId;
    /** 调整时间 */
    private LocalDateTime adjustTime;
    /** 计算时间 */
    private LocalDateTime createTime;
}
