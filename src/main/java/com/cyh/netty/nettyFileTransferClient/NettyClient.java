package com.cyh.netty.nettyFileTransferClient;

import com.cyh.netty.util.CommonUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 *  Netty客户端 心跳测试
 */
@Component
public class NettyClient implements Runnable{

    @Value("${netty.file.server.host}")
    private String host;
    @Value("${netty.file.server.port}")
    private int port ;

    // 通过nio方式来接收连接和处理连接
    private EventLoopGroup group = new NioEventLoopGroup();

    @Autowired
    private NettyClientFilter nettyClientFilter;

    /**唯一标记 */
    private boolean initFalg=true;

    /**
     * Netty创建全部都是实现自AbstractBootstrap。 客户端的是Bootstrap，服务端的则是 ServerBootstrap。
     **/
    @Override
    public void run() {
        doConnect(group);
    }

    /**
     * 重连
     */
    public void doConnect( EventLoopGroup eventLoopGroup) {
        ChannelFuture f = null;
        Bootstrap bootstrap = new Bootstrap();
        try {
            if (bootstrap != null) {
                bootstrap.group(eventLoopGroup);
                bootstrap.channel(NioSocketChannel.class);
                bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
                bootstrap.handler(nettyClientFilter);
                bootstrap.remoteAddress(host, port);
                f = bootstrap.connect().addListener((ChannelFuture futureListener) -> {
                    final EventLoop eventLoop = futureListener.channel().eventLoop();
                    if (!futureListener.isSuccess()) {
                        CommonUtil.print("与服务端断开连接!在10s之后准备尝试重连!");
                        eventLoop.schedule(() -> doConnect(eventLoop), 10, TimeUnit.SECONDS);
                    }
                });
                if(initFalg){
                    CommonUtil.print("Netty文件传输客户端启动成功!");
                    initFalg=false;
                }
                // 阻塞
                f.channel().closeFuture().sync();
            }
        } catch (Exception e) {
            CommonUtil.print("Netty文件传输客户端连接失败!!"+e.getMessage());
        }
    }
}
