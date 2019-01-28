package com.cheung.shadowsockets.proxy;

import com.cheung.shadowsockets.encryption.CryptUtil;
import com.cheung.shadowsockets.encryption.ICrypt;
import com.cheung.shadowsockets.model.SSModel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接受互联网消息处理
 */
public class InternetDataHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(InternetDataHandler.class);

    private ChannelHandlerContext clientProxyChannel;
    private ICrypt _crypt;
    private volatile CompositeByteBuf cacheBuffer;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        AttributeKey<ChannelHandlerContext> serverChannel = AttributeKey.valueOf("server.channel");
        AttributeKey<SSModel> ssModel = AttributeKey.valueOf("ss.model");

        Attribute<ChannelHandlerContext> channelAttribute = ctx.channel().attr(serverChannel);
        Attribute<SSModel> ssModelAttribute = channelAttribute.get().channel().attr(ssModel);

        this.clientProxyChannel = channelAttribute.get();
        this._crypt = ssModelAttribute.get().getCrypt();
        this.cacheBuffer = CompositeByteBuf.class.cast(ssModelAttribute.get().getData());

        Channel channel = ctx.channel();
        for (ByteBuf msg : this.cacheBuffer) {
            channel.writeAndFlush(msg);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        ByteBuf data = ByteBuf.class.cast(msg);
        byte[] deData = CryptUtil.encrypt(_crypt, data);

        Channel channel = clientProxyChannel.channel();

        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(Unpooled.copiedBuffer(deData));
        } else {
            ctx.close();
        }

        data.release();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        Channel channel = clientProxyChannel.channel();

        // 等待 发送缓冲区 的 数据发送完 为减少 Connection reset by peer
        while ((channel != null) && (channel.isActive()) && (!(channel.unsafe().outboundBuffer().isEmpty()))) {
            Thread.sleep(2000);
        }

        ctx.close();
        clientProxyChannel.close();
        logger.info("InternetDataHandler channelInactive close  Interview address = {}", ctx.channel().remoteAddress());


    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 确保清空 cacheBuffer
        ReferenceCountUtil.safeRelease(this.cacheBuffer);

        ctx.close();
        clientProxyChannel.close();
        logger.error("InternetDataHandler error Interview address = " + ctx.channel().remoteAddress(), cause);
    }

}
