package com.cheung.shadowsocks.proxy;

import com.cheung.shadowsocks.common.ClientProxy;
import com.cheung.shadowsocks.config.Config;
import com.cheung.shadowsocks.config.ConfigXmlLoader;
import com.cheung.shadowsocks.encryption.CryptFactory;
import com.cheung.shadowsocks.encryption.CryptUtil;
import com.cheung.shadowsocks.encryption.ICrypt;
import com.cheung.shadowsocks.groovy.GroovyUtils;
import com.cheung.shadowsocks.utils.BootContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 4次握手中的connect阶段，接受shadowsocks-netty发送给shadowsocks-netty-server的消息
 * <p>
 * 具体数据的读取可以参考类：SocksCmdRequest
 */
@Deprecated
public class HostHandler extends ChannelInboundHandlerAdapter {


    private final AtomicBoolean isFirstRead = new AtomicBoolean(Boolean.TRUE);
    private final ReentrantLock lock = new ReentrantLock();
    //private final List<byte[]> clientCache = Lists.newCopyOnWriteArrayList();



    static {
        config = ConfigXmlLoader.loader.load();
    }

    private static final Logger logger = LoggerFactory.getLogger(HostHandler.class);
    private ICrypt _crypt = CryptFactory.factory.get(config.getMethod(), config.getPassword());
    private boolean isTrack = config.isTrack();
    private static Config config;

    private void channelRead0(ByteBuf msg) {

        if (msg.readableBytes() <= 0) {
            return;
        }

        byte[] decrypt = CryptUtil.decrypt(_crypt, msg);
//        if (remoteChannel != null && remoteChannel.isActive()) {
//            remoteChannel.writeAndFlush(Unpooled.copiedBuffer(decrypt));
//        }

        msg.release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        logger.error("HostHandler error  client address=" + ctx.channel().remoteAddress(), cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.close();
        logger.info("HostHandler channelInactive close client address={}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        logger.info(msg.toString());
        logger.info("$$$$$$$$$$$$$$$$$$$$$$$$$ " + Thread.currentThread().getName());

        try {
            lock.lock();
            if (isFirstRead.get()) {
                logger.info("T ===>" + msg.toString());
                ByteBuf buff = (ByteBuf) msg;
                Channel channel = decode(ctx, buff);
                logger.info("channel =========>>>> " + channel);

            } else {
                logger.info("F ===>" + msg.toString());
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }


    }


//

    private synchronized Channel init(final String host, final int port, final ChannelHandlerContext ctx) {

        logger.info("######################## " + Thread.currentThread().getName());
        try {
            ClientProxy channelPool = BootContext.getBeanFactory().getBean(ClientProxy.class);
            ChannelFuture future = channelPool.connect(host, port);
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Channel> remoteChannel = new AtomicReference<>(null);
            // 保存客户端连接
            future.addListener(arg0 -> {
                logger.info("******************* " + Thread.currentThread().getName());
                if (future.isSuccess()) {
                    remoteChannel.compareAndSet(null, future.channel());
                    AttributeKey<ChannelHandlerContext> serverChannel = AttributeKey.valueOf("server.channel");
                    Attribute<ChannelHandlerContext> channelAttribute = future.channel().attr(serverChannel);
                    channelAttribute.set(ctx);
                } else {
                    future.cancel(true);
                }
                latch.countDown();
            });
            latch.await();
            logger.info("test ----------->" + remoteChannel.get().toString());
            return remoteChannel.get();
        } catch (Exception e) {
            logger.error("connect intenet error", e);
            return null;
        }
    }

    private synchronized Channel decode(ChannelHandlerContext ctx, ByteBuf buff) {

        if (!isFirstRead.compareAndSet(Boolean.TRUE, Boolean.FALSE)) {
        }


        int maxAddressLength = 1 + 1 + 255 + 2;
        ByteBuf dataBuff = Unpooled.buffer(maxAddressLength);
        String host;
        int port;
        // 用作拆包
        String addressTypeString;

        try {
            String codeText = GroovyUtils.readCodeTextInputStream(ClassLoader.getSystemResourceAsStream("handler-script.ss"), "UTF-8");

            dataBuff.writeBytes(CryptUtil.decrypt(_crypt, buff));

            if (dataBuff.readableBytes() < 2) {
                return null;
            }

            ByteBuf copy = Unpooled.copiedBuffer(dataBuff);
            logger.info("hex=======>>>> " + ByteBufUtil.hexDump(copy) + "  handler ===>>> " + toString());

            // 读索引 不会改变
            byte addressType = dataBuff.getByte(0);
            SocksAddressType socksAddressType = SocksAddressType.valueOf(addressType);

            // 手动 改变 读索引
            dataBuff.readerIndex(1);
            switch (socksAddressType) {
                case IPv4: {
                    addressTypeString = SocksAddressType.IPv4.toString();
                    if (dataBuff.readableBytes() < 7) {
                        return null;
                    }

                    byte[] ipBytes = new byte[4];
                    dataBuff.readBytes(ipBytes);
                    // 必须截取掉 第一位的 '/' 字符
                    host = Inet4Address.getByAddress(ipBytes).toString().substring(1);
                    port = dataBuff.readShort();
                    break;
                }
                case DOMAIN: {
                    addressTypeString = SocksAddressType.DOMAIN.toString();

                    // 读索引 不会改变
                    int hostLength = dataBuff.getByte(1);
                    // 手动 改变 读索引
                    dataBuff.readerIndex(2);
                    if (dataBuff.readableBytes() < hostLength + 4) {

                        return null;
                    }

                    byte[] hostBytes = new byte[hostLength];
                    dataBuff.readBytes(hostBytes);
                    host = new String(hostBytes);
                    port = dataBuff.readShort();
                    break;
                }
                case IPv6: {
                    addressTypeString = SocksAddressType.IPv6.toString();

                    if (dataBuff.readableBytes() < 19) {
                        return null;
                    }

                    byte[] hostBytes = new byte[16];
                    dataBuff.readBytes(hostBytes);
                    // 必须截取掉 第一位的 '/' 字符
                    host = Inet6Address.getByAddress(hostBytes).toString().substring(1);
                    port = dataBuff.readShort();
                    break;
                }
                default: {
                    GroovyUtils.invokeMethod(codeText, "unknownAddressTypeHandler", ctx, buff, addressType, "", isTrack);
                    GroovyUtils.clearGroovyClassLoaderCache();

                    // 遇到不完整的包 就释放掉
                    CryptUtil.releaseByteBufAllRefCnt(dataBuff);
                    ctx.close();
                    return null;
                }
            }

            GroovyUtils.invokeMethod(codeText, "loggerHandler", ctx, buff, addressTypeString, host, port, isTrack);
            GroovyUtils.clearGroovyClassLoaderCache();

            return init(host, port, ctx);

        } catch (Exception e) {
            // 不出异常时 dataBuff 被传到下一个 handler 无需释放
            CryptUtil.releaseByteBufAllRefCnt(dataBuff);
            logger.error("解析出错 !!!", e);
            return null;
        } finally {
            CryptUtil.releaseByteBufAllRefCnt(buff);
        }

    }


    //    private void addBlacklist(ChannelHandlerContext ctx) throws ConfigurationException {
//        Configuration config = blacklist.getConfiguration();
//        String ip = ctx.channel().remoteAddress().toString().substring(1, ctx.channel().remoteAddress().toString().indexOf(":"));
//        int countBlacklist = config.getInt(ip, 0) + 1;
//        config.setProperty(ip, countBlacklist);
//    }

}