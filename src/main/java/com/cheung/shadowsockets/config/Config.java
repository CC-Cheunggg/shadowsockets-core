package com.cheung.shadowsockets.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@ToString
public class Config implements Serializable {

    @Setter
    @Getter
    private String ipAddr;

    @Setter
    @Getter
    private String localIpAddr;

    @Setter
    @Getter
    private boolean track;

    @Getter
    private User[] users;


}
