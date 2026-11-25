package com.city.manhole.controller;

import com.city.manhole.common.Result;
import com.city.manhole.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 首页看板接口：一次返回指标与图表数据，不在前端二次聚合
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public Result<Map<String, Object>> overview() {
        return Result.ok(dashboardService.overview());
    }
}
