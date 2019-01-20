package com.cheung.shadowsockets;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationPid;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean;

import java.lang.reflect.InvocationTargetException;

/**
 * socksserver启动类
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class ServerStart implements CommandLineRunner {

    static {
        System.setProperty("java.net.preferIPv4Stack", "false");
        System.setProperty("java.net.preferIPv6Addresses", "true");
    }

    public static void main(String[] args) {
        SpringApplication.run(ServerStart.class, args);
    }

    private static String getCurrentPID() {
        return new ApplicationPid().toString();
    }

    @Override
    public void run(String... args) throws Exception {

        log.info("current server pid: " + getCurrentPID());
    }
}
