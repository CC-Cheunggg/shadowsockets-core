package com.cheung.shadowsockets.config;

import lombok.Data;

import java.io.Serializable;

/**
 * 配置
 */
@Data
public class User implements Serializable {

    private int serverPort;
    private String method;
    private String password;
    private String name;

    public User() {

    }

    public User(String name, int serverPort, String method, String password) {
        this.name = name;
        this.serverPort = serverPort;
        this.method = method;
        this.password = password;
    }

}
