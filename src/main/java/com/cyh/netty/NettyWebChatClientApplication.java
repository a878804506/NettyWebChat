package com.cyh.netty;

import com.cyh.netty.nettyFileTransferClient.NettyClient;
import com.cyh.netty.nettyWebChat.WebSocketServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.cyh.netty")
@SpringBootApplication
public class NettyWebChatClientApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(NettyWebChatClientApplication.class, args);

		// 启动netty文件传输客户端
		NettyClient nc = applicationContext.getBean(NettyClient.class);
		new Thread(nc).start();

		// 启动webChat服务
		WebSocketServer wss = applicationContext.getBean(WebSocketServer.class);
		new Thread(wss).start();
	}
}
