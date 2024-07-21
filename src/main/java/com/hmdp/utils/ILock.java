package com.hmdp.utils;

public interface ILock {
    /**
     * 尝试获取锁
     * @param timeSec 锁的持有时间 过期后自动释放
     * @return
     */
    boolean tryLock(long timeSec);

    /**
     * 释放锁
     */
    void unlock();
}
