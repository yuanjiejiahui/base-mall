package com.vector.mallproduct.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.vector.common.to.SkuHasStockTo;
import com.vector.common.to.SpuBoundTo;
import com.vector.common.to.es.SkuEsModel;
import com.vector.common.utils.PageUtils;
import com.vector.common.utils.Query;
import com.vector.common.utils.R;
import com.vector.mallproduct.dao.SpuInfoDao;
import com.vector.mallproduct.entity.*;
import com.vector.mallproduct.openfeign.CouponOpenFeinService;
import com.vector.mallproduct.openfeign.SearchOpenFeignService;
import com.vector.mallproduct.openfeign.WareOpenFeinService;
import com.vector.mallproduct.service.*;
import com.vector.mallproduct.vo.*;
import com.vector.common.constant.StatusEnum;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Resource
    private SpuInfoDescService spuInfoDescService;
    @Resource
    private SpuImagesService spuImagesService;
    @Resource
    private ProductAttrValueService productAttrValueService;
    @Resource
    private AttrService attrService;
    @Resource
    private SkuInfoService skuInfoService;
    @Resource
    private CouponOpenFeinService couponOpenFeinService;
    @Resource
    private BrandService brandService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private WareOpenFeinService wareOpenFeinService;
    @Resource
    private SearchOpenFeignService searchOpenFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    // TODO ????????????????????????????????????
    public void saveSpuInfo(SpuSaveVo vo) {
        // 1. ??????spu???????????? pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);

        Long id = spuInfoEntity.getId();
        // 2. ??????spu??????????????? pms_spu_info_desc
        List<String> decript = vo.getDecript();
        spuInfoDescService.saveSpuInfoDesc(id, decript);
        // 3. ??????spu???????????? pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveImages(id, images);
        // 4. ??????spu???????????????: pms_prduct_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        productAttrValueService.saveProductAttrValue(id, baseAttrs);
        // 5. ??????spu???????????? sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(id);
        if (spuBoundTo.getBuyBounds().compareTo(new BigDecimal("0")) > 0 ||
                spuBoundTo.getGrowBounds().compareTo(new BigDecimal("0")) > 0) {
            R r = couponOpenFeinService.saveSpuBounds(spuBoundTo);
            if (r.getCode() != 0) {
                log.error("????????????spu??????????????????!");
            }
        }

        // 5. ????????????spu?????????sku??????
        List<Skus> skus = vo.getSkus();
        skuInfoService.saveSkuInfo(spuInfoEntity, skus);

    }

    @Override
    public PageUtils queryPageByCondition(SpuInfoFindRagneVo spuInfoFindRagneVo) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        wrapper.nested(w -> w.apply("1=1").eq(StringUtils.isNotBlank(spuInfoFindRagneVo.getKey()),
                                "id", spuInfoFindRagneVo.getKey()).or()
                        .like(StringUtils.isNotBlank(spuInfoFindRagneVo.getKey()),
                                "spu_name", spuInfoFindRagneVo.getKey()))
                .eq(StringUtils.isNotBlank(spuInfoFindRagneVo.getBrandId()) && !"0".equalsIgnoreCase(spuInfoFindRagneVo.getBrandId()),
                        "brand_id", spuInfoFindRagneVo.getBrandId())
                .eq(StringUtils.isNotBlank(spuInfoFindRagneVo.getCatalogId()) && !"0".equalsIgnoreCase(spuInfoFindRagneVo.getCatalogId()),
                        "catalog_id", spuInfoFindRagneVo.getCatalogId())
                .eq(StringUtils.isNotBlank(spuInfoFindRagneVo.getStatus()),
                        "publish_status", spuInfoFindRagneVo.getStatus());
        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(new HashMap<>()), wrapper);
        return new PageUtils(page);
    }

    @Override
    public void up(Long spuId) {
        // 1.????????????spuId???????????????sku??????
        List<SkuInfoEntity> skuInfoEntities = skuInfoService.getSkusBySpuId(spuId);
        List<Long> skuIds = skuInfoEntities.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        //  ????????????sku?????????????????????????????????????????????
        List<ProductAttrValueEntity> baseAttrListforspu = productAttrValueService.baseAttrListforspu(spuId);
        List<Long> attrIds = baseAttrListforspu.stream().map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());
        List<AttrEntity> attrEntities = attrService.selectSearchAttrs(attrIds);
        Set<Long> searchAttrIds = attrEntities.stream().map(AttrEntity::getAttrId).collect(Collectors.toSet());
        List<SkuEsModel.Attrs> attrs = baseAttrListforspu.stream().filter(i -> searchAttrIds.contains(i.getAttrId())).map(item -> {
            SkuEsModel.Attrs attrs1 = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs1);
            return attrs1;
        }).collect(Collectors.toList());

        // ??????????????????,???????????????????????????
        Map<Long, Boolean> stockMap = null;
        try {
            R r = wareOpenFeinService.getSkuHasStock(skuIds);
            TypeReference<List<SkuHasStockTo>> typeReference = new TypeReference<List<SkuHasStockTo>>() {
            };
            stockMap = r.getData(typeReference).stream()
                    .collect(Collectors.toMap(SkuHasStockTo::getSkuId, SkuHasStockTo::getHasStock));
        } catch (Exception e) {
            log.error("????????????????????????: ??????{}", e);
        }
        Map<Long, Boolean> finalStockMap = stockMap;
        // 2.???????????????sku??????
        List<SkuEsModel> upProducts = skuInfoEntities.stream().map(sku -> {
            // ?????????????????????
            SkuEsModel skuEsModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, skuEsModel);
            // skuPrice,skuImg,brandName,brandImg,catalogName;
            skuEsModel.setSkuPrice(sku.getPrice());
            skuEsModel.setSkuImg(sku.getSkuDefaultImg());
            // hasStock,hotSorce,
            // ??????????????????
            if (!Optional.ofNullable(finalStockMap).isPresent()) {
                skuEsModel.setHasStock(true);
            } else {
                skuEsModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }

            //  ???????????? ??????0
            skuEsModel.setHotScore(0L);
            //  ?????????????????????????????????
            BrandEntity brand = brandService.getById(skuEsModel.getBrandId());
            skuEsModel.setBrandImg(brand.getLogo());
            skuEsModel.setBrandName(brand.getName());

            CategoryEntity categoryEntity = categoryService.getById(skuEsModel.getCatalogId());
            skuEsModel.setCatalogName(categoryEntity.getName());
            // ??????????????????
            skuEsModel.setAttrs(attrs);
            return skuEsModel;
        }).collect(Collectors.toList());
        //  ?????????es??????
        R r = searchOpenFeignService.productStatusUp(upProducts);
        if (r.getCode() == 0) {
            //  ??????????????????
            baseMapper.updateSpuStatus(spuId, StatusEnum.SPU_UP.getCode());
        } else {
            // ??????????????????
            //TODO ????????????? ???????????????;????????????
            // 1.Feign????????????
            /**
             * 1.??????????????????,???????????????json
             * RequestTemplate template = this.buildTemplateFromArgs.create(argv);
             * 2.???????????????????????? (?????????????????????????????????)
             * return this.executeAndDecode(template, options);
             * 3.??????????????????????????????
             *  while(true){
             *      try{
             *          return this.executeAndDecode(template, options);
             *      }catch{
             *              RetryableException e = var9;
             *              try {
             *                  retryer.continueOrPropagate(e);
             *              }catch (RetryableException var8) {
             *                     Throwable cause = var8.getCause();
             *                     if (this.propagationPolicy == ExceptionPropagationPolicy.UNWRAP && cause != null) {
             *                         throw cause;
             *                     }
             *                     throw var8;
             *      }
             *  }
             *
             */
        }

    }

    private void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

}
