package com.city.manhole.controller;

import com.city.manhole.common.PageResult;
import com.city.manhole.common.Result;
import com.city.manhole.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 维修工单接口：派发、接单、安全措施、提交结果、作废、催办
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String team,
                                                        @RequestParam(required = false) Boolean mine,
                                                        @RequestParam(required = false) Boolean overdue,
                                                        @RequestParam(required = false) Boolean pendingFirst) {
        return Result.ok(orderService.page(page, size, keyword, status, team, mine, overdue, pendingFirst));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @PostMapping("/dispatch")
    public Result<?> dispatch(@RequestBody Map<String, Object> body) {
        return Result.ok("已派发", orderService.dispatch(
                Long.valueOf(body.get("issueId").toString()),
                (String) body.get("teamName"),
                Long.valueOf(body.get("repairerId").toString()),
                (String) body.get("requireArrive"),
                (String) body.get("safetyMeasure"),
                (String) body.get("planDesc")));
    }

    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        orderService.accept(id);
        return Result.ok("已接单", null);
    }

    @PostMapping("/{id}/safety")
    public Result<Void> safety(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.registerSafety(id, body.get("safetyMeasure"), body.get("beforeImage"));
        return Result.ok("安全措施已登记", null);
    }

    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.submitResult(id, body.get("resultDesc"), body.get("afterImage"));
        return Result.ok("已提交，等待验收", null);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.cancel(id, body.get("reason"));
        return Result.ok("工单已作废，未使用备件已退回", null);
    }

    @PostMapping("/{id}/urge")
    public Result<Void> urge(@PathVariable Long id) {
        orderService.urge(id);
        return Result.ok("已发送催办", null);
    }
}
