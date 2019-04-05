package com.cyh.netty.nettyFileTransferClient;

import com.cyh.netty.entity.fileTransfer.NettyFileProtocolDecoder;
import com.cyh.netty.entity.fileTransfer.NettyFileProtocolEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Netty客户端 过滤器
 */
@Component
public class NettyClientFilter extends ChannelInitializer<SocketChannel> {

    @Autowired
    private NettyClientHandler nettyClientHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline ph = ch.pipeline();
        /*
         * 解码和编码，应和服务端一致
         * */
        //入参说明: 读超时时间、写超时时间、所有类型的超时时间、时间格式
        ph.addLast(new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS));

        //添加自定义协议的编解码工具  数据接收最大2K
        ph.addLast(new ObjectDecoder(1024*2,ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
        ph.addLast(new ObjectEncoder());

        //业务逻辑实现类
        ph.addLast("nettyClientHandler",nettyClientHandler);
    }
}