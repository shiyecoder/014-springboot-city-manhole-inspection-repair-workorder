package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * spare_part_flow 实体（字段与库表 spare_part_flow 一一对应）
 */
@Data
@TableName("spare_part_flow")
public class SparePartFlowEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 备件 */
    private Long partId;
    /** IN 入库 / OUT 领用 / BACK 退回 */
    private String flowType;
    /** 数量（正数） */
    private Integer qty;
    /** 变更前库存 */
    private Integer beforeQty;
    /** 变更后库存 */
    private Integer afterQty;
    /** 关联工单 */
    private Long orderId;
    /** 操作人 */
    private Long operatorId;
    /** 备注 */
    private String remark;
    /** 时间 */
    private LocalDateTime createTime;
}
