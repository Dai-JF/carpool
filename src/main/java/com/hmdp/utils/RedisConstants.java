package com.hmdp.utils;

public class RedisConstants {
    /**
     * 验证码相关
     */
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    /**
     * token相关
     */
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 36000L;

    /**
     * 缓存空对象的过期时间
     */
    public static final Long CACHE_NULL_TTL = 2L;

    /**
     * 店铺相关
     */
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final Long CACHE_SHOP_TTL = 30L;

    /**
     * 店铺类型相关
     */
    public static final String CACHE_SHOP_TYPE_KEY = "cache:shopType:";
    public static final Long CACHE_SHOP_TYPE_TTL = 30L;

    /**
     * 店铺互斥锁相关
     */
    public static final String LOCK_SHOP_KEY = "lock:shop:";

    public static final Long LOCK_SHOP_TTL = 10L;

    /**
     * 优惠券库存相关
     */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    /**
     * 点赞相关
     */
    public static final String BOLG_LIKED_KEY = "blog:liked:";
}
