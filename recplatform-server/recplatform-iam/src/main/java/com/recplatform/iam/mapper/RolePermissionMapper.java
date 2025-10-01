package com.recplatform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recplatform.iam.entity.RolePermissionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Role-Permission association mapper.
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermissionEntity> {
}
