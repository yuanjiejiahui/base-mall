package com.vector.mallcoupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vector.common.utils.PageUtils;
import com.vector.common.utils.Query;
import com.vector.mallcoupon.dao.HomeAdvDao;
import com.vector.mallcoupon.entity.HomeAdvEntity;
import com.vector.mallcoupon.service.HomeAdvService;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("homeAdvService")
public class HomeAdvServiceImpl extends ServiceImpl<HomeAdvDao, HomeAdvEntity> implements HomeAdvService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<HomeAdvEntity> page = this.page(
                new Query<HomeAdvEntity>().getPage(params),
                new QueryWrapper<HomeAdvEntity>()
        );

        return new PageUtils(page);
    }

}
