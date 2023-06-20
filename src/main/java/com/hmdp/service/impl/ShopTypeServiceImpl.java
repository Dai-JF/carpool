package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_TTL;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryList() {
        //1.从redis查询商铺类型缓存
        String shopTypeKey = CACHE_SHOP_TYPE_KEY;
        String shopTypeJson = stringRedisTemplate.opsForValue().get(shopTypeKey);

        //2.判断缓存是否存在
        if (StrUtil.isNotBlank(shopTypeJson)) {
            //3.存在 直接返回
            List<ShopType> list = JSONUtil.toList(shopTypeJson, ShopType.class);
            return Result.ok(list);
        }

        //4.不存在 查询数据库
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();

        // 存在 写入redis
        stringRedisTemplate.opsForValue().set(shopTypeKey, JSONUtil.toJsonStr(shopTypeList),CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        //6.返回店铺信息
        return Result.ok(shopTypeList);
    }
}
