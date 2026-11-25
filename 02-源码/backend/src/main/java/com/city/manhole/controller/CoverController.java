package com.city.manhole.controller;

import com.city.manhole.common.PageResult;
import com.city.manhole.common.Result;
import com.city.manhole.entity.ManholeCoverEntity;
import com.city.manhole.service.CoverService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 井盖台账接口（文档 REST：/api/manhole-repair/*）
 */
@RestController
@RequestMapping({"/api/manhole-repair", "/api/covers"})
public class CoverController {

    private final CoverService coverService;

    public CoverController(CoverService coverService) {
        this.coverService = coverService;
    }

    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String road,
                                                        @RequestParam(required = false) String coverType,
                                                        @RequestParam(required = false) String ownerUnit,
                                                        @RequestParam(required = false) String riskLevel,
                                                        @RequestParam(required = false) String coverStatus,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String lastPatrolDate) {
        return Result.ok(coverService.page(page, size, road, coverType, ownerUnit, riskLevel, coverStatus, keyword, lastPatrolDate));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(coverService.detail(id));
    }

    @PostMapping
    public Result<ManholeCoverEntity> create(@RequestBody ManholeCoverEntity cover) {
        return Result.ok("井盖已登记", coverService.save(cover));
    }

    @PutMapping("/{id}")
    public Result<ManholeCoverEntity> update(@PathVariable Long id, @RequestBody ManholeCoverEntity cover) {
        cover.setId(id);
        return Result.ok("已保存", coverService.save(cover));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        coverService.delete(id);
        return Result.ok("已删除", null);
    }

    @GetMapping("/options")
    public Result<List<ManholeCoverEntity>> options(@RequestParam(required = false) String keyword) {
        return Result.ok(coverService.options(keyword));
    }
}
