package com.cheung.shadowsocks.model;

import com.cheung.shadowsocks.common.SSModelValidator;
import com.cheung.shadowsocks.encryption.ICrypt;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socks.SocksAddressType;
import lombok.Data;

import java.util.List;

/**
 * Created by cheungrp on 18/7/3.
 */
@Data
@SSModelValidator(message = "host and port cannot be empty")
public class SSModel {

    private volatile ChannelHandlerContext channelHandlerContext;
    private volatile String tsn;
    private volatile ICrypt crypt;
    private volatile SocksAddressType hostType;
    private volatile String host;
    private volatile int port;
}
