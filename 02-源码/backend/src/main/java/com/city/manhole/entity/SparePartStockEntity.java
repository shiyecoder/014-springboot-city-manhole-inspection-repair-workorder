package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * spare_part_stock 实体（字段与库表 spare_part_stock 一一对应）
 */
@Data
@TableName("spare_part_stock")
public class SparePartStockEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 备件编号 */
    private String partNo;
    /** 备件名称 */
    private String partName;
    /** 类别：井盖/井圈/警示物料/其他 */
    private String category;
    /** 规格 */
    private String spec;
    /** 单位 */
    private String unit;
    /** 当前库存 */
    private Integer stockQty;
    /** 预警线 */
    private Integer warnQty;
    /** 单价 */
    private BigDecimal unitPrice;
    /** 库位 */
    private String location;
    /** 1 在用 0 停用 */
    private Integer status;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
