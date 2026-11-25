package com.city.manhole.controller;

import com.city.manhole.common.PageResult;
import com.city.manhole.common.Result;
import com.city.manhole.entity.CoverIssueEntity;
import com.city.manhole.service.IssueService;
import com.city.manhole.service.RiskService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 问题上报接口
 */
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService issueService;
    private final RiskService riskService;

    public IssueController(IssueService issueService, RiskService riskService) {
        this.issueService = issueService;
        this.riskService = riskService;
    }

    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String damageType,
                                                        @RequestParam(required = false) String riskLevel,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String source,
                                                        @RequestParam(required = false) String beginDate,
                                                        @RequestParam(required = false) String endDate,
                                                        @RequestParam(defaultValue = "false") boolean mine) {
        return Result.ok(issueService.page(page, size, keyword, damageType, riskLevel, status, source, beginDate, endDate, mine));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(issueService.detail(id));
    }

    @PostMapping
    public Result<CoverIssueEntity> create(@RequestBody Map<String, Object> body) {
        CoverIssueEntity issue = new CoverIssueEntity();
        if (body.get("coverId") != null) {
            issue.setCoverId(Long.valueOf(body.get("coverId").toString()));
        }
        issue.setDamageType((String) body.get("damageType"));
        issue.setSeverity((String) body.get("severity"));
        issue.setDescription((String) body.get("description"));
        issue.setImageUrl((String) body.get("imageUrl"));
        issue.setSourceType((String) body.get("sourceType"));
        boolean force = Boolean.TRUE.equals(body.get("force"));
        return Result.ok("上报成功，已自动计算风险等级", issueService.create(issue, force));
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        issueService.confirm(id);
        return Result.ok("已确认，可进入派单", null);
    }

    @PostMapping("/{id}/transition")
    public Result<Void> transition(@PathVariable Long id, @RequestBody Map<String, String> body) {
        issueService.transition(id, body.get("target"), body.get("reason"));
        return Result.ok("状态已更新", null);
    }

    @PostMapping("/{id}/evaluate")
    public Result<?> evaluate(@PathVariable Long id) {
        return Result.ok("已重新计算", riskService.evaluate(id));
    }
}
