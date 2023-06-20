package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class HmDianPingApplicationTests {
    @Resource
    ShopServiceImpl shopService;

    @Test
    void testSaveShop() throws InterruptedException {
        shopService.saveShopToRedis(2L, 10L);
    }
}
