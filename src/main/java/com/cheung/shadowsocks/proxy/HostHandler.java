package com.cheung.shadowsocks.proxy;

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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.socks.SocksAddressType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 4次握手中的connect阶段，接受shadowsocks-netty发送给shadowsocks-netty-server的消息
 * <p>
 * 具体数据的读取可以参考类：SocksCmdRequest
 */
public class HostHandler extends ChannelInboundHandlerAdapter {

//    private static final FileBasedConfigurationBuilder<FileBasedConfiguration> blacklist;
//    private static final FileBasedConfigurationBuilder<FileBasedConfiguration> heartbeatPacket;
    private final AtomicBoolean isUser = new AtomicBoolean(Boolean.FALSE);

    static {
//        // 读取黑名单文件
//        Parameters paramsBlacklist = new Parameters();
//        blacklist =
//                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
//                        .configure(paramsBlacklist.properties()
//                                .setFileName(ClassLoader.getSystemResource("blacklist.properties").getFile()));
//        blacklist.setAutoSave(true);
//        // 读取记录心跳文件
//        Parameters paramsHeartbeatPacket = new Parameters();
//        heartbeatPacket =
//                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
//                        .configure(paramsHeartbeatPacket.properties()
//                                .setFileName(ClassLoader.getSystemResource("heartbeatPacket.properties").getFile()));
//        heartbeatPacket.setAutoSave(true);

        config = ConfigXmlLoader.loader.load();

    }

    private static final Logger logger = LoggerFactory.getLogger(HostHandler.class);
    private ICrypt _crypt = CryptFactory.factory.get(config.getMethod(), config.getPassword());
    private boolean isTrack = config.isTrack();
    private static Config config;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        logger.error("HostHandler error  client address=" + ctx.channel().remoteAddress(), cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
        logger.info("HostHandler channelInactive close client address={}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

//        if (checkBlacklist(ctx, false)) {
//            ByteBuf buff = (ByteBuf) msg;
//            decode(ctx, buff);
//        } else {
//            // 超过三次 密码错误 将不再允许访问
//            ctx.channel().close();
//            ctx.close();
//        }

        ByteBuf buff = (ByteBuf) msg;
        decode(ctx, buff);



    }


//    private boolean checkBlacklist(ChannelHandlerContext ctx, boolean isOpenBlacklist) throws IOException, ConfigurationException {
//
//        if (isOpenBlacklist) {
//            Configuration config = blacklist.getConfiguration();
//            String ip = ctx.channel().remoteAddress().toString()
//                    .substring(1, ctx.channel().remoteAddress().toString().indexOf(":"));
//            int countBlacklist = config.getInt(ip, 0);
//
//            if (isTrack) {
//                logger.info("{} 密码错误次数为: {} 次", ip, countBlacklist);
//            }
//
//            return config.getInt(ip, 0) < 3;
//        } else {
//            return true;
//        }
//    }

//    private boolean checkHeartbeatPacket(int data) throws ConfigurationException {
//        Configuration config = heartbeatPacket.getConfiguration();
//        String packets = config.getString("HeartbeatPacket", "");
//        if (StringUtils.indexOf(packets, ",") == -1) {
//            return config.getInt("HeartbeatPacket", 0) == data;
//        }
//        String[] packet = StringUtils.split(packets, ",");
//
//        for (String p : packet) {
//            int pInt = Integer.parseInt(p);
//            if (pInt == data) {
//                return true;
//            }
//        }
//        return false;
//    }

    private synchronized void decode(ChannelHandlerContext ctx, ByteBuf buff) {

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
                return;
            }

            ByteBuf copy = Unpooled.copiedBuffer(dataBuff);
            logger.info("hex=======>>>> " + ByteBufUtil.hexDump(copy)+"  handler ===>>> "+toString());

            // 读索引 不会改变
            byte addressType = dataBuff.getByte(0);
            SocksAddressType socksAddressType = SocksAddressType.valueOf(addressType);

            // 手动 改变 读索引
            dataBuff.readerIndex(1);
            switch (socksAddressType) {
                case IPv4: {
                    addressTypeString = SocksAddressType.IPv4.toString();
                    if (dataBuff.readableBytes() < 7) {
                        return;
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
                        return;
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
                        return;
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
                    return;
                }
            }

            GroovyUtils.invokeMethod(codeText, "loggerHandler", ctx, buff, addressTypeString, host, port, isTrack);
            GroovyUtils.clearGroovyClassLoaderCache();

            EventLoopGroup eventExecutors = BootContext.getBeanFactory().getBean("eventExecutors", EventLoopGroup.class);
            ctx.channel().pipeline().addLast(eventExecutors, new ClientProxyHandler(host, port, dataBuff, ctx, _crypt));
            ctx.channel().pipeline().remove(this);

        } catch (Exception e) {
            // 不出异常时 dataBuff 被传到下一个 handler 无需释放
            CryptUtil.releaseByteBufAllRefCnt(dataBuff);
            logger.error("解析出错 !!!", e);
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