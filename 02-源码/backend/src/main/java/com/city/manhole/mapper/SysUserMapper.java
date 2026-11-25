package com.city.manhole.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.manhole.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;

/** sys_user 数据访问 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {
}
