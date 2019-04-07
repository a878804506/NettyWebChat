package com.cyh.netty.nettyWebChat;

import com.cyh.netty.util.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import javax.annotation.PreDestroy;
import java.util.Set;

/**
 * 启动服务
 * */
@Component
public class WebSocketServer implements Runnable{

	//用于客户端连接请求
	@Qualifier("nettyWebChatBoss")
	@Autowired
	private EventLoopGroup bossGroup;
	
	//用于处理客户端I/O操作
	@Qualifier("nettyWebChatWork")
	@Autowired
	private EventLoopGroup workerGroup;
	
	//服务器的辅助启动类
	@Qualifier("nettyWebChatServerBootstrap")
	@Autowired
	private ServerBootstrap serverBootstrap;
	
	//BS的I/O处理类
	@Qualifier("webSocketChildChannelHandler")
	@Autowired
	private ChannelHandler childChannelHandler;
	
	private ChannelFuture channelFuture;
	
	//服务端口
	@Value("${netty.websocket.server.port}")
	private int port;

	@Autowired
	private RedisDB redisDB;

	@Autowired
	private ZookeeperUtil zookeezperUtil;

	@Autowired
	private UpdateHistoryMsgToRedis updateHistoryMsgToRedis;

	public WebSocketServer(){
//		System.out.println("NettyWebChat服务正在初始化。。。");
	}

	@Override
	public void run() {
		Jedis jedis = null;
		try {
			jedis = redisDB.getJedis();
			//从redis中获取联系人列表
			loadAllContactsListFromRedis(jedis);
			//加载所有redis 中的聊天记录
			loadAllHistoryMessageFromRedis(jedis);
			//加载所有redis中的未读聊天记录数
			loadAllHistoryMessageCountFromRedis(jedis);
			jedis.close();
			//连接到zookeeper服务，用于监控用户上下线的状态变化
			startZookeeper();
			//启动线程将聊天信息存入redis
			updateHistoryMsgToRedis.start();
			//各种信息加载完成之后，开始初始化NettyWebChat服务 并且启动
			bulid(port);
			
		} catch (Exception e) {
			e.printStackTrace();
            RedisDB.returnBrokenResource(jedis);
		}finally {
			RedisDB.returnResource(jedis);
		}
	}
	
	public void bulid(int port) throws Exception{
		try {
			//（1）boss辅助客户端的tcp连接请求  worker负责与客户端之前的读写操作
			//（2）配置客户端的channel类型
			//(3)配置TCP参数，握手字符串长度设置
			//(4)TCP_NODELAY是一种算法，为了充分利用带宽，尽可能发送大块数据，减少充斥的小块数据，true是关闭，可以保持高实时性,若开启，减少交互次数，但是时效性相对无法保证
			//(5)开启心跳包活机制，就是客户端、服务端建立连接处于ESTABLISHED状态，超过2小时没有交流，机制会被启动
			//(6)netty提供了2种接受缓存区分配器，FixedRecvByteBufAllocator是固定长度，但是拓展，AdaptiveRecvByteBufAllocator动态长度
			//(7)绑定I/O事件的处理类,WebSocketChildChannelHandler中定义
			serverBootstrap.group(bossGroup,workerGroup)
						   .channel(NioServerSocketChannel.class)
						   .option(ChannelOption.SO_BACKLOG, 1024)
						   .option(ChannelOption.TCP_NODELAY, true)
						   .childOption(ChannelOption.SO_KEEPALIVE, true)
						   .childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
						   .childHandler(childChannelHandler);
			
			CommonUtil.print("NettyWebChat服务启动成功！");
			channelFuture = serverBootstrap.bind(port).sync();
			channelFuture.channel().closeFuture().sync();
		} catch (Exception e) {
			bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
		}
	}
	
	//加载所有redis 中的聊天记录
	public void loadAllHistoryMessageFromRedis(Jedis jedis) {
		jedis.select(redisDB.dbSelectedForHistoryMessage);
		//获得所有的key
		Set<String> keys = jedis.keys("history_*");
		for(String key : keys) {
			byte[] msg = jedis.get(key.getBytes());
			CommonUtil.allHistoryMessage.put(key, SerializeUtil.unserializeForList(msg));
		}
	}
	
	//加载所有redis中的未读聊天记录数
	public void loadAllHistoryMessageCountFromRedis(Jedis jedis) {
		jedis.select(redisDB.dbSelectedForHistoryMessage);
		//获得所有的key
		Set<String> keys = jedis.keys("unread_*");
		for(String key : keys) {
			String msg = jedis.get(key);
			CommonUtil.unreadHistoryMessage.put(key, Integer.valueOf(msg));
		}
	}
	
	//从redis中获取联系人列表
	public void loadAllContactsListFromRedis(Jedis jedis) {
		jedis.select(redisDB.dbSelectedForSystem);
		CommonUtil.contactsList = SerializeUtil.unserializeForList(jedis.get(redisDB.systemUsers.getBytes()));
	}

	//连接到zookeeper服务，用于监控用户上下线的状态变化
	public void startZookeeper() {
		try {
			zookeezperUtil.zkConnect();
			zookeezperUtil.zkNodeCache();
			zookeezperUtil.zkPathChildrenCache();
		}catch (Exception e){
			CommonUtil.print("webSocket程序连接Zookeeper服务失败！");
		}

	}
	
	//执行之后关闭
	@PreDestroy
	public void close(){
		bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
	}
}
