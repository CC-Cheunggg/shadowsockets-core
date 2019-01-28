package com.cheung.shadowsockets.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
@Deprecated
//@Component
public class ChannelPool {


    private GenericObjectPool<Bootstrap> pool;

    @Autowired
    @Qualifier("config")
    private GenericObjectPoolConfig config;


    @Autowired
    private ClientProxyChannelFactory factory;

    @Autowired
    @Qualifier("abandonedConfig")
    private AbandonedConfig abandonedConfig;

    @PostConstruct
    public void initConfig() {
        abandonedConfig.setRemoveAbandonedOnBorrow(true);
        config.setBlockWhenExhausted(true);
        config.setMaxTotal(2);
        config.setMaxIdle(2);
        config.setMaxWaitMillis(-1);
        config.setLifo(true);
        config.setMinIdle(0);
        config.setMinEvictableIdleTimeMillis(30 * 60 * 1000);// 默认三十分钟
        pool = new GenericObjectPool<>(factory, config, abandonedConfig);
    }


    public ChannelFuture connect(final String host, final int port, final ChannelInboundHandler inboundHandler) throws Exception {
        Bootstrap bootstrap = pool.borrowObject();
        ChannelFuture channelFuture = bootstrap
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(inboundHandler);
                    }
                }).connect(host, port);
        pool.returnObject(bootstrap);
        return channelFuture;
    }

}
