package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

   // private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Override
    public Result queryById(Long id) {
        // 解决缓存穿透
        // queryWithPassThrough(id);
        // cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // queryWithMutex(id);
        // cacheClient.queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);


        // 逻辑过期时间解决缓存击穿
        // queryWithLogicalExpire(id);
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }

        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        if (shop.getId() == null) {
            return Result.fail("店铺id不能为空");
        }

        // 1.更新数据库
        updateById(shop);

        // 2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());

        return Result.ok();
    }

    /**
     * 解决缓存穿透
     */
    /* public Shop queryWithPassThrough(Long id) {
        // 1.从redis查询商铺缓存
        String shopKey = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);

        // 2.判断缓存是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在 直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            // return Result.ok(shop);
            return shop;
        }

        // 判断命中是否为空值
        if (shopJson != null) {
            // 返回错误信息
            // return Result.fail("店铺信息不存在！");
            return null;
        }

        // 4.不存在 根据id查询数据库
        Shop shop = getById(id);

        // 5.再判断店铺在数据库是否存在
        if (shop == null) {
            // 将空对象写入redis[解决缓存穿透]
            stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);

            // 不存在 返回错误
            // return Result.fail("店铺不存在！");
            return null;
        }

        // 存在 写入redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 6.返回店铺信息
        // return Result.ok(shop);
        return shop;
    } */

    /**
     * 维护逻辑过期时间解决缓存击穿
     */
    /* public Shop queryWithLogicalExpire(Long id) {
        // 1.从redis查询商铺缓存
        String shopKey = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);

        // 2.判断缓存是否存在
        if (StrUtil.isBlank(shopJson)) {
            // 3.不存在 直接返回空
            return null;
        }

        // 4.命中，Json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        // 5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 未过期，返回店铺信息
            return shop;
        }
        // 6. 已过期，重建缓存[重查数据库，写入缓存]

        // 6.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);

        // 6.2 判断是否加锁成功
        if (isLock) {
            // 6.3 成功开启独立线程 实现缓存重建[获取锁成功应该再次检查redis缓存是否过期，做doubleCheck，如果存在则无需重建缓存]
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                // 重建缓存
                try {
                    saveShopToRedis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }
        // 6.4 返回过期的店铺信息
        return shop;
    } */

    /**
     * 互斥锁解决缓存击穿
     */
    /* public Shop queryWithMutex(Long id) {
        // 1.从redis查询商铺缓存
        String shopKey = CACHE_SHOP_KEY + id;
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);

        // 2.判断缓存是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在 直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 判断命中是否为空值
        if (shopJson != null) {
            // 返回错误信息
            // return Result.fail("店铺信息不存在！");
            return null;
        }

        // 实现缓存重建[重查数据库，写入缓存]
        // 4.1获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2判断是否获取成功【获取锁成功应该再次检查redis缓存是否过期，做doubleCheck，如果存在则无需重建缓存】
            if (!isLock) {
                // 4.3失败 失眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }


            // 4.4成功 根据id查询数据库
            shop = getById(id);
            // 模拟重建的延迟
            Thread.sleep(1000);

            // 5.再判断店铺在数据库是否存在
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

        // 8.返回店铺信息
        return shop;
    } */

    /**
     * 存入店铺信息至redis
     */
   /*  public void saveShopToRedis(Long id, Long expireSeconds) throws InterruptedException {
        // 1.查询店铺信息
        Shop shop = getById(id);
        Thread.sleep(200);

        // 2.封装逻辑过期时间
        RedisData data = new RedisData();
        data.setData(shop);
        data.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        // 3.写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(data));
    } */

    /**
     * 尝试获取锁
     */
   /*  private boolean tryLock(String key) {
        // setnx
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.MINUTES);
        // 防止自动拆箱出现空指针
        return BooleanUtil.isTrue(flag);
    } */

    /**
     * 删除锁
     */
   /*  private void unlock(String key) {
        stringRedisTemplate.delete(key);
    } */

}
