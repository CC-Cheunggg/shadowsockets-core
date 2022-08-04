package com.cheung.shadowsocks.proxy;

import com.cheung.shadowsocks.common.ClientProxy;
import com.cheung.shadowsocks.encryption.CryptUtil;
import com.cheung.shadowsocks.encryption.ICrypt;
import com.cheung.shadowsocks.model.SSModel;
import com.cheung.shadowsocks.utils.BootContext;
import com.cheung.shadowsocks.utils.ValidateUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 接受客户端代理发送来的消息
 */
public class ProxyHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    private final ICrypt _crypt;
    private final AtomicReference<Channel> remoteChannel = new AtomicReference<>(null);
    private ChannelHandlerContext channelHandlerContext;
    private final AtomicReference<String> tsn = new AtomicReference<>("");
    private final List<byte[]> cache = new CopyOnWriteArrayList<>();


    public ProxyHandler(SSModel ssModel) {

        // 防止 channel 断开 无法 释放 byteBuf
        this._crypt = ssModel.getCrypt();
        this.channelHandlerContext = ssModel.getChannelHandlerContext();
        ssModel.setData(cache);
        AttributeKey<SSModel> ss_model = AttributeKey.valueOf("ss.model");
        Attribute<SSModel> ssModelAttribute = this.channelHandlerContext.channel().attr(ss_model);
        ssModelAttribute.setIfAbsent(ssModel);
        tsn.compareAndSet("", ssModel.getTsn());
        String message = ValidateUtils.utils.validate(ssModel);
        if (!"".equals(message)) {
            throw new IllegalArgumentException(message);
        }
        init(ssModel.getHost(), ssModel.getPort());
        sendData(ssModel.getPayload(), Boolean.TRUE, ssModel.getTsn());
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ctx.pipeline().remove("shadowsocksDecoder");
    }

    private synchronized void init(final String host, final int port) {

        try {
            ClientProxy channelPool = BootContext.getBeanFactory().getBean(ClientProxy.class);
            ChannelFuture future = channelPool.connect(host, port);
            // 保存客户端连接

            future.addListener(arg0 -> {
                if (future.isSuccess()) {
                    remoteChannel.compareAndSet(null, future.channel());
                    AttributeKey<ChannelHandlerContext> serverChannel = AttributeKey.valueOf("server.channel");
                    Attribute<ChannelHandlerContext> channelAttribute = future.channel().attr(serverChannel);
                    channelAttribute.set(channelHandlerContext);
                } else {
                    future.cancel(true);
                }
            });
        } catch (Exception e) {
            logger.error("connect intenet error", e);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        ByteBuf buff = (ByteBuf) msg;
        if (buff.readableBytes() <= 0) {
            return;
        }

        sendData(CryptUtil.decrypt(_crypt, buff.asReadOnly()), Boolean.FALSE, tsn.get());
        buff.release();

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("ClientProxyHandler channelInactive close client address={}", ctx.channel().remoteAddress());
        ctx.close();
        if (remoteChannel.get() != null) {
            remoteChannel.get().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("ClientProxyHandler error client address=" + ctx.channel().remoteAddress(), cause);
        ctx.close();
        if (remoteChannel.get() != null) {
            remoteChannel.get().close();
        }
    }

    private void sendData(byte[] data, boolean isFlush, String channelId) {
        if (remoteChannel.get() != null && remoteChannel.get().isActive()) {
            logger.info("to: {} ,TSN: {}", remoteChannel.get().remoteAddress().toString(), channelId);
            ByteBuf directBuffer = Unpooled.directBuffer();
            if (isFlush) {
                remoteChannel.get().writeAndFlush(directBuffer.writeBytes(data));
            } else {
                remoteChannel.get().write(directBuffer.writeBytes(data));
            }
        } else {
            cache.add(data);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        if (remoteChannel.get() != null) {
            remoteChannel.get().flush();
        }
    }
}
