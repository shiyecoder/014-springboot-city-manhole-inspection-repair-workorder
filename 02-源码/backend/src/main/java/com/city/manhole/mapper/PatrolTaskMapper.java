package com.city.manhole.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.manhole.entity.PatrolTaskEntity;
import org.apache.ibatis.annotations.Mapper;

/** patrol_task 数据访问 */
@Mapper
public interface PatrolTaskMapper extends BaseMapper<PatrolTaskEntity> {
}
