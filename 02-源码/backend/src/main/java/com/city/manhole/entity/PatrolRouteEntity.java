package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * patrol_route 实体（字段与库表 patrol_route 一一对应）
 */
@Data
@TableName("patrol_route")
public class PatrolRouteEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 路线编号 */
    private String routeNo;
    /** 路线名称 */
    private String routeName;
    /** 覆盖道路 */
    private String roadName;
    /** 路段范围 */
    private String roadRange;
    /** 周期：DAY/WEEK/HALF_MONTH/MONTH */
    private String cycleType;
    /** 周期描述 */
    private String cycleDesc;
    /** 所属班组 */
    private Long deptId;
    /** 负责巡检员 */
    private Long leaderId;
    /** 井盖数量 */
    private Integer coverCount;
    /** 预计用时（分钟） */
    private Integer estimateMin;
    /** 1 启用 0 停用 */
    private Integer status;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
