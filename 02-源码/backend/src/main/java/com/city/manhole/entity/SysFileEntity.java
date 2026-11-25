package com.city.manhole.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * sys_file 实体（字段与库表 sys_file 一一对应）
 */
@Data
@TableName("sys_file")
public class SysFileEntity {

    @TableId(value = "id", type = IdType.AUTO)
    /** 主键 */
    private Long id;
    /** 业务类型 COVER/ISSUE/ORDER/AVATAR */
    private String bizType;
    /** 业务主键 */
    private Long bizId;
    /** 原始文件名 */
    private String fileName;
    /** 相对路径 */
    private String filePath;
    /** 字节 */
    private Long fileSize;
    /** MIME */
    private String fileType;
    /** 上传人 */
    private Long uploaderId;
    /** 上传时间 */
    private LocalDateTime createTime;
}
