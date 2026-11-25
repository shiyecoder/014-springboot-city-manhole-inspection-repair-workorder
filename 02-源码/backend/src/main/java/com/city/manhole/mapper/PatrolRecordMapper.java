package com.city.manhole.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.manhole.entity.PatrolRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/** patrol_record 数据访问 */
@Mapper
public interface PatrolRecordMapper extends BaseMapper<PatrolRecordEntity> {
}
