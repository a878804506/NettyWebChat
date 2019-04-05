package com.cyh.netty.entity.fileTransfer;

import com.cyh.netty.constant.ConstantValue;

import java.util.Arrays;

/**
 * 自己定义的协议
 *  数据包格式
 * +——----——+——-----+—---------+---------------+---------------+-------------+-—-——----+
 * |协议开始标志|  长度   |   类型    |   发送者id    |   接收者id    |   时间戳    |   数据     |
 * |     4      |  4      |   4       |      4        |      4        |     8       |     x      |   数据长度（单位：字节）
 * +——----——+——-----+-----------+---------------+---------------+-------------+------------+
 */
public class NettyFileProtocol implements java.io.Serializable{
    private static final long serialVersionUID = 1L;
    //消息的开头的信息标志
    private int head_data = ConstantValue.HEAD_DATA;
    //消息的长度
    private int contentLength;
    //消息的类型  定义: -1->心跳   2->图片   3->文件
    private int contentType;
    //消息发送者id
    private int fronUserId;
    //消息接收者id
    private int toUserId;
    //消息时间戳
    private long time;
    //消息的内容
    private byte[] content;
    //结果
    private boolean result;

    public NettyFileProtocol() {
    }

    public NettyFileProtocol(int contentLength, int contentType, int fronUserId, int toUserId, long time, byte[] content,boolean result) {
        this.contentLength = contentLength;
        this.contentType = contentType;
        this.fronUserId = fronUserId;
        this.toUserId = toUserId;
        this.time = time;
        this.content = content;
        this.result = result;
    }

    public int getHead_data() {
        return head_data;
    }

    public void setHead_data(int head_data) {
        this.head_data = head_data;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    public int getFronUserId() {
        return fronUserId;
    }

    public void setFronUserId(int fronUserId) {
        this.fronUserId = fronUserId;
    }

    public int getToUserId() {
        return toUserId;
    }

    public void setToUserId(int toUserId) {
        this.toUserId = toUserId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "NettyFileProtocol{" +
                "head_data=" + head_data +
                ", contentLength=" + contentLength +
                ", contentType=" + contentType +
                ", fronUserId=" + fronUserId +
                ", toUserId=" + toUserId +
                ", time=" + time +
                ", content=" + Arrays.toString(content) +
                ", result=" + result +
                '}';
    }
}
