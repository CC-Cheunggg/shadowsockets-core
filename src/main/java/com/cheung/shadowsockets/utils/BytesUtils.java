package com.cheung.shadowsockets.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public enum BytesUtils {

    utils;

    public byte[] byteBuf2Bytes(ByteBuf byteBuf) {
        byte[] data = new byte[byteBuf.readableBytes()];
        if (byteBuf.readableBytes() == 0) {
            return new byte[0];
        }
        byteBuf.readBytes(data);
        return data;
    }

    public ByteBuf bytes2ByteBuf(byte[] data){
        return Unpooled.copiedBuffer(data);
    }
}
