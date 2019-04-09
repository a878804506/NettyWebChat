package com.cyh.netty.nettyWebChat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cyh.netty.constant.ConstantValue;
import com.cyh.netty.entity.fileTransfer.NettyFileProtocol;
import com.cyh.netty.entity.webChat.OneToOneMessage;
import com.cyh.netty.nettyFileTransferClient.NettyClient;
import com.cyh.netty.util.CommonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * websocket 具体业务处理方法
 * 
 */

@Component
@Sharable
public class WebSocketServerHandler extends BaseWebSocketServerHandler {

	private WebSocketServerHandshaker handshaker;

	// byte[] 数据集合
	private List<byte[]> tempByteBuf = new ArrayList<>();

	@Autowired
	private NettyClient nettyClient;

	@Value("${nginx.staticMessageFilePath}")
	public String staticMessageFilePath ;

	/**
	 * 当客户端连接成功，返回个成功信息
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
//		ctx.channel().write(new TextWebSocketFrame("server:主动给客户端发消息"));
		ctx.flush();
	}

	/**
	 * 当客户端断开连接
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		//剔除ChannelHandlerContext
		for (String key : CommonUtil.pushCtxMap.keySet()) {
			if (ctx.equals(CommonUtil.pushCtxMap.get(key))) {
				// 从连接池内剔除
				CommonUtil.pushCtxMap.remove(key);
				CommonUtil.print("下线用户id:" + key + ",在线用户数:"+CommonUtil.pushCtxMap.size());
			}
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		// http：//xxxx
		if (msg instanceof FullHttpRequest) {
			handleHttpRequest(ctx, (FullHttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			// ws://xxxx
			handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}

	public void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
		// 关闭请求
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return;
		}
		// ping请求
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}
		String request = null;
		JSONObject jsonObject = null;
        if (frame instanceof BinaryWebSocketFrame) {
			// 如果是二进制的文件消息，就先把文件流保存起来，在告诉客户端保存好了，进入了文件发送状态
			ByteBuf content = frame.content();
			byte[] bytes = new byte[content.readableBytes()];
			content.readBytes(bytes);
			CommonUtil.print("BinaryWebSocketFrame接收到的数据为"+bytes.length);
			if (!frame.isFinalFragment()) {
				// 临时接收
				tempByteBuf.add(bytes);
				return;
			}
			tempByteBuf.clear();
			// 只需要一次发送二进制文件就可以发送完成 ，说明此次数据包比较小
			CommonUtil.print("BinaryWebSocketFrame接收到的最终数据为+++++++++"+bytes.length);
			String [] head = null;
			byte [] sendPicData = null;
			Map<String, Object> sendPicStruts = new HashMap<>();
			try{
				byte headLength = bytes[0]; //报文第一位是报文头大小
				String res = new String(Arrays.copyOfRange(bytes, 1, headLength+1),"UTF-8");
				head = res.split(",");
				if(head.length != 6){
					System.out.println("报错");
					sendPicStruts.put("sendPicStruts", false);
					sendPicStruts.put("sendPicAddress", "报文有误！请重试！");
					ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(sendPicStruts))); //告知发送者 发送状态
					return;
				}
				sendPicData = Arrays.copyOfRange(bytes, headLength+1, bytes.length);
				CommonUtil.print("BinaryWebSocketFrame接收到的最终数据为+++++++++"+sendPicData.length);
			}catch (Exception e){
				e.printStackTrace();
				sendPicStruts.put("sendPicStruts", false);
				sendPicStruts.put("sendPicAddress", "报文有误！请重试！");
				ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(sendPicStruts))); //告知发送者 发送状态
				return;
			}

			try {
				String fileName = head[0]+"_"+head[1]+"_"+head[3]+"_"+head[4]+"."+head[5];
				NettyFileProtocol nfp = new NettyFileProtocol(sendPicData.length, 2,
						Integer.valueOf(head[0]), Integer.valueOf(head[1]), Long.valueOf(head[3]),
						sendPicData, Integer.valueOf(head[4]), head[5], false);

				Channel channel = nettyClient.channelFuture.channel();

				//方式二：采用监听channel机制
				ChannelFuture await = channel.writeAndFlush(nfp);
				String[] finalHead = head;
				await.addListener((ChannelFutureListener) future -> {
					if(future.isSuccess()){ // 发送成功
						sendPicStruts.put("id", finalHead[4]);
						sendPicStruts.put("type", 7);
						sendPicStruts.put("sendPicStruts", true);
						sendPicStruts.put("sendPicAddress", staticMessageFilePath+fileName);
						// 发给目标用户
						OneToOneMessage oneToOneMessage = new OneToOneMessage();
						oneToOneMessage.setId(fileName);
						oneToOneMessage.setMsgType("2");
						oneToOneMessage.setFrom(Integer.valueOf(finalHead[0]));
						oneToOneMessage.setTo(Integer.valueOf(finalHead[1]));
						oneToOneMessage.setData(staticMessageFilePath+fileName); // 发送给目标用户的是ngixn静态文件服务器上的图片地址
						oneToOneMessage.setDate(CommonUtil.DateToString(new Date(), ConstantValue.DATE_FORMAT));
						if (CommonUtil.pushCtxMap.containsKey(oneToOneMessage.getTo().toString())) {//找到目标用户
							push(CommonUtil.pushCtxMap.get(oneToOneMessage.getTo().toString()), JSON.toJSONString(oneToOneMessage));//给目标用户发送消息
						} else {//不在线
							CommonUtil.print("消息发送的目标用户不在线！");
						}
						//加入未读集合
						CommonUtil.addunreadHistoryMessage(oneToOneMessage);
						//加入聊天历史集合
						CommonUtil.addAllHistoryMessage(oneToOneMessage);
						ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(sendPicStruts))); //告知发送者 发送状态
					}else{
						future.cause().printStackTrace(); //向文件服务器发送文件 发送失败了
					}
				});

			} catch (Exception e) {
				e.printStackTrace();
				sendPicStruts.put("sendPicStruts", false);
				sendPicStruts.put("sendPicAddress", "发送失败！");
				ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(sendPicStruts))); //告知发送者 发送状态
			}finally {

			}
			return;
        }else if(frame instanceof ContinuationWebSocketFrame){
			ByteBuf content = frame.content();
			byte[] bytes = new byte[content.readableBytes()];
			content.readBytes(bytes);
			CommonUtil.print("ContinuationWebSocketFrame接收到的数据为"+bytes.length);
			// 临时接收
			tempByteBuf.add(bytes);
			if (!frame.isFinalFragment()) {
				return;
			}
			String [] head = null;
			byte [] sendPicData = null;
			Map<String, Object> sendPicStruts = new HashMap<>();
			try{
				//合并为一个byte[]
				byte[] allBytes = new byte[0];
				for (byte[] _bytes:tempByteBuf) {
					allBytes = ArrayUtils.addAll(allBytes,_bytes);
				}
				byte headLength = allBytes[0]; //报文第一位是报文头大小
				String res = new String(Arrays.copyOfRange(allBytes, 1, headLength+1),"UTF-8");
				head = res.split(",");
				if(head.length != 6){
					System.out.println("报错");
					sendPicStruts.put("sendPicStruts", false);
					sendPicStruts.put("sendPicAddress", "报文有误！请重试！");
					ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(sendPicStruts))); //告知发送者 发送状态
					return;
				}
				sendPicData = Arrays.copyOfRange(allBytes, headLength+1, allBytes.length);
				CommonUtil.print("清理前，byte集合为："+tempByteBuf.size()+"个");
				CommonUtil.print("ContinuationWebSocketFrame接收到的最终数据为+++++++++"+sendPicData.length);
			}catch (Exception e){
				e.printStackTrace();
				sendPicStruts.put("sendPicStruts", false);
				sendPicStruts.put("sendPicAddress", "报文有误！请重试！");
				ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(sendPicStruts))); //告知发送者 发送状态
				return;
			}finally {
				tempByteBuf.clear();
			}
			try {
				String fileName = head[0]+"_"+head[1]+"_"+head[3]+"_"+head[4]+"."+head[5];
				NettyFileProtocol nfp = new NettyFileProtocol(sendPicData.length, 2,
						Integer.valueOf(head[0]), Integer.valueOf(head[1]), Long.valueOf(head[3]),
						sendPicData, Integer.valueOf(head[4]), head[5], false);

				//方式一：await()此种方式会被阻塞，长时间阻塞 webSocket客户端会发送不了心跳而断线
//							nettyClient.channelFuture.channel().writeAndFlush(nfp).await();
				//方式二：采用监听channel机制
				ChannelFuture await = nettyClient.channelFuture.channel().writeAndFlush(nfp);
				String[] finalHead = head;
				await.addListener((ChannelFutureListener) future -> {
					if(future.isSuccess()){ // 发送成功
						sendPicStruts.put("id", finalHead[4]);
						sendPicStruts.put("type", 7);
						sendPicStruts.put("sendPicStruts", true);
						sendPicStruts.put("sendPicAddress", staticMessageFilePath+fileName);
						// 发给目标用户
						OneToOneMessage oneToOneMessage = new OneToOneMessage();
						oneToOneMessage.setId(fileName);
						oneToOneMessage.setMsgType("2");
						oneToOneMessage.setFrom(Integer.valueOf(finalHead[0]));
						oneToOneMessage.setTo(Integer.valueOf(finalHead[1]));
						oneToOneMessage.setData(staticMessageFilePath+fileName); // 发送给目标用户的是ngixn静态文件服务器上的图片地址
						oneToOneMessage.setDate(CommonUtil.DateToString(new Date(), ConstantValue.DATE_FORMAT));
						//加入未读集合
						CommonUtil.addunreadHistoryMessage(oneToOneMessage);
						//加入聊天历史集合
						CommonUtil.addAllHistoryMessage(oneToOneMessage);
						ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(sendPicStruts))); //告知发送者 发送状态
						if (CommonUtil.pushCtxMap.containsKey(oneToOneMessage.getTo().toString())) {//找到目标用户
							push(CommonUtil.pushCtxMap.get(oneToOneMessage.getTo().toString()), JSON.toJSONString(oneToOneMessage));//给目标用户发送消息
						} else {//不在线
							CommonUtil.print("消息发送的目标用户不在线！");
						}
					}else{
						future.cause().printStackTrace(); //向文件服务器发送文件 发送失败了
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
				sendPicStruts.put("sendPicStruts", false);
				sendPicStruts.put("sendPicAddress", "发送失败！");
				ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(sendPicStruts))); //告知发送者 发送状态
			}
			return;
		}else if(frame instanceof TextWebSocketFrame) {
			// 客服端发送过来的消息
			request = ((TextWebSocketFrame) frame).text();
			try {
				jsonObject = JSONObject.parseObject(request);
				CommonUtil.print(jsonObject.toJSONString());
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (jsonObject == null) {
				return;
			}
			// 返回应答消息
			/*Map<String, Object> systemMsg = new HashMap<>();
			systemMsg.put("id", "system");
			systemMsg.put("type", -1);
			systemMsg.put("data", "服务器收到并返回了你发送的JSON：" + request);
			ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(systemMsg)));*/

