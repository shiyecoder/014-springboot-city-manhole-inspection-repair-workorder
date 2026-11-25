package com.city.manhole.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.manhole.entity.RepairOrderEntity;
import org.apache.ibatis.annotations.Mapper;

/** repair_order 数据访问 */
@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrderEntity> {
}
