package com.vector.mallproduct.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vector.common.utils.PageUtils;
import com.vector.common.utils.Query;
import com.vector.mallproduct.dao.AttrGroupDao;
import com.vector.mallproduct.entity.AttrEntity;
import com.vector.mallproduct.entity.AttrGroupEntity;
import com.vector.mallproduct.service.AttrGroupService;
import com.vector.mallproduct.service.AttrService;
import com.vector.mallproduct.vo.AttrGroupWithAttrsVo;
import com.vector.mallproduct.vo.SpuItemAttrGroupVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Resource
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catalogId) {
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        wrapper.and(StringUtils.isNotBlank(key), (obj) -> {
                    obj.eq("attr_group_id", key)
                            .or()
                            .like("attr_group_name", key);
                })
                .eq(catalogId == 0, "catalog_id", catalogId);
        IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    /**
     * ????????????id?????????????????????????????????????????????
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrgroupWithAttrsByCatalogId(Long catlogId) {
        // 1.??????????????????
        List<AttrGroupEntity> attrGroupEntities =
                this.list(new QueryWrapper<AttrGroupEntity>()
                        .eq("catalog_id", catlogId));
        // 2.??????????????????
        List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().map(item -> {
            AttrGroupWithAttrsVo attrGroupWithAttrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(item, attrGroupWithAttrsVo);
            List<AttrEntity> attrs = attrService.getRelationAttr(attrGroupWithAttrsVo.getAttrGroupId());
            attrGroupWithAttrsVo.setAttrs(attrs);
            return attrGroupWithAttrsVo;
        }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(collect)) {
            return null;
        }
        return collect;
    }

    /**
     * ????????????spu????????????????????????????????????????????????????????????????????????????????????
     */
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        // 1.??????spuId????????????????????????pms_product_attr_value???
        // 2.??????attrId???????????????????????????pms_attr_attrgroup_relation???
        // 3.??????attrGroupId + catalogId???????????????????????????pms_attr_group???
        return baseMapper.getAttrGroupWithAttrsBySpuId(spuId, catalogId);
    }

}
