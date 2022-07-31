package com.cheung.shadowsocks.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class Config implements Serializable {


    private volatile String ipAddr;

    private volatile String localIpAddr;

    private volatile boolean track;

    private volatile int serverPort;

    private volatile String method;

    private volatile String password;


}
