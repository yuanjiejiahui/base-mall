package io.renren.modules.app.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.renren.modules.app.entity.loginUserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSecurityTestDao extends BaseMapper<loginUserEntity> {

}
