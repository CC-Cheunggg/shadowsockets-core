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

    private ChannelHandlerContext channelHandlerContext;
    private String tsn;
    private ICrypt crypt;
    private List<byte[]> data;
    private byte[] payload;
    private SocksAddressType hostType;
    private String host;
    private int port;
}
