package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * patrol_record 实体（字段与库表 patrol_record 一一对应）
 */
@Data
@TableName("patrol_record")
public class PatrolRecordEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 任务 */
    private Long taskId;
    /** 井盖 */
    private Long coverId;
    /** 完好/松动/破损/丢失/沉降/异响 */
    private String checkResult;
    /** 轻微/一般/严重 */
    private String severity;
    /** 现场图片 */
    private String imageUrl;
    /** 说明 */
    private String remark;
    /** 巡检员 */
    private Long inspectorId;
    /** 巡检时间 */
    private LocalDateTime checkTime;
}
