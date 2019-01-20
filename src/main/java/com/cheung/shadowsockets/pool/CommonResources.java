package com.cheung.shadowsockets.pool;

import com.cheung.shadowsockets.model.SSModel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.util.AttributeKey;

/**
 * Created by cheungrp on 18/5/12.
 */
public interface CommonResources {
//    EpollEventLoopGroup eventExecutors = new EpollEventLoopGroup(256);
//    EpollEventLoopGroup bossGroup = new EpollEventLoopGroup(128);
//    EpollEventLoopGroup workerGroup = new EpollEventLoopGroup(256);

    EventLoopGroup eventExecutors = new EpollEventLoopGroup(32);
    EventLoopGroup bossGroup = new EpollEventLoopGroup(8);
    EventLoopGroup workerGroup = new EpollEventLoopGroup(16);

    AttributeKey<ChannelHandlerContext> SERVER_CHANNEL = AttributeKey.valueOf("server.channel");
    AttributeKey<SSModel> SS_MODEL = AttributeKey.valueOf("ss.model");

}
