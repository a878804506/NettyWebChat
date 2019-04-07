package com.cyh.netty.entity.webChat;

import java.io.Serializable;

/**
 *  一对一聊天实体
 * @author cyh
 *
 */
public class OneToOneMessage  implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String id ; // 当发送图片时 是前端发过来的uuid
	private String msgType ;  // 消息类型 0：文本     1：表情     2：图片
	private Integer from;  //消息发出方
	private Integer to;   // 消息接收方
	private String data;  //消息体
	private String date; //消息产生的时间
	private String type = "2"; //json类型  一对一聊天json 默认为2
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getMsgType() {
		return msgType;
	}
	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}
	
	public Integer getFrom() {
		return from;
	}
	public void setFrom(Integer from) {
		this.from = from;
	}
	public Integer getTo() {
		return to;
	}
	public void setTo(Integer to) {
		this.to = to;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	
}
