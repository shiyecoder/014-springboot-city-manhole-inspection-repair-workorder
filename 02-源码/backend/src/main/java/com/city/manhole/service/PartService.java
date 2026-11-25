package com.city.manhole.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.manhole.common.BizException;
import com.city.manhole.common.PageResult;
import com.city.manhole.common.UserContext;
import com.city.manhole.entity.*;
import com.city.manhole.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 备件库存：入库、领用、退回。领用数量不得大于可用库存，任何变更都写流水（前后库存可追溯）
 */
@Service
public class PartService {

    private final SparePartStockMapper stockMapper;
    private final SparePartFlowMapper flowMapper;
    private final RepairMaterialMapper materialMapper;
    private final RepairOrderMapper orderMapper;

    public PartService(SparePartStockMapper stockMapper, SparePartFlowMapper flowMapper,
                       RepairMaterialMapper materialMapper, RepairOrderMapper orderMapper) {
        this.stockMapper = stockMapper;
        this.flowMapper = flowMapper;
        this.materialMapper = materialMapper;
        this.orderMapper = orderMapper;
    }

    public PageResult<SparePartStockEntity> page(long page, long size, String keyword, String category, Boolean lowStock) {
        LambdaQueryWrapper<SparePartStockEntity> w = new LambdaQueryWrapper<SparePartStockEntity>()
                .and(keyword != null && !keyword.isBlank(), q -> q.like(SparePartStockEntity::getPartName, keyword)
                        .or().like(SparePartStockEntity::getPartNo, keyword)
                        .or().like(SparePartStockEntity::getSpec, keyword))
                .eq(category != null && !category.isBlank(), SparePartStockEntity::getCategory, category)
                .orderByAsc(SparePartStockEntity::getPartNo);
        Page<SparePartStockEntity> p = stockMapper.selectPage(new Page<>(page, size), w);
        List<SparePartStockEntity> records = p.getRecords();
        if (Boolean.TRUE.equals(lowStock)) {
            records = records.stream().filter(r -> r.getStockQty() <= r.getWarnQty()).toList();
        }
        return new PageResult<>(p.getTotal(), page, size, records);
    }

    public void save(SparePartStockEntity part) {
        if (part.getPartName() == null || part.getPartName().isBlank()) {
            throw new BizException("备件名称不能为空");
        }
        if (part.getStockQty() == null || part.getStockQty() < 0) {
            throw new BizException("库存数量不能为负数");
        }
        if (part.getId() == null) {
            part.setPartNo(part.getPartNo() == null || part.getPartNo().isBlank()
                    ? "SP-" + String.format("%03d", stockMapper.selectCount(null) + 1) : part.getPartNo().trim());
            part.setStatus(part.getStatus() == null ? 1 : part.getStatus());
            part.setUpdateTime(LocalDateTime.now());
            stockMapper.insert(part);
        } else {
            stockMapper.updateById(part);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void stockIn(Long partId, int qty, String remark) {
        if (qty <= 0) {
            throw new BizException("入库数量必须大于 0");
        }
        SparePartStockEntity part = stockMapper.selectById(partId);
        if (part == null) {
            throw new BizException("备件不存在");
        }
        int before = part.getStockQty();
        part.setStockQty(before + qty);
        stockMapper.updateById(part);
        writeFlow(partId, "IN", qty, before, before + qty, null, remark);
    }

    /** 工单领用：库存不足直接拒绝 */
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long orderId, Long partId, int qty, String remark) {
        if (qty <= 0) {
            throw new BizException("领用数量必须大于 0");
        }
        RepairOrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("工单不存在");
        }
        if (List.of("已完成", "已作废").contains(order.getOrderStatus())) {
            throw new BizException("工单" + order.getOrderStatus() + "，不能领用备件");
        }
        SparePartStockEntity part = stockMapper.selectById(partId);
        if (part == null) {
            throw new BizException("备件不存在");
        }
        if (part.getStockQty() < qty) {
            throw new BizException("领用数量大于可用库存（当前可用 " + part.getStockQty() + " " + part.getUnit() + "）");
        }
        int before = part.getStockQty();
        part.setStockQty(before - qty);
        stockMapper.updateById(part);

        RepairMaterialEntity m = new RepairMaterialEntity();
        m.setOrderId(orderId);
        m.setPartId(partId);
        m.setPartName(part.getPartName());
        m.setSpec(part.getSpec());
        m.setUnit(part.getUnit());
        m.setApplyQty(qty);
        m.setUseQty(0);
        m.setReturnQty(0);
        m.setUnitPrice(part.getUnitPrice());
        m.setApplyUserId(UserContext.id());
        m.setApplyTime(LocalDateTime.now());
        materialMapper.insert(m);

        order.setMaterialCost(order.getMaterialCost() == null ? part.getUnitPrice().multiply(BigDecimal.valueOf(qty))
                : order.getMaterialCost().add(part.getUnitPrice().multiply(BigDecimal.valueOf(qty))));
        orderMapper.updateById(order);
        writeFlow(partId, "OUT", qty, before, before - qty, orderId, remark);
    }

