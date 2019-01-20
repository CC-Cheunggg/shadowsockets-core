package com.cheung.shadowsockets.model;

import com.cheung.shadowsockets.encryption.ICrypt;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

/**
 * Created by cheungrp on 18/7/3.
 */
@Data
public class SSModel {

    private ChannelHandlerContext channelHandlerContext;
    private ICrypt crypt;
    private ByteBuf data;
}
