package com.cyh.netty.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 *  该配置是为Netty WebChat 所服务的配置类
 */
@Configuration
public class NettyWebChatConfig {

    @Bean("nettyWebChatBoss")
    public NioEventLoopGroup createNettyWebChatBossGroup(){
        return new NioEventLoopGroup();
    }

    @Bean("nettyWebChatWork")
    public NioEventLoopGroup createNettyWebChatWorkerGroup(){
        return new NioEventLoopGroup();
    }

    @Scope("prototype")
    @Bean("nettyWebChatServerBootstrap")
    public ServerBootstrap createNettyWebChatServerBootstrap(){
        return new ServerBootstrap();
    }
}
