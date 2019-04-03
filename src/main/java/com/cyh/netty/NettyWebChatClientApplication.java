package com.cyh.netty;

import com.cyh.netty.nettyWebChat.WebSocketServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@ComponentScan("com.cyh.netty")
@SpringBootApplication
public class NettyWebChatClientApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(NettyWebChatClientApplication.class, args);

//		String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
//		for (String name : beanDefinitionNames)
//			System.out.println(name);

		WebSocketServer nettyWebChatIOC = applicationContext.getBean(WebSocketServer.class);
		nettyWebChatIOC.run();
	}
}
