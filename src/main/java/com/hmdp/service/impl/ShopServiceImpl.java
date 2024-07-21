package com.hmdp.service.impl;


import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

//    @Override
    // 最早的解决缓存与数据库不一致的解决方案
//    public Result queryById(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//
//        // 1. 从Redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//
//        // 2. 判断是否存在
//        if(StrUtil.isNotBlank(shopJson)) {
//            // 3. 存在，直接返回
//            System.out.println("redis数据已经存在，直接返回");
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return Result.ok(shop);
//        }
//
//        if(shopJson != null) {
//            return Result.fail("店铺信息不存在");
//        }
//
//        // 4. 不存在，根据id查询数据库
//        Shop shop = getById(id);
//
//        // 5. 不存在，返回错误
//        if(shop == null) {
//            // 没有缓存 存入空对象
//            stringRedisTemplate.opsForValue().set(key, "", CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//            return Result.fail("店铺不存在！");
//        }
//
//        // 6. 存在，写入redis
//        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//        // 7. 返回
//        return Result.ok(shop);
//    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    // 互斥锁部分代码
    @Override
    public Result queryById(Long id) {
        // 解决缓存穿透
        // Shop shop = cacheClient
//                .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = cacheClient
        //         .queryWithMutex(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
         Shop shop = cacheClient
                 .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);

        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        // 7.返回
        return Result.ok(shop);
    }


//
//    public Shop queryWithMutex(Long id)  {
//        String key = CACHE_SHOP_KEY + id;
//        // 1、从redis中查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        // 2、判断是否存在 判断 shopJson 是否不为空且不为空字符串。
//        if (StrUtil.isNotBlank(shopJson)) {
//            // 存在,直接返回
//            // System.out.println("存在 shopJson 是否不为空且不为空字符串,直接返回");
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
//        //判断命中的值是否是空值
//        if (shopJson != null) {
//            // shopJson 不为空，但也不包含有效的商铺数据（即为空值），说明之前缓存重建时数据库查询结果为空（例如，数据库中没有该商铺）
//            // 这种情况下，返回一个错误信息null 以避免重复的数据库查询。
//            return null;
//        }
//        // 4.实现缓存重构
//
//        //4.1 获取互斥锁
//        String lockKey = "lock:shop:" + id;
//        Shop shop = null;
//        try {
//            // System.out.println("进入互斥锁模块");
//            boolean isLock = tryLock(lockKey);
//            // 4.2 判断否获取成功
//            if(!isLock){
//                //4.3 失败，则休眠重试
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            //4.4 成功，根据id查询数据库
//            shop = getById(id);
//            // 模拟重建延时
//            Thread.sleep(200);
//            // 5.不存在，返回错误
//            if(shop == null){
//                //将空值写入redis
//                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
//                //返回错误信息
//                return null;
//            }
//            //6.写入redis
//            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL,TimeUnit.MINUTES);
//        }catch (Exception e){
//            throw new RuntimeException(e);
//        }
//        finally {
//            //7.释放互斥锁
//            unlock(lockKey);
//        }
//        return shop;
//    }
//

//    // 逻辑过期
//    // 线程池
//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
//
//    public Shop queryWithLogicalExpire( Long id ) {
//        String key = CACHE_SHOP_KEY + id;
//        // 1.从redis查询商铺缓存
//        String json = stringRedisTemplate.opsForValue().get(key);
//        // 2.判断是否存在
//        // 缓存未命中：当 Redis 中根本没有对应 key 的数据时，stringRedisTemplate.opsForValue().get(key) 会返回 null 或空字符串。
//        // 这个判断确保方法在这种情况下不会尝试反序列化空数据，而是直接返回 null。
//        //缓存初始化：在某些场景下，缓存可能尚未被初始化或者尚未构建。在这种情况下，这个判断可以有效地避免进一步的错误操作。
//
//        if (StrUtil.isBlank(json)) {
//            // 3.存在，直接返回
//            return null;
//        }
//        // 4.命中，需要先把json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//
//        // 5.判断是否过期
//        if(expireTime.isAfter(LocalDateTime.now())) {
//            // 5.1.未过期，直接返回店铺信息
//            return shop;
//        }
//        // 5.2.已过期，需要缓存重建
//
//        // 6.缓存重建
//        // 6.1.获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        // 6.2.判断是否获取锁成功
//        if (isLock){
//            CACHE_REBUILD_EXECUTOR.submit( ()->{
//
//                try{
//                    //重建缓存
//                    this.saveShop2Redis(id,20L);
//                }catch (Exception e){
//                    throw new RuntimeException(e);
//                }finally {
//                    unlock(lockKey);
//                }
//            });
//        }
//        // 6.4.返回过期的商铺信息
//        return shop;
//    }
}
