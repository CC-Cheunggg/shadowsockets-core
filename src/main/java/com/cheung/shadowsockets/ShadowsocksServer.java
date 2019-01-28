package com.cheung.shadowsockets;

import com.cheung.shadowsockets.config.Config;
import com.cheung.shadowsockets.config.ConfigXmlLoader;
import com.cheung.shadowsockets.config.User;
import com.cheung.shadowsockets.proxy.HostHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
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

    @Autowired
    @Qualifier("bossGroup")
    private EventLoopGroup bossGroup;

    @Autowired
    @Qualifier("workerGroup")
    private EventLoopGroup workerGroup;

    @Autowired
    @Qualifier("eventExecutors")
    private EventLoopGroup eventExecutors;


    public void start(Config config, User user) {
        try {
            bootstrap.group(bossGroup, workerGroup)
                    .channel(EpollServerSocketChannel.class)
                    .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                    .childOption(ChannelOption.AUTO_READ, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ConfigXmlLoader.loader.addUser(user);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast("hostHandler", new HostHandler(user, config.isTrack()));
                }
            });

            InetAddress inetAddress = InetAddress.getByName(config.getIpAddr());
            log.info("Started !  Port :" + user.getServerPort() + " Ip :" + inetAddress.getHostAddress());

            ChannelFuture channelFuture = bootstrap.bind(inetAddress, user.getServerPort());
            // 阻塞直到 channel 关闭
            channelFuture.sync().channel().closeFuture().sync();

        } catch (Exception e) {
            log.error("start error", e);
            ConfigXmlLoader.loader.clear();
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

            Config config = ConfigXmlLoader.loader.load();
            User[] users = config.getUsers();

            start(config, users[0]);
        }
        if (event instanceof ContextClosedEvent) {
            stop();
        }

    }
}
