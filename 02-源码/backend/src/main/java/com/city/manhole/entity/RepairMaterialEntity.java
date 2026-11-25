package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * repair_material 实体（字段与库表 repair_material 一一对应）
 */
@Data
@TableName("repair_material")
public class RepairMaterialEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 工单 */
    private Long orderId;
    /** 备件 */
    private Long partId;
    /** 备件名称快照 */
    private String partName;
    /** 规格快照 */
    private String spec;
    /** 单位 */
    private String unit;
    /** 领用数量 */
    private Integer applyQty;
    /** 使用数量 */
    private Integer useQty;
    /** 退回数量 */
    private Integer returnQty;
    /** 单价 */
    private BigDecimal unitPrice;
    /** 申请人 */
    private Long applyUserId;
    /** 申请时间 */
    private LocalDateTime applyTime;
}
