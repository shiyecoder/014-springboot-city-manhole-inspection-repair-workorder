package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_message 实体（字段与库表 sys_message 一一对应）
 */
@Data
@TableName("sys_message")
public class SysMessageEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 接收人 */
    private Long userId;
    /** 标题 */
    private String title;
    /** 内容 */
    private String content;
    /** 类型 TODO/OVERDUE/REJECT/ACCEPT/SYSTEM */
    private String msgType;
    /** 业务编号 */
    private String bizNo;
    /** 详情跳转地址 */
    private String link;
    /** 0 未读 1 已读 */
    private Integer readFlag;
    /** 创建时间 */
    private LocalDateTime createTime;
}