    /** 工单作废时退回未使用备件 */
    @Transactional(rollbackFor = Exception.class)
    public int returnUnused(Long orderId) {
        List<RepairMaterialEntity> list = materialMapper.selectList(new LambdaQueryWrapper<RepairMaterialEntity>()
                .eq(RepairMaterialEntity::getOrderId, orderId));
        int returned = 0;
        for (RepairMaterialEntity m : list) {
            int unused = m.getApplyQty() - (m.getUseQty() == null ? 0 : m.getUseQty()) - (m.getReturnQty() == null ? 0 : m.getReturnQty());
            if (unused <= 0) {
                continue;
            }
            SparePartStockEntity part = stockMapper.selectById(m.getPartId());
            if (part == null) {
                continue;
            }
            int before = part.getStockQty();
            part.setStockQty(before + unused);
            stockMapper.updateById(part);
            m.setReturnQty((m.getReturnQty() == null ? 0 : m.getReturnQty()) + unused);
            materialMapper.updateById(m);
            writeFlow(m.getPartId(), "BACK", unused, before, before + unused, orderId, "工单作废退回");
            returned += unused;
        }
        return returned;
    }

    private void writeFlow(Long partId, String type, int qty, int before, int after, Long orderId, String remark) {
        SparePartFlowEntity f = new SparePartFlowEntity();
        f.setPartId(partId);
        f.setFlowType(type);
        f.setQty(qty);
        f.setBeforeQty(before);
        f.setAfterQty(after);
        f.setOrderId(orderId);
        f.setOperatorId(UserContext.get() == null ? null : UserContext.id());
        f.setRemark(remark);
        f.setCreateTime(LocalDateTime.now());
        flowMapper.insert(f);
    }

    public List<Map<String, Object>> flows(Long partId) {
        List<SparePartFlowEntity> list = flowMapper.selectList(new LambdaQueryWrapper<SparePartFlowEntity>()
                .eq(partId != null, SparePartFlowEntity::getPartId, partId)
                .orderByDesc(SparePartFlowEntity::getCreateTime).last("limit 50"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (SparePartFlowEntity f : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("partId", f.getPartId());
            m.put("flowType", f.getFlowType());
            m.put("qty", f.getQty());
            m.put("beforeQty", f.getBeforeQty());
            m.put("afterQty", f.getAfterQty());
            m.put("orderId", f.getOrderId());
            m.put("remark", f.getRemark());
            m.put("createTime", f.getCreateTime());
            SparePartStockEntity p = stockMapper.selectById(f.getPartId());
            m.put("partName", p == null ? "" : p.getPartName());
            out.add(m);
        }
        return out;
    }

    public List<RepairMaterialEntity> materialsOf(Long orderId) {
        return materialMapper.selectList(new LambdaQueryWrapper<RepairMaterialEntity>()
                .eq(RepairMaterialEntity::getOrderId, orderId));
    }
}
