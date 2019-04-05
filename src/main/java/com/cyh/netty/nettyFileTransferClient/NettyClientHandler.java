package com.cyh.netty.nettyFileTransferClient;

import com.cyh.netty.entity.fileTransfer.NettyFileProtocol;
import com.cyh.netty.util.CommonUtil;
import com.cyh.netty.util.SerializeUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 客户端业务逻辑实现
 */
@Service
@ChannelHandler.Sharable
public class NettyClientHandler extends ChannelInboundHandlerAdapter {

    @Autowired
    private NettyClient nettyClient;

    /**
     * 建立连接成功
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        CommonUtil.print("建立连接成功");
        ctx.fireChannelActive();
    }

    /**
     * 关闭连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        CommonUtil.print("关闭连接");
        final EventLoop eventLoop = ctx.channel().eventLoop();
        nettyClient.doConnect(new Bootstrap(), eventLoop);
        super.channelInactive(ctx);
    }

    /**
     * 心跳请求处理 每4秒发送一次心跳请求;
     *
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object obj) throws Exception {
        if (obj instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) obj;
            if (event.state().equals(IdleState.READER_IDLE)) {
                CommonUtil.print("READER_IDLE");
            } else if (event.state().equals(IdleState.WRITER_IDLE)) {
                /**发送心跳,保持长连接*/
                NettyFileProtocol nfp = new NettyFileProtocol(1,-1,1,1,1,null,false);
                ctx.channel().writeAndFlush(nfp);
                CommonUtil.print("心跳发送成功!");
            } else if (event.state().equals(IdleState.ALL_IDLE)) {
                CommonUtil.print("ALL_IDLE");
            }
        }
    }

    /**
     * 业务逻辑处理
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof NettyFileProtocol){
            CommonUtil.print("NettyFileProtocol类型的数据!" + msg.toString());
            NettyFileProtocol nfp = (NettyFileProtocol) msg;
            switch(nfp.getContentType()){
                case -1: //收到服务器的心跳回复
                    // nothing to do
                    break;
                case 2: // 图片发送了服务器，收到了服务器的反馈

                    break;

                case 3: // 文件发送到了服务器，收到了服务器的反馈

                    break;
            }
        }else{
            CommonUtil.print("垃圾数据!");
        }
    }
}
