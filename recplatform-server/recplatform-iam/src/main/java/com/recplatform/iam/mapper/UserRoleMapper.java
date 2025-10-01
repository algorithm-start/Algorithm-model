package com.recplatform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recplatform.iam.entity.UserRoleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * User-Role association mapper.
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {
}
