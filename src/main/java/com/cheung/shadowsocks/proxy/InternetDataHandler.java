package com.cheung.shadowsocks.proxy;

import com.cheung.shadowsocks.encryption.CryptUtil;
import com.cheung.shadowsocks.encryption.ICrypt;
import com.cheung.shadowsocks.model.SSModel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * 接受互联网消息处理
 */
@Sharable
public class InternetDataHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(InternetDataHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        SSModel model = getSSModel(ctx);
        List<byte[]> cache = model.getData();

        if (cache != null) {
            for (byte[] data : cache) {
                logger.info("to: {} ,TSN: {}", ctx.channel().remoteAddress().toString(), model.getTsn());
                ctx.channel().writeAndFlush(Unpooled.copiedBuffer(data));
            }
        }
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        ChannelHandlerContext clientProxyChannel = getClientProxyChannel(ctx);
        ICrypt _crypt = getCrypt(ctx);

        ByteBuf data = (ByteBuf) msg;
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

        ChannelHandlerContext context = getClientProxyChannel(ctx);

        // 等待 发送缓冲区 的 数据发送完 为减少 Connection reset by peer
//        while ((channel != null) && (channel.isActive()) && (!(channel.unsafe().outboundBuffer().isEmpty()))) {
//            Thread.sleep(2000);
//        }

        ctx.close();
        context.close();
        logger.info("InternetDataHandler channelInactive close  Interview address = {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        ctx.close();
        getClientProxyChannel(ctx).close();
        logger.error("InternetDataHandler error Interview address = " + ctx.channel().remoteAddress(), cause);
    }

    private ChannelHandlerContext getClientProxyChannel(ChannelHandlerContext ctx) {
        AttributeKey<ChannelHandlerContext> serverChannel = AttributeKey.valueOf("server.channel");
        Attribute<ChannelHandlerContext> channelAttribute = ctx.channel().attr(serverChannel);
        return channelAttribute.get();
    }

    private ICrypt getCrypt(ChannelHandlerContext ctx) {
        AttributeKey<SSModel> ssModel = AttributeKey.valueOf("ss.model");
        Optional<ChannelHandlerContext> channelHandlerContextOptional = Optional.ofNullable(getClientProxyChannel(ctx));
        if (!channelHandlerContextOptional.isPresent()) {
            return null;
        }
        Optional<Channel> channelOptional = Optional.ofNullable(getClientProxyChannel(ctx).channel());
        if (!channelOptional.isPresent()) {
            return null;
        }
        Attribute<SSModel> ssModelAttribute = getClientProxyChannel(ctx).channel().attr(ssModel);
        return ssModelAttribute.get().getCrypt();
    }

    private SSModel getSSModel(ChannelHandlerContext ctx) {
        AttributeKey<SSModel> ssModel = AttributeKey.valueOf("ss.model");
        Optional<ChannelHandlerContext> channelHandlerContextOptional = Optional.ofNullable(getClientProxyChannel(ctx));
        if (!channelHandlerContextOptional.isPresent()) {
            throw new IllegalStateException("ss model is null");
        }
        Optional<Channel> channelOptional = Optional.ofNullable(getClientProxyChannel(ctx).channel());
        if (!channelOptional.isPresent()) {
            throw new IllegalStateException("ss model is null");
        }
        Attribute<SSModel> ssModelAttribute = getClientProxyChannel(ctx).channel().attr(ssModel);
        return ssModelAttribute.get();
    }
}
