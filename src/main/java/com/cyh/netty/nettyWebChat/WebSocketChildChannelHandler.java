package com.cyh.netty.nettyWebChat;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class WebSocketChildChannelHandler extends ChannelInitializer<SocketChannel> {

	@Resource(name = "webSocketServerHandler")
	private ChannelHandler webSocketServerHandler;

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast("http-codec", new HttpServerCodec());
		ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
		ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
		ch.pipeline().addLast("handler",webSocketServerHandler);
	}

}
