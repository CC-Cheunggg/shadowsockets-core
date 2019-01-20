package com.cheung.shadowsockets.proxy;

import com.cheung.shadowsockets.encryption.CryptUtil;
import com.cheung.shadowsockets.encryption.ICrypt;
import com.cheung.shadowsockets.model.SSModel;
import com.cheung.shadowsockets.pool.ChannelPool;
import com.cheung.shadowsockets.pool.CommonResources;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接受客户端代理发送来的消息
 */
public class ClientProxyHandler extends ChannelInboundHandlerAdapter {

    private static Logger logger = LoggerFactory.getLogger(ClientProxyHandler.class);
    private final ICrypt _crypt;
    //private final AtomicReference<Channel> remoteChannel = new AtomicReference<>();
    private volatile Channel remoteChannel = null;
    private CompositeByteBuf clientCache = Unpooled.compositeBuffer();
    private ChannelHandlerContext channelHandlerContext;


    ClientProxyHandler(String host, int port, ByteBuf clientCache, ChannelHandlerContext channelHandlerContext, ICrypt _crypt) {
        this._crypt = _crypt;
        this.clientCache.addComponent(clientCache);
        this.channelHandlerContext = channelHandlerContext;
        Attribute<SSModel> ssModelAttribute = channelHandlerContext.channel().attr(CommonResources.SS_MODEL);
        SSModel model = new SSModel();
        model.setChannelHandlerContext(this.channelHandlerContext);
        model.setCrypt(this._crypt);
        model.setData(this.clientCache);
        ssModelAttribute.setIfAbsent(model);
        init(host, port);
    }

    private void init(final String host, final int port) {

        try {
            ChannelFuture future = ChannelPool.connect(host, port, new InternetDataHandler());
            // 保存客户端连接
            future.addListener(arg0 -> {
                if (future.isSuccess()) {
                    remoteChannel = future.channel();
                    Attribute<ChannelHandlerContext> channelAttribute = future.channel().attr(CommonResources.SERVER_CHANNEL);
                    channelAttribute.set(channelHandlerContext);
                } else {
                    future.cancel(true);
                }
            });
//            // 监听 第三方连接 是否 已经活动 满一天 是则关闭 防止 系统连接数瓶颈
//            future.addListener((ChannelFutureListener) future1 -> {
//                future1.channel().eventLoop().schedule(() -> {
//                    if (future1.channel().isActive()) {
//                        clientCache.release();
//                        future1.channel().pipeline().close();
//                    }
//                }, 1, TimeUnit.DAYS);
//            });
        } catch (Exception e) {
            logger.error("connect intenet error", e);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ByteBuf buff = (ByteBuf) msg;
        if (buff.readableBytes() <= 0) {
            return;
        }

        byte[] decrypt = CryptUtil.decrypt(_crypt, buff);

        if (remoteChannel != null && remoteChannel.isActive()) {
            remoteChannel.writeAndFlush(Unpooled.copiedBuffer(decrypt));
            buff.release();
        } else {
            this.clientCache.addComponent(buff);
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("ClientProxyHandler channelInactive close client address={}", ctx.channel().remoteAddress());
        ctx.close();
        if (remoteChannel != null) {
            remoteChannel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("ClientProxyHandler error client address=" + ctx.channel().remoteAddress(), cause);
        ctx.close();
        if (remoteChannel != null) {
            remoteChannel.close();
        }
    }
}
