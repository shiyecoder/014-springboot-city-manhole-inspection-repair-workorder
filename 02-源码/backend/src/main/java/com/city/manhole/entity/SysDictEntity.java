package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * sys_dict 实体（字段与库表 sys_dict 一一对应）
 */
@Data
@TableName("sys_dict")
public class SysDictEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 字典类型 */
    private String dictType;
    /** 显示名 */
    private String dictLabel;
    /** 字典值 */
    private String dictValue;
    /** 排序 */
    private Integer sort;
    /** 1 启用 0 停用 */
    private Integer status;
}
