package com.vector.mallproduct.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vector.common.utils.PageUtils;
import com.vector.common.utils.Query;
import com.vector.mallproduct.dao.AttrAttrgroupRelationDao;
import com.vector.mallproduct.dao.AttrDao;
import com.vector.mallproduct.dao.AttrGroupDao;
import com.vector.mallproduct.dao.CategoryDao;
import com.vector.mallproduct.entity.AttrAttrgroupRelationEntity;
import com.vector.mallproduct.entity.AttrEntity;
import com.vector.mallproduct.entity.AttrGroupEntity;
import com.vector.mallproduct.entity.CategoryEntity;
import com.vector.mallproduct.service.AttrAttrgroupRelationService;
import com.vector.mallproduct.service.AttrService;
import com.vector.mallproduct.service.CategoryService;
import com.vector.mallproduct.vo.AttrGroupRelationVo;
import com.vector.mallproduct.vo.AttrRespVo;
import com.vector.mallproduct.vo.AttrVo;
import com.vector.common.constant.AttrEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {
    @Resource
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Resource
    private AttrGroupDao attrGroupDao;

    @Resource
    private CategoryDao categoryDao;

    @Resource
    private CategoryService categoryService;

    @Resource
    private AttrAttrgroupRelationService attrAttrgroupRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        // 1.??????????????????
        this.save(attrEntity);
        // 2.??????????????????
        if (attr.getAttrType() == AttrEnum.Attr_TYPE_BASE.getCode() && attr.getAttrId() != null) {
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrGroupId(attr.getAttrGroupId());
            attrAttrgroupRelationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationDao.insert(attrAttrgroupRelationEntity);
        }

    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catalogId, String type) {
        String key = (String) params.get("key");
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                .eq("attr_type", "base".equalsIgnoreCase(type)
                        ? AttrEnum.Attr_TYPE_BASE.getCode()
                        : AttrEnum.Attr_TYPE_SALE.getCode())
                .eq(catalogId != 0, "catalog_id", catalogId)
                .and(StringUtils.isNotBlank(key), (wrapper) -> {
                    wrapper.eq("attr_id", key)
                            .or()
                            .like("attr_name", key);
                });
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params), queryWrapper

        );
        // ??????????????????,???????????????
        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> respVos = records.stream().map(attrEntity -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);

            // 1.??????????????????????????????  ???????????? -> ??????????????? -> ???????????????
            if ("base".equalsIgnoreCase(type)) {
                AttrAttrgroupRelationEntity attrId = attrAttrgroupRelationDao
                        .selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>()
                                .eq("attr_id", attrEntity.getAttrId()));
                if (attrId != null && attrId.getAttrGroupId() != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrId.getAttrGroupId());
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatalogId());
            if (Optional.ofNullable(categoryEntity).isPresent()) {
                attrRespVo.setCatalogName(categoryEntity.getName());
            }
            return attrRespVo;
        }).collect(Collectors.toList());
        pageUtils.setList(respVos);
        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrEntity attrEntity = this.getById(attrId);
        AttrRespVo respVo = new AttrRespVo();
        BeanUtils.copyProperties(attrEntity, respVo);

        if (attrEntity.getAttrType() == AttrEnum.Attr_TYPE_BASE.getCode()) {
            // 1.????????????
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = attrAttrgroupRelationDao
                    .selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>()
                            .eq("attr_id", attrEntity.getAttrId()));
            if (Optional.ofNullable(attrAttrgroupRelationEntity).isPresent()) {
                Long attrGroupId = attrAttrgroupRelationEntity.getAttrGroupId();
                respVo.setAttrGroupId(attrGroupId);

                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrGroupId);
                if (Optional.ofNullable(attrGroupEntity).isPresent()) {
                    respVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }


        // 2.????????????
        Long catalogId = attrEntity.getCatalogId();
        Long[] catalogPath = categoryService.findCatalogPath(catalogId);
        respVo.setCatalogPath(catalogPath);

        CategoryEntity categoryEntity = categoryDao.selectById(catalogId);
        if (Optional.ofNullable(categoryEntity).isPresent()) {
            respVo.setCatalogName(categoryEntity.getName());
        }
        return respVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);
        if (attrEntity.getAttrType() == AttrEnum.Attr_TYPE_BASE.getCode()) {
            // 1.??????????????????
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrGroupId(attr.getAttrGroupId());
            attrAttrgroupRelationEntity.setAttrId(attr.getAttrId());
            attrAttrgroupRelationService.saveOrUpdate(attrAttrgroupRelationEntity,
                    new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
        }

    }

    /**
     * ????????????id?????????????????????????????????
     *
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> attrAttrgroupRelationEntities = attrAttrgroupRelationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>()
                        .eq("attr_group_id", attrgroupId));

        List<Long> attrIds = attrAttrgroupRelationEntities.stream()
                .map(AttrAttrgroupRelationEntity::getAttrId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(attrIds)) {
            return null;
        }
        return this.listByIds(attrIds);
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] attrGroupRelationVos) {
        // 1.????????????
        List<AttrAttrgroupRelationEntity> entities = Arrays.stream(attrGroupRelationVos).map((item) -> {
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, attrAttrgroupRelationEntity);
            return attrAttrgroupRelationEntity;
        }).collect(Collectors.toList());
        attrAttrgroupRelationDao.deleteBatchRelation(entities);

    }

    /**
     * ???????????????????????????????????????
     *
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        // 1.?????????????????????????????????????????????????????????     ??????????????????id??????????????????(??????,??????,??????,??????)
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catalogId = attrGroupEntity.getCatalogId();
        // 2.???????????????????????????????????????????????????(??????????????????????????????: ??????...,cpu...,??????...,??????...)
        // 2.1 ????????????????????????????????????id (??????,????????????...)
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(
                new QueryWrapper<AttrGroupEntity>().eq("catalog_id", catalogId));
        List<Long> attrGroupIds = attrGroupEntities.stream()
                .map(AttrGroupEntity::getAttrGroupId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(attrGroupIds)) {
            return null;
        }

        // 2.2 ???????????????????????????????????????(??????????????????????????????????????????)
        List<AttrAttrgroupRelationEntity> attrAttrgroupRelationEntities = attrAttrgroupRelationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", attrGroupIds)
        );
        List<Long> attrIds = attrAttrgroupRelationEntities.stream()
                .map(AttrAttrgroupRelationEntity::getAttrId)
                .collect(Collectors.toList());

        // 2.3 ???????????????????????????????????????????????????
        QueryWrapper<AttrEntity> queryWrapper =
                new QueryWrapper<AttrEntity>().eq("catalog_id", catalogId).eq("attr_type", AttrEnum.Attr_TYPE_BASE.getCode());
        if (CollectionUtils.isNotEmpty(attrIds)) {
            queryWrapper.notIn("attr_id", attrIds);
        }
        String key = (String) params.get("key");
        if (StringUtils.isNotBlank(key)) {
            queryWrapper.and(wrapper -> {
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), queryWrapper);
        return new PageUtils(page);
    }

    @Override
    public List<AttrEntity> selectSearchAttrs(List<Long> attrIds) {
        List<AttrEntity> attrEntities = baseMapper.selectList(new QueryWrapper<AttrEntity>()
                .in(CollectionUtils.isNotEmpty(attrIds), "attr_id", attrIds)
                .nested(w -> w
                        .apply("1=1")
                        .and(i -> i.eq("search_type", 1))));
        return attrEntities;
    }


}
