package com.city.manhole.controller;

import com.city.manhole.common.BizException;
import com.city.manhole.common.Result;
import com.city.manhole.common.UserContext;
import com.city.manhole.entity.SysFileEntity;
import com.city.manhole.mapper.SysFileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件上传：只允许图片/PDF，按业务类型分目录保存到项目根 uploads/，返回相对路径
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp", "pdf");

    private final SysFileMapper fileMapper;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public FileController(SysFileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(defaultValue = "COVER") String bizType,
                                              @RequestParam(required = false) Long bizId) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED.contains(ext)) {
            throw new BizException("不支持的文件类型，仅允许 jpg / png / webp / pdf");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BizException("文件超过 10MB，请压缩后上传");
        }
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String sub = bizType.toLowerCase() + "/" + month;
        File dir = new File(uploadDir, sub);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("上传目录创建失败");
        }
        String name = UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "." + ext;
        File dest = new File(dir, name);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BizException("文件保存失败，请重试");
        }
        String relative = "/uploads/" + sub + "/" + name;
        SysFileEntity entity = new SysFileEntity();
        entity.setBizType(bizType);
        entity.setBizId(bizId);
        entity.setFileName(original);
        entity.setFilePath(relative);
        entity.setFileSize(file.getSize());
        entity.setFileType(file.getContentType());
        entity.setUploaderId(UserContext.id());
        entity.setCreateTime(LocalDateTime.now());
        fileMapper.insert(entity);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", entity.getId());
        m.put("url", relative);
        m.put("fileName", original);
        m.put("size", file.getSize());
        return Result.ok("上传成功", m);
    }

    @GetMapping("/list")
    public Result<List<SysFileEntity>> list(@RequestParam String bizType, @RequestParam Long bizId) {
        return Result.ok(fileMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysFileEntity>()
                .eq(SysFileEntity::getBizType, bizType).eq(SysFileEntity::getBizId, bizId)));
    }
}
