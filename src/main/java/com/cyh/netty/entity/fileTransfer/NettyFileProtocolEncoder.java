package com.cyh.netty.entity.fileTransfer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * 自定义协议的编码器
 */
public class NettyFileProtocolEncoder extends MessageToByteEncoder<NettyFileProtocol> {

    @Override
    protected void encode(ChannelHandlerContext ctx, NettyFileProtocol msg, ByteBuf out) throws Exception {
        /*// 写入消息NettyFileProtocol的具体内容
        // 1.写入消息的开头的信息标志(int类型)
        out.writeInt(msg.getHead_data());
        // 2.写入消息的长度(int 类型)
        out.writeInt(msg.getContentLength());
        // 3.写入消息的类型  定义: 2->图片   3->文件
        out.writeInt(msg.getContentType());
        // 4.写入消息发送者id
        out.writeInt(msg.getFronUserId());
        // 5.写入消息接收者id
        out.writeInt(msg.getToUserId());
        // 6.写入消息时间戳
        out.writeLong(msg.getTime());
        // 7.写入消息的内容
        out.writeBytes(msg.getContent());*/

        byte[] datas = objectToByte(msg);
        out.writeBytes(datas);
        ctx.flush();
    }

    public static byte[] objectToByte(Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = null;
        try {
            oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);
            bytes = bo.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                bo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                oo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }
}
