package com.recplatform.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recplatform.iam.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * User mapper.
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
