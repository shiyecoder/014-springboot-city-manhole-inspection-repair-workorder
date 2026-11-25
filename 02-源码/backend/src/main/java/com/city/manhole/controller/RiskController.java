package com.city.manhole.controller;

import com.city.manhole.common.PageResult;
import com.city.manhole.common.Result;
import com.city.manhole.entity.RiskRuleEntity;
import com.city.manhole.entity.RiskScoreEntity;
import com.city.manhole.service.RiskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 创新点接口：规则配置、风险计算、结果解释、人工修正、效果统计
 */
@RestController
@RequestMapping("/api/risk")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/rule/active")
    public Result<RiskRuleEntity> active() {
        return Result.ok(riskService.activeRule());
    }

    @GetMapping("/rules")
    public Result<List<RiskRuleEntity>> rules() {
        return Result.ok(riskService.rules());
    }

    @PostMapping("/rules")
    public Result<RiskRuleEntity> publish(@RequestBody RiskRuleEntity rule) {
        return Result.ok("新规则版本已发布，仅影响后续计算", riskService.publishRule(rule));
    }

    @PostMapping("/evaluate/{issueId}")
    public Result<RiskScoreEntity> evaluate(@PathVariable Long issueId) {
        return Result.ok("计算完成", riskService.evaluate(issueId));
    }

    @GetMapping("/score/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String level,
                                                        @RequestParam(required = false) String version) {
        return Result.ok(riskService.page(page, size, keyword, level, version));
    }

    @GetMapping("/score/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(riskService.detail(id));
    }

    @PostMapping("/score/{id}/adjust")
    public Result<Void> adjust(@PathVariable Long id, @RequestBody Map<String, String> body) {
        riskService.adjust(id, body.get("level"), body.get("reason"));
        return Result.ok("已记录人工调整，原始计算结果保留", null);
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(riskService.stats());
    }
}
