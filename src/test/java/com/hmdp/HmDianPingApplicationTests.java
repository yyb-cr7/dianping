package com.hmdp;


import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private CacheClient cacheClient;

    @Autowired
    private ShopServiceImpl shopService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("ttt");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };

        long begin = System.currentTimeMillis();
        for (int i = 0; i < 450; i++) {
            es.submit(task);
        }

        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }
    /*
    在这个执行流程中，首先创建一个固定大小为500的线程池用于并发任务处理。定义的任务是生成100个唯一ID并打印，每个任务完成后会通知一个计数器减少1。
    接着，初始化一个计数器，初始值为300，用于跟踪所有提交任务的完成情况。在任务开始执行前，记录开始时间，以便后续计算总执行时间。
    然后，将300个任务提交到线程池中，由线程池的线程并发执行。主线程在这期间进入等待状态，直到所有任务完成，即计数器减到0。
    所有任务完成后，主线程记录结束时间，并计算从开始到结束的总执行时间，最终打印出结果。
    这个过程旨在验证高并发环境下ID生成器的性能和正确性，通过计时来评估并发任务的总执行时间和系统效率
    * */


    @Test
    void testSaveShop() throws InterruptedException {
        Shop shop = shopService.getById(1L);

        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
    }


}
