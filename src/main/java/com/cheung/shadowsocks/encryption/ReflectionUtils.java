package com.cheung.shadowsocks.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by cheungrp on 18/9/18.
 */
public enum  ReflectionUtils  {

    utils;
    private static Logger logger = LoggerFactory.getLogger(ReflectionUtils.class);

    public ICrypt initCryptObject(Class<? extends ICrypt> clazz,String name, String password){
        try {
           Constructor<? extends ICrypt> constructor = clazz.getConstructor(name.getClass(),password.getClass());
           return constructor.newInstance(name,password);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            logger.error("get crypt error", e);
            return null;
        }
    }
}
