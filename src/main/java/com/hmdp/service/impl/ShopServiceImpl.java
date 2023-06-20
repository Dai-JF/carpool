package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        // 解决缓存穿透
        // queryWithPassThrough(id);

        //互斥锁解决缓存击穿
        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }

        return Result.ok(shop);
    }

    /**
     * 解决缓存穿透
     */
    public Shop queryWithPassThrough(Long id) {
        //1.从redis查询商铺缓存
        String shopKey = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);

        //2.判断缓存是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //3.存在 直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            // return Result.ok(shop);
            return shop;
        }

        // 判断命中是否为空值
        if (shopJson != null) {
            //返回错误信息
            //return Result.fail("店铺信息不存在！");
            return null;
        }

        //4.不存在 根据id查询数据库
        Shop shop = getById(id);

        //5.再判断店铺在数据库是否存在
        if (shop == null) {
            // 将空对象写入redis[解决缓存穿透]
            stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);

            // 不存在 返回错误
            //return Result.fail("店铺不存在！");
            return null;
        }

        // 存在 写入redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //6.返回店铺信息
        //return Result.ok(shop);
        return shop;
    }

    /**
     * 互斥锁解决缓存击穿
     */
    public Shop queryWithMutex(Long id) {
        //1.从redis查询商铺缓存
        String shopKey = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);

        //2.判断缓存是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //3.存在 直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 判断命中是否为空值
        if (shopJson != null) {
            //返回错误信息
            //return Result.fail("店铺信息不存在！");
            return null;
        }

        //实现缓存重建
        //4.1获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //4.2判断是否获取成功
            if (!isLock) {
                //4.3失败 失眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }


            //4.4成功 根据id查询数据库
            shop = getById(id);
            //模拟重建的延迟
            Thread.sleep(1000);

            //5.再判断店铺在数据库是否存在
            if (shop == null) {
                // 将空对象写入redis
                stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);

                // 不存在 返回错误
                return null;
            }

            // 6.存在 写入redis
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 7.释放互斥锁
            unlock(lockKey);
        }

        //8.返回店铺信息
        return shop;
    }


    /**
     * 尝试获取锁
     */
    private boolean tryLock(String key) {
        //setnx
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
        //防止自动拆箱出现空指针
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 删除锁
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("店铺id不能为空");
        }

        //1.更新数据库
        updateById(shop);

        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());

        return Result.ok();
    }

}
