package com.cheung.shadowsocks.encryption;

import com.cheung.shadowsocks.encryption.impl.*;
import com.google.common.collect.Maps;
import com.zaxxer.hikari.HikariConfig;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.util.Map;


public enum CryptFactory {

    factory;

    private final Map<String, Class<? extends ICrypt>> crypts = Maps.newConcurrentMap();

    CryptFactory(){


        crypts.putAll(AesStreamCrypt.getCiphers());
        crypts.putAll(CamelliaStreamCrypt.getCiphers());
        crypts.putAll(BlowFishStreamCrypt.getCiphers());
        crypts.putAll(SeedStreamCrypt.getCiphers());
        crypts.putAll(Chacha20StreamCrypt.getCiphers());
        crypts.putAll(AesGcmCrypt.getCiphers());
        crypts.putAll(ChaCha20Poly1305Crypt.getCiphers());
    }

    public ICrypt get(String name, String password) {
        Class<? extends ICrypt> clazz = crypts.get(name);
        if (clazz == null) {
            throw new IllegalArgumentException("找不到此算法 :" + name);
        }

        return ReflectionUtils.utils.initCryptObject(clazz, name, password);
    }
}
