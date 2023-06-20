package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    /**
     * 逻辑过期时间
     */
    private LocalDateTime expireTime;
    /**
     * 封装其他信息【如：店铺，用户等】
     */
    private Object data;
}
