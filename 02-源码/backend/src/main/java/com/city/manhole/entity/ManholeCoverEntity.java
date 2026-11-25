package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * manhole_cover 实体（字段与库表 manhole_cover 一一对应）
 */
@Data
@TableName("manhole_cover")
public class ManholeCoverEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 井盖编号 */
    private String coverNo;
    /** 设施类型（字典 cover_type） */
    private String coverType;
    /** 道路名称 */
    private String roadName;
    /** 道路等级：主干道/次干道/支路 */
    private String roadLevel;
    /** 人流等级：密集/一般/稀疏 */
    private String crowdLevel;
    /** 经度 */
    private BigDecimal longitude;
    /** 纬度 */
    private BigDecimal latitude;
    /** 位置描述 */
    private String locationDesc;
    /** 权属单位 */
    private String ownerUnit;
    /** 规格 */
    private String spec;
    /** 材质 */
    private String material;
    /** 安装日期 */
    private LocalDate installDate;
    /** 负责人 */
    private Long dutyUserId;
    /** 风险等级：重大/紧急/一般 */
    private String riskLevel;
    /** 设施状态：正常/待派单/维修中/待验收/已关闭 */
    private String coverStatus;
    /** 现场图片相对路径 */
    private String imageUrl;
    /** 备注 */
    private String remark;
    /** 所属班组 */
    private Long deptId;
    /** 逻辑删除 */
    private Integer deleted;
    /** 乐观锁版本 */
    private Integer version;
    /** 创建人 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
