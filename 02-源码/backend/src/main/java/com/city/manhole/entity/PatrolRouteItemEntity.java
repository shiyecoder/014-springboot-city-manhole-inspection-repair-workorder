package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * patrol_route_item 实体（字段与库表 patrol_route_item 一一对应）
 */
@Data
@TableName("patrol_route_item")
public class PatrolRouteItemEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 路线 */
    private Long routeId;
    /** 井盖 */
    private Long coverId;
    /** 点位顺序 */
    private Integer seq;
}