			/**
			 * 聊天业务逻辑处理开始
			 */
			String type = jsonObject.get("type").toString();// 消息类型
			if ("2".equals(type)) {  //JSON定义type=2 ----> 一对一聊天
				String msgType = jsonObject.get("msgType").toString();
				OneToOneMessage oneToOneMessage = new OneToOneMessage();
				oneToOneMessage.setId(jsonObject.get("id").toString());
				oneToOneMessage.setMsgType(msgType);
				oneToOneMessage.setFrom(Integer.valueOf(jsonObject.get("from").toString()));
				oneToOneMessage.setTo(Integer.valueOf(jsonObject.get("to").toString()));
				switch (msgType) {
					case "0":  //文本消息
					case "1":  //表情消息
						oneToOneMessage.setDate(CommonUtil.DateToString(new Date(), ConstantValue.DATE_FORMAT));
						oneToOneMessage.setData(jsonObject.get("data").toString());
						if (CommonUtil.pushCtxMap.containsKey(oneToOneMessage.getTo().toString())) {//找到目标用户
							push(CommonUtil.pushCtxMap.get(oneToOneMessage.getTo().toString()), JSON.toJSONString(oneToOneMessage));
						} else {//不在线
							CommonUtil.print("消息发送的目标用户不在线！");
						}
						//加入未读集合
						CommonUtil.addunreadHistoryMessage(oneToOneMessage);
						//加入聊天历史集合
						CommonUtil.addAllHistoryMessage(oneToOneMessage);
						break;
					case "2": //图片消息
						// 图片消息是二进制消息  走的是 BinaryWebSocketFrame 或者 ContinuationWebSocketFrame
						break;
				}
			} else if ("3".equals(type)) { //客户端要求拉取一对一聊天记录
				List<OneToOneMessage> list = new LinkedList<>();
				if ("0".equals(jsonObject.get("msgDate").toString())) {  //只拉取最近三天的一对一聊天记录
					// 获取 key
					String oneToOneMessageKey = CommonUtil.getOneToOneMessageKey(Integer.valueOf(jsonObject.get("from").toString()), Integer.valueOf(jsonObject.get("to").toString()));
					//聊天记录
					if (CommonUtil.allHistoryMessage.containsKey(oneToOneMessageKey)) {
						list = CommonUtil.allHistoryMessage.get(oneToOneMessageKey);
					}
				} else { //全部记录
					// 巴拉巴拉。。。。。。。。。。。

				}
				//置为0条未读消息
				CommonUtil.unreadHistoryMessage.put(CommonUtil.getOneToOneUnReadMessageKey(Integer.valueOf(jsonObject.get("to").toString()), Integer.valueOf(jsonObject.get("from").toString())), 0);

				Map<String, Object> oneToOneHistoryMessage = new HashMap<>();
				oneToOneHistoryMessage.put("id", "");
				oneToOneHistoryMessage.put("type", 3);
				oneToOneHistoryMessage.put("data", JSON.toJSONString(list));
				ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(oneToOneHistoryMessage)));
			} else if ("4".equals(type)) { // 客户端告知已读此消息
				//置为0条未读消息
				CommonUtil.unreadHistoryMessage.put(CommonUtil.getOneToOneUnReadMessageKey(Integer.valueOf(jsonObject.get("from").toString()), Integer.valueOf(jsonObject.get("to").toString())), 0);
			} else if ("5".equals(type)) {
				// 用户下线通知，这里没有客户端向服务器发送5请求
				// 是zookeeper 监测到 zk上有节点变化后 触发一次 从redis中获取最新在线用户列表并推送到其他用户的聊天界面
			} else if ("6".equals(type)) {
				// 这里客户端发送心跳时 会带上自己的userId和sessionId  可以在这里验证一下
				Map<String, Object> pongToClient = new HashMap<>();
				pongToClient.put("id", "pongTo6");
				pongToClient.put("type", 6);
				pongToClient.put("stauts", true);
				ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(pongToClient)));
			}
			/**
			 * 聊天业务逻辑处理结束
			 */
		}
	}

	// 第一次请求是http请求，请求头包括ws的信息
	public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req)  {
		if (!req.decoderResult().isSuccess()) {
			sendHttpResponse(ctx, req,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}
		//获取http请求的参数
		QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        Map<String, List<String>> paramList = decoder.parameters();
        String msg = "";
        for (Map.Entry<String, List<String>> entry : paramList.entrySet()) {
			CommonUtil.print(entry.getKey()+"----------------"+entry.getValue().get(0));
            msg = entry.getValue().get(0);
        }
        
        JSONObject jsonObject = null;
		try {
			jsonObject = JSONObject.parseObject(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (jsonObject == null) {
			return;
		}
		
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory
                ("ws:/" + ctx.channel() + "/websocket", null, false,65535*10);
		handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			// 不支持
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		} else {
			
			//websocket连接校验  开始
			String sessionId = (String) jsonObject.get("id");
			String userId = (String)jsonObject.get("userId").toString();
			//先不写
			CommonUtil.print("客户端连接成功，上线用户id为："+userId);
			push(ctx, "服务器收到并返回：连接成功！");
			//websocket连接校验  结束
			
			//加入列表
			CommonUtil.pushCtxMap.put(userId, ctx);
			CommonUtil.aaChannelGroup.add(ctx.channel());

			try {
				handshaker.handshake(ctx.channel(), req);
			}catch (Exception e){
				CommonUtil.print("异常连接，过滤。。");
			}

			Map<String,Object> systemMsg = new HashMap<>();
			systemMsg.put("id", "system");
			systemMsg.put("type", -1);
			systemMsg.put("data", "服务器推送消息：登陆成功！！！@@@");
			ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(systemMsg)));
	        
	        //当前登陆用户的联系人列表
			Map<String,Object> ContactsMap = new HashMap<>();
			ContactsMap.put("id", userId);
			ContactsMap.put("type", 0);
			ContactsMap.put("data", CommonUtil.getOneToOneUnReadMessageCount(CommonUtil.contactsList, Integer.valueOf(userId)));
			ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(ContactsMap)));
		}
	}

	public static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
		// 返回应答给客户端
		if (res.status().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
		}
		// 如果是非Keep-Alive，关闭连接
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.status().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static boolean isKeepAlive(FullHttpRequest req) {
		return false;
	}

	// 异常处理，netty默认是关闭channel
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// 输出日志
//		cause.printStackTrace();
//		ctx.close();
	}
}
