package com.cheung.shadowsocks;

import com.cheung.shadowsocks.encryption.CryptFactory;
import com.cheung.shadowsocks.encryption.ICrypt;
import com.cheung.shadowsocks.utils.JarUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

/**
 * socksserver启动类
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
@SuppressWarnings("restriction")
public class ServerStart implements CommandLineRunner {

    static {
        System.setProperty("java.net.preferIPv4Stack", "false");
        System.setProperty("java.net.preferIPv6Addresses", "true");
        registerCryptClass();
    }

    public static void main(String[] args) {
        SpringApplication.run(ServerStart.class, args);
    }

    private void signalConfiguration() {
//        Signal.handle(new Signal("KILL"), signal -> log.info("shadowsocks server is killed."));
//        Signal.handle(new Signal("TERM"), signal -> log.info("shadowsocks server is terminated."));
    }

    private void initLog() {
        log.info("default Configuration : port: 8388 , method: chacha20-ietf-poly1305 , password: 000000");
    }

    private static void registerCryptClass() {
        String jarPath = ServerStart.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String[] classes = JarUtils.utils.jarScanner(new File(jarPath)
                , new String[]{"com/cheung/shadowsocks/encryption/impl/*.class"}
                , new String[]{"com/cheung/shadowsocks/encryption/impl/*$*.class"});

        for (String clz : classes) {
            log.info("已支持的加密=====>>>> " + clz);
            try {
                @SuppressWarnings("unchecked")
                Class<? extends ICrypt> c = (Class<? extends ICrypt>) Thread.currentThread().getContextClassLoader().loadClass(clz);
                CryptFactory.factory.addCryptClass(c);
            } catch (Exception e) {
                log.error("init error:", e);
            }

        }
        CryptFactory.factory.registerCrypt();
    }

    @Override
    public void run(String... args) {
        // 设置信号量 优雅关闭 netty
        signalConfiguration();
        initLog();
    }
}
