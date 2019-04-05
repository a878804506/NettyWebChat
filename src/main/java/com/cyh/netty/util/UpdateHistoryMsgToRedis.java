package com.cyh.netty.util;

import com.cyh.netty.entity.webChat.OneToOneMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map.Entry;

@Component
public class UpdateHistoryMsgToRedis extends Thread{

	@Autowired
	private RedisDB redisDB;

	@Override
	public void run() {
		while(true) {
			Jedis jedis = null;
			try {
				jedis = redisDB.getJedis();
				jedis.select(redisDB.dbSelectedForHistoryMessage);
				// 聊天记录定期存入reids
				synchronized (CommonUtil.allHistoryMessage) {
					if(CommonUtil.allHistoryMessage.size() != 0) {
						for (Entry<String, List<OneToOneMessage>> entry : CommonUtil.allHistoryMessage.entrySet()) {
							jedis.set(entry.getKey().getBytes(), SerializeUtil.serialize(entry.getValue()));
						}
					}
				}
				
				// 一对一未读消息 定期存入redis
				synchronized (CommonUtil.unreadHistoryMessage) {
					if(CommonUtil.unreadHistoryMessage.size() != 0) {
						for (Entry<String, Integer> entry : CommonUtil.unreadHistoryMessage.entrySet()) {
							if( 0 == entry.getValue()) { //0条未读就	清空redis
								jedis.del(entry.getKey());
							}else {
								jedis.set(entry.getKey(), entry.getValue().toString());
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				RedisDB.returnBrokenResource(jedis);
			}finally {
				RedisDB.returnResource(jedis);
				try {
					Thread.sleep(60*60*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
