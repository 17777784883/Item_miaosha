package com.ren.service.model;

/**
 * @author Ren
 */

public class RedisLock {

    /**
     * 锁的key
     */
    private String key;
    /**
     * 锁的值
     */
    private String value;

    public RedisLock(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
