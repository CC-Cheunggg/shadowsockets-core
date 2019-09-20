package com.cheung.shadowsocks.common;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "common-resources-config")
@Slf4j
@Getter
@Setter
public class CommonResourcesConfig {


    private int eventExecutorsSize;
    private int workerGroupSize;
    private int internetEventGroupSize;


    @Bean
    //@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public EventLoopGroup eventExecutors() {
        return new EpollEventLoopGroup(eventExecutorsSize);
    }

    @Bean
    public EventLoopGroup workerGroup() {
        return new EpollEventLoopGroup(workerGroupSize);
    }

    @Bean
    public EventLoopGroup bossGroup() {
        return new EpollEventLoopGroup(1);
    }

    @Bean
    public EventLoopGroup internetEventLoopGroup() {
        return new EpollEventLoopGroup(internetEventGroupSize);
    }

    @Bean
    public Bootstrap bootstrap() {
        return new Bootstrap();
    }


}
