package com.cheung.shadowsockets.pool;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class ClientProxyChannelFactory extends BasePooledObjectFactory<Bootstrap> {

    private final static EventLoopGroup group = new EpollEventLoopGroup(32);

    @Override
    public Bootstrap create() throws Exception {
        return new Bootstrap().group(group).channel(EpollSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .option(ChannelOption.AUTO_READ, true);
    }

    @Override
    public PooledObject<Bootstrap> wrap(Bootstrap obj) {
        return new DefaultPooledObject<>(obj);
    }


}
