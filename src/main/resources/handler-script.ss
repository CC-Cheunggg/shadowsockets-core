package com.cheung.shadowsockets;

import com.cheung.shadowsockets.pool.CommonResources;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class DataHandler {

    private static Logger logger = LoggerFactory.getLogger(DataHandler.class);


    public void unknownAddressTypeHandler(ChannelHandlerContext ctx, ByteBuf dataBuff, int addressType, String userName, boolean isTrack) throws Exception {
        if (isTrack) {
            logger.warn(userName + "'s unknown address type: " + addressType + " client address: " + ctx.channel().remoteAddress());
            logger.info("error data: {} ,data length :{} ,hex string :{}", dataBuff.toString(Charset.forName("utf-8"))
                    , dataBuff.readableBytes(), ByteBufUtil.hexDump(dataBuff));
        } else {
            logger.warn("unknown address type: " + addressType + " client address: " + ctx.channel().remoteAddress());
            logger.info("error data: {} ,data length :{} ,hex string :{}", dataBuff.toString(Charset.forName("utf-8"))
                    , dataBuff.readableBytes(), ByteBufUtil.hexDump(dataBuff));
        }

        ctx.channel().close();
        ctx.close();
    }

    public void loggerHandler(ChannelHandlerContext ctx, ByteBuf dataBuff, String addressTypeString, String userName, String host, int port, boolean isTrack) throws Exception {
        if (isTrack) {
            Thread.currentThread().getStackTrace()[1].getLineNumber();
            logger.info("userName = " + userName + " =>  client address = " + ctx.channel().remoteAddress() + "  interview address = "
                    + new InetSocketAddress(host, port));
        }
    }


}