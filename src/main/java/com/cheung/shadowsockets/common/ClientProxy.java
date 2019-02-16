package com.cheung.shadowsockets.common;

import com.cheung.shadowsockets.proxy.InternetDataHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ClientProxy {

    @Autowired
    @Qualifier("bootstrap")
    private Bootstrap bootstrap;

    @Autowired
    @Qualifier("internetEventLoopGroup")
    private EventLoopGroup internetEventLoopGroup;

    private final InternetDataHandler internetDataHandler = new InternetDataHandler();

    @PostConstruct
    public void init() {
        bootstrap.group(internetEventLoopGroup)
                .channel(EpollSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .option(ChannelOption.AUTO_READ, true);
    }

    public ChannelFuture connect(String host, int port) {
        return bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(internetDataHandler);
            }
        }).connect(host, port);
    }

}
