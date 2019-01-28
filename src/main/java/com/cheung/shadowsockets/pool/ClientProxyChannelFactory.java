package com.cheung.shadowsockets.pool;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
@Deprecated
//@Component
public class ClientProxyChannelFactory extends BasePooledObjectFactory<Bootstrap> {

    @Autowired
    @Qualifier("internetEventLoopGroup")
    private EventLoopGroup internetEventLoopGroup;

    @Override
    public Bootstrap create() throws Exception {
        return new Bootstrap().group(internetEventLoopGroup)
                .channel(EpollSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .option(ChannelOption.AUTO_READ, true);
    }

    @Override
    public PooledObject<Bootstrap> wrap(Bootstrap obj) {
        return new DefaultPooledObject<>(obj);
    }


}
