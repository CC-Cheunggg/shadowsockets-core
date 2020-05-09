package com.cheung.shadowsocks.server;

import com.cheung.shadowsocks.codec.ShadowsocksDecoder;
import com.cheung.shadowsocks.config.Config;
import com.cheung.shadowsocks.config.ConfigXmlLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Slf4j
@Component
public class ShadowsocksServer implements ApplicationListener {

    private final ServerBootstrap bootstrap = new ServerBootstrap();
    private final Config config = ConfigXmlLoader.loader.load();


    @Autowired
    @Qualifier("bossGroup")
    private EventLoopGroup bossGroup;

    @Autowired
    @Qualifier("workerGroup")
    private EventLoopGroup workerGroup;

    @Autowired
    @Qualifier("eventExecutors")
    private EventLoopGroup eventExecutors;

    public void start() {
        try {
            bootstrap.group(bossGroup, workerGroup)
                    .channel(EpollServerSocketChannel.class)
                    .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                    .childOption(ChannelOption.AUTO_READ, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // [bandwidth] * [delay]
                    .childOption(ChannelOption.SO_SNDBUF, 128 * 5 * 120 * 1024)
                    .childOption(ChannelOption.SO_RCVBUF, 128 * 5 * 120 * 1024)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    socketChannel.pipeline().addLast("shadowsocksDecoder", new ShadowsocksDecoder());
                }
            });

            InetAddress inetAddress = InetAddress.getByName(config.getIpAddr());
            log.info("Started !  Port :" + config.getServerPort() + " Ip :" + inetAddress.getHostAddress());

            ChannelFuture channelFuture = bootstrap.bind(inetAddress, config.getServerPort());
            // 阻塞直到 channel 关闭
            channelFuture.sync().channel().closeFuture().sync();

        } catch (Exception e) {
            log.error("start error", e);
        } finally {
            stop();
        }
    }

    private void stop() {
        try {
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
            eventExecutors.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            log.error("Stop Server!", e);
        }
        log.info("Stop Server!");
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            start();
        }
        if (event instanceof ContextClosedEvent) {
            stop();
        }
    }
}
