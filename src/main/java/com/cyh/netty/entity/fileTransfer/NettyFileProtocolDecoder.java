package com.cyh.netty.entity.fileTransfer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

/**
 * 自定义协议的解码器
 */
public class NettyFileProtocolDecoder extends ByteToMessageDecoder {

    /**
     * 协议开始的标准head_data，int类型，占据4个字节.
     * 表示数据的长度contentLength，int类型，占据4个字节.
     * 发送者id  int类型，占据4个字节.
     * 接收者id  int类型，占据4个字节.
     * 时间戳    long类型，占据8个字节.
     */
    /*public final int BASE_LENGTH = 4 + 4 + 4 + 4 + 8;*/

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        Object obj = byteToObject(bytes);
        out.add(obj);
    }

    public static Object byteToObject(byte[] bytes) {
        Object obj = null;
        ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
        ObjectInputStream oi = null;
        try {
            oi = new ObjectInputStream(bi);
            obj = oi.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                bi.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                oi.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return obj;
    }
}
