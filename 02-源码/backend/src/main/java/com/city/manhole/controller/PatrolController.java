package com.city.manhole.controller;

import com.city.manhole.common.PageResult;
import com.city.manhole.common.Result;
import com.city.manhole.service.PatrolService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 巡检路线与任务接口
 */
@RestController
@RequestMapping("/api/patrol")
public class PatrolController {

    private final PatrolService patrolService;

    public PatrolController(PatrolService patrolService) {
        this.patrolService = patrolService;
    }

    @GetMapping("/route/page")
    public Result<PageResult<Map<String, Object>>> routes(@RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String cycleType) {
        return Result.ok(patrolService.routes(page, size, keyword, cycleType));
    }

    @PostMapping("/task/generate")
    public Result<Integer> generate(@RequestBody Map<String, Object> body) {
        String planDate = body.get("planDate") == null ? null : body.get("planDate").toString();
        Long routeId = body.get("routeId") == null ? null : Long.valueOf(body.get("routeId").toString());
        int n = patrolService.generateTasks(planDate, routeId);
        return Result.ok("已生成 " + n + " 条巡检任务", n);
    }

    @GetMapping("/task/page")
    public Result<PageResult<Map<String, Object>>> tasks(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Boolean mine,
                                                         @RequestParam(required = false) String planDate) {
        return Result.ok(patrolService.tasks(page, size, status, keyword, mine, planDate));
    }

    @GetMapping("/task/{id}")
    public Result<Map<String, Object>> taskDetail(@PathVariable Long id) {
        return Result.ok(patrolService.taskDetail(id));
    }

    @PostMapping("/task/{id}/record")
    public Result<Map<String, Object>> record(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok("已登记", patrolService.submitRecord(id,
                Long.valueOf(body.get("coverId").toString()),
                (String) body.get("checkResult"),
                (String) body.get("severity"),
                (String) body.get("imageUrl"),
                (String) body.get("remark")));
    }

    @PostMapping("/task/{id}/finish")
    public Result<Map<String, Object>> finish(@PathVariable Long id) {
        return Result.ok("任务已结束", patrolService.finish(id));
    }
}
