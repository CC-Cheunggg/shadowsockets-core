package com.cheung.shadowsocks.encryption;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

@Slf4j
public enum CryptFactory {

    factory;

    private final Map<String, Class<? extends ICrypt>> crypts = Maps.newConcurrentMap();
    private final Set<Class<? extends ICrypt>> cryptClasses = Sets.newConcurrentHashSet();

    public void registerCrypt() {
        for (Class<? extends ICrypt> clz : cryptClasses) {
            if (clz.isAnnotationPresent(CryptName.class)) {
                CryptName cryptPlugin = clz.getAnnotation(CryptName.class);
                for (String val : cryptPlugin.value()) {
                    log.info("已开启的算法=========>>>> " + val);
                    crypts.put(val.trim(), clz);
                }
            }
        }
    }

    public void addCryptClass(Class<? extends ICrypt> crypt) {
        cryptClasses.add(crypt);
    }

    public ICrypt get(String name, String password) {
        Class<? extends ICrypt> clazz = crypts.get(name);
        if (clazz == null) {
            throw new IllegalArgumentException("找不到此算法 :" + name);
        }

        return ReflectionUtils.utils.initCryptObject(clazz, name, password);
    }
}
