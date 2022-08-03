package com.cheung.shadowsocks.config;

import lombok.Data;

import java.io.Serializable;

@Data
public class Config implements Serializable {


    private String ipAddr;

    private String localIpAddr;

    private boolean track;

    private int serverPort;

    private String method;

    private String password;


}
