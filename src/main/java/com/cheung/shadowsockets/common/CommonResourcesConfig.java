package com.cheung.shadowsockets.common;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonResourcesConfig {

    @Bean
    //@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public EventLoopGroup eventExecutors() {
        return new EpollEventLoopGroup(16);
    }

    @Bean
    public EventLoopGroup workerGroup() {
        return new EpollEventLoopGroup(8);
    }

    @Bean
    public EventLoopGroup bossGroup() {
        return new EpollEventLoopGroup(2);
    }

    @Bean
    public EventLoopGroup internetEventLoopGroup() {
        return new EpollEventLoopGroup(32);
    }

    @Bean
    public Bootstrap bootstrap() {
        return new Bootstrap();
    }




}
