package com.city.manhole.controller;

import com.city.manhole.common.PageResult;
import com.city.manhole.common.Result;
import com.city.manhole.service.AcceptanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 验收接口
 */
@RestController
@RequestMapping("/api/acceptances")
public class AcceptanceController {

    private final AcceptanceService acceptanceService;

    public AcceptanceController(AcceptanceService acceptanceService) {
        this.acceptanceService = acceptanceService;
    }

    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Boolean pending) {
        return Result.ok(acceptanceService.page(page, size, status, keyword, pending));
    }

    @PostMapping("/{orderId}/submit")
    public Result<Map<String, Object>> submit(@PathVariable Long orderId, @RequestBody Map<String, Object> body) {
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        boolean surfaceOk = Boolean.TRUE.equals(body.get("surfaceOk"));
        boolean signOk = Boolean.TRUE.equals(body.get("signOk"));
        boolean barrierOk = Boolean.TRUE.equals(body.get("barrierOk"));
        boolean cleanOk = Boolean.TRUE.equals(body.get("cleanOk"));
        Integer score = body.get("score") == null ? null : Integer.valueOf(body.get("score").toString());
        String remark = (String) body.get("remark");
        return Result.ok(pass ? "验收合格，问题已关闭" : "已判定返工",
                acceptanceService.submit(orderId, pass, surfaceOk, signOk, barrierOk, cleanOk, score, remark));
    }

    @GetMapping("/{orderId}/records")
    public Result<List<?>> records(@PathVariable Long orderId) {
        return Result.ok(acceptanceService.records(orderId));
    }
}
