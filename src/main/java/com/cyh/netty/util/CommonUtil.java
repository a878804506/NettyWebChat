package com.cyh.netty.util;

import com.cyh.netty.entity.OneToOneMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  公共静态方法 工具类
 */
public class CommonUtil {

    /**
     *
     * @param date  传入的时间
     * @param format 需要格式化的 标准字符串 如 yyyy-MM-dd
     * @return 格式化后的时间字符串
     */
    public static String DateToString(Date date,String format){
        return new SimpleDateFormat(format).format(date);
    }

    //存放所有的ChannelHandlerContext
    //key : userId
    public static Map<String, ChannelHandlerContext> pushCtxMap = new ConcurrentHashMap<String, ChannelHandlerContext>() ;

    //存放某一类的channel
    public static ChannelGroup aaChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    //redis中所有的聊天记录  服务启动加载redis中的聊天记录
    public static Map<String,List<OneToOneMessage>> allHistoryMessage = new HashMap<String, List<OneToOneMessage>>();

    //未读的聊天消息   key : from_To_to_unread    value : 未读消息条数
    public static Map<String,Integer> unreadHistoryMessage = new HashMap<String, Integer>();

    //联系人列表
    public static List<Map<String,Object>> contactsList = new LinkedList<Map<String, Object>>();

    //生成 一对一聊天的 redis key
    public static String getOneToOneMessageKey(Integer fromUserId,Integer toUserId) {
        String results = "history_";
        if(fromUserId.compareTo(toUserId) > 0) {      //str1的字典序大于str2的字典序，则交换两者变量
            results += fromUserId+"_to_"+toUserId;
        }else {
            results += toUserId+"_to_"+fromUserId;
        }
        return results;
    }

    //生成 一对一 未读聊天的  key    from_To_to_unread
    public static String getOneToOneUnReadMessageKey(Integer fromUserId,Integer toUserId) {
        return "unread_"+fromUserId+"_to_"+toUserId;
    }

    /**
     * 返回带有未读条数 的最新联系人列表
     * @param contactsList  最新联系人列表
     * @param toUserId  当前登陆用户
     * @return
     */
    public static List<Map<String,Object>> getOneToOneUnReadMessageCount(List<Map<String,Object>> contactsList ,Integer toUserId) {
        for (Map<String, Object> temp : contactsList) {
            String OneToOneUnReadMessageCountKey = getOneToOneUnReadMessageKey(Integer.valueOf(temp.get("id").toString()),toUserId);
            if(unreadHistoryMessage.containsKey(OneToOneUnReadMessageCountKey)) {
                temp.put("unread", unreadHistoryMessage.get(OneToOneUnReadMessageCountKey));
            }else {
                temp.put("unread", 0);
            }
        }
        return contactsList;
    }

    //加入聊天历史集合
    public static void addAllHistoryMessage(OneToOneMessage oneToOneMessage) {
        // 获取 key
        String oneToOneMessageKey = getOneToOneMessageKey(oneToOneMessage.getFrom(),oneToOneMessage.getTo());
        //聊天记录
        List<OneToOneMessage> list = new LinkedList<>();
        if(allHistoryMessage.containsKey(oneToOneMessageKey)) {
            list = (List<OneToOneMessage>) allHistoryMessage.get(oneToOneMessageKey);
        }
        //加入最新聊天记录 并再次存入历史聊天集合中
        list.add(oneToOneMessage);
        synchronized (allHistoryMessage) {  //存入时加锁
            allHistoryMessage.put(oneToOneMessageKey, list);
        }
    }

    //加入未读集合
    public static void addunreadHistoryMessage(OneToOneMessage oneToOneMessage) {
        // 获取 key
        String oneToOneMessageCountKey = getOneToOneUnReadMessageKey(oneToOneMessage.getFrom(),oneToOneMessage.getTo());
        // 获取未读聊天记录条数  然后加1
        int unReadMsgCount = 0;
        if(unreadHistoryMessage.containsKey(oneToOneMessageCountKey)) {
            unReadMsgCount =  unreadHistoryMessage.get(oneToOneMessageCountKey) +1;
        }else {
            unReadMsgCount = 1;
        }
        synchronized (unreadHistoryMessage) {  //存入时加锁
            unreadHistoryMessage.put(oneToOneMessageCountKey, unReadMsgCount);
        }
    }
}
