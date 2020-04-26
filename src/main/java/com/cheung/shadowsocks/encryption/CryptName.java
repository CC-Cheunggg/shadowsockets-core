package com.cheung.shadowsocks.encryption;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CryptName {
    // 值为客户端中加密方式的名称
    String[] value();
}
