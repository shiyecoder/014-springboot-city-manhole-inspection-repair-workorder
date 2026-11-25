package com.city.manhole.controller;

import com.city.manhole.common.PageResult;
import com.city.manhole.common.Result;
import com.city.manhole.entity.SparePartStockEntity;
import com.city.manhole.service.PartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 备件库存接口
 */
@RestController
@RequestMapping("/api/parts")
public class PartController {

    private final PartService partService;

    public PartController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping("/page")
    public Result<PageResult<SparePartStockEntity>> page(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String category,
                                                         @RequestParam(required = false) Boolean lowStock) {
        return Result.ok(partService.page(page, size, keyword, category, lowStock));
    }

    @PostMapping
    public Result<Void> save(@RequestBody SparePartStockEntity part) {
        partService.save(part);
        return Result.ok("已保存", null);
    }

    @PostMapping("/{id}/in")
    public Result<Void> stockIn(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int qty = Integer.parseInt(body.get("qty").toString());
        partService.stockIn(id, qty, (String) body.get("remark"));
        return Result.ok("入库成功", null);
    }

    @PostMapping("/apply")
    public Result<Void> apply(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        Long partId = Long.valueOf(body.get("partId").toString());
        int qty = Integer.parseInt(body.get("qty").toString());
        partService.apply(orderId, partId, qty, (String) body.get("remark"));
        return Result.ok("领用成功，库存已扣减", null);
    }

    @GetMapping("/flows")
    public Result<List<Map<String, Object>>> flows(@RequestParam(required = false) Long partId) {
        return Result.ok(partService.flows(partId));
    }
}
