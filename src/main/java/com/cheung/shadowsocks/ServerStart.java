package com.cheung.shadowsocks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationPid;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import sun.misc.Signal;

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

    private String getCurrentPID() {
        return new ApplicationPid().toString();
    }

    private void signalConfiguration() {
//        Signal.handle(new Signal("KILL"), signal -> log.info("shadowsocks server is killed."));
//        Signal.handle(new Signal("TERM"), signal -> log.info("shadowsocks server is terminated."));
    }

    private void initLog() {
        log.info("current server pid: " + getCurrentPID());
        log.info("default Configuration : port: 8388 , method: aes-256-cfb , password: 000000");
    }

    @Override
    public void run(String... args) {
        // 设置信号量 优雅关闭 netty
        signalConfiguration();
        initLog();
    }
}
