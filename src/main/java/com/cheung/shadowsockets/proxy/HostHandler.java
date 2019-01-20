package com.cheung.shadowsockets.proxy;

import com.cheung.shadowsockets.config.ConfigXmlLoader;
import com.cheung.shadowsockets.config.User;
import com.cheung.shadowsockets.encryption.CryptFactory;
import com.cheung.shadowsockets.encryption.CryptUtil;
import com.cheung.shadowsockets.encryption.ICrypt;
import com.cheung.shadowsockets.groovy.GroovyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.nio.charset.Charset;

import static com.cheung.shadowsockets.pool.CommonResources.eventExecutors;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 4次握手中的connect阶段，接受shadowsocks-netty发送给shadowsocks-netty-server的消息
 * <p>
 * 具体数据的读取可以参考类：SocksCmdRequest
 */
public class HostHandler extends ChannelInboundHandlerAdapter {

    private static final FileBasedConfigurationBuilder<FileBasedConfiguration> blacklist;
    private static final FileBasedConfigurationBuilder<FileBasedConfiguration> heartbeatPacket;

    static {
        // 读取黑名单文件
        Parameters paramsBlacklist = new Parameters();
        blacklist =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(paramsBlacklist.properties()
                                .setFileName(ClassLoader.getSystemResource("blacklist.properties").getFile()));
        blacklist.setAutoSave(true);
        // 读取记录心跳文件
        Parameters paramsHeartbeatPacket = new Parameters();
        heartbeatPacket =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(paramsHeartbeatPacket.properties()
                                .setFileName(ClassLoader.getSystemResource("heartbeatPacket.properties").getFile()));
        heartbeatPacket.setAutoSave(true);
    }

    private static Logger logger = LoggerFactory.getLogger(HostHandler.class);
    private ICrypt _crypt;
    private boolean isTrack = false;

    public HostHandler(User user, boolean isTrack) {
        this._crypt = CryptFactory.factory.get(user.getMethod(), user.getPassword());
        this.isTrack = isTrack;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        ctx.close();
        logger.error("HostHandler error  client address=" + ctx.channel().remoteAddress(), cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().close();
        ctx.close();
        logger.info("HostHandler channelInactive close client address={}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (checkBlacklist(ctx, false)) {
            ByteBuf buff = (ByteBuf) msg;
            decode(ctx, buff);
        } else {
            // 超过三次 密码错误 将不再允许访问
            ctx.channel().close();
            ctx.close();
        }

    }


    private boolean checkBlacklist(ChannelHandlerContext ctx, boolean isOpenBlacklist) throws IOException, ConfigurationException {

        if (isOpenBlacklist) {
            Configuration config = blacklist.getConfiguration();
            String ip = ctx.channel().remoteAddress().toString()
                    .substring(1, ctx.channel().remoteAddress().toString().indexOf(":"));
            int countBlacklist = config.getInt(ip, 0);

            if (isTrack) {
                logger.info("{} 密码错误次数为: {} 次", ip, countBlacklist);
            }

            return config.getInt(ip, 0) < 3;
        } else {
            return true;
        }
    }

    private boolean checkHeartbeatPacket(int data) throws ConfigurationException {
        Configuration config = heartbeatPacket.getConfiguration();
        String packets = config.getString("HeartbeatPacket", "");
        if (StringUtils.indexOf(packets, ",") == -1) {
            return config.getInt("HeartbeatPacket", 0) == data;
        }
        String[] packet = StringUtils.split(packets, ",");

        for (String p : packet) {
            int pInt = Integer.parseInt(p);
            if (pInt == data) {
                return true;
            }
        }
        return false;
    }

    private void decode(ChannelHandlerContext ctx, ByteBuf buff) {

        ByteBuf dataBuff = Unpooled.buffer();
        String host;
        int port;
        String addressTypeString;

        try {
            String codeText = GroovyUtils.readCodeTextInputStream(ClassLoader.getSystemResourceAsStream("handler-script.ss"), "UTF-8");
            String userName = null;
            if (isTrack) {

                // 为了便于查询故障
                String userAddress = ctx.channel().localAddress().toString();
                int userPort = Integer.parseInt(userAddress.substring(userAddress.indexOf(":") + 1, userAddress.length()));
                userName = ConfigXmlLoader.loader.getUserMapper().get(userPort);

            }

            if (buff.readableBytes() <= 0) {
                return;
            }

            dataBuff.writeBytes(CryptUtil.decrypt(_crypt, buff));
            if (dataBuff.readableBytes() < 2) {
                return;
            }

            // 读索引 不会改变
            int addressType = dataBuff.getUnsignedByte(0);

            // 手动 改变 读索引
            dataBuff.readerIndex(1);
            if (addressType == SocksAddressType.IPv4.byteValue()) {
                addressTypeString = SocksAddressType.IPv4.toString();
                if (dataBuff.readableBytes() < 7) {
                    return;
                }

                byte[] ipBytes = new byte[4];
                dataBuff.readBytes(ipBytes);
                // 必须截取掉 第一位的 '/' 字符
                host = Inet4Address.getByAddress(ipBytes).toString().substring(1);
                port = dataBuff.readShort();
            } else if (addressType == SocksAddressType.DOMAIN.byteValue()) {
                addressTypeString = SocksAddressType.DOMAIN.toString();

                // 读索引 不会改变
                int hostLength = dataBuff.getUnsignedByte(1);
                // 手动 改变 读索引
                dataBuff.readerIndex(2);
                if (dataBuff.readableBytes() < hostLength + 4) {
                    return;
                }

                byte[] hostBytes = new byte[hostLength];
                dataBuff.readBytes(hostBytes);
                host = new String(hostBytes);
                port = dataBuff.readShort();
            } else if (addressType == SocksAddressType.IPv6.byteValue()) {
                addressTypeString = SocksAddressType.IPv6.toString();

                if (dataBuff.readableBytes() < 19) {
                    return;
                }

                byte[] hostBytes = new byte[16];
                dataBuff.readBytes(hostBytes);
                // 必须截取掉 第一位的 '/' 字符
                host = Inet6Address.getByAddress(hostBytes).toString().substring(1);
                port = dataBuff.readShort();
            } else {
                addBlacklist(ctx);
                if (checkHeartbeatPacket(addressType)) {
                    logger.info("HeartbeatPacket: {} ,HeartbeatPacket :{} ,HeartbeatPacket Hex:{}", dataBuff.toString(Charset.forName("utf-8"))
                            , dataBuff.readableBytes(), ByteBufUtil.hexDump(dataBuff));
                    ReferenceCountUtil.safeRelease(dataBuff);
                    ctx.pipeline().addLast("httpResponseEncoder", new HttpResponseEncoder());
                    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.EMPTY_BUFFER);
                    ctx.channel().writeAndFlush(response).sync();
                    ctx.pipeline().remove("httpResponseEncoder");
                    return;
                }

                // 测试中
//                if (isTrack) {
//                    logger.warn(userName + "'s unknown address type: " + addressType + " client address: " + ctx.channel().remoteAddress());
//                    logger.info("error data: {} ,data length :{} ,hex string :{}", dataBuff.toString(Charset.forName("utf-8"))
//                            , dataBuff.readableBytes(), ByteBufUtil.hexDump(dataBuff));
//                } else {
//                    logger.warn("unknown address type: " + addressType + " client address: " + ctx.channel().remoteAddress());
//                    logger.info("error data: {} ,data length :{} ,hex string :{}", dataBuff.toString(Charset.forName("utf-8"))
//                            , dataBuff.readableBytes(), ByteBufUtil.hexDump(dataBuff));
//                }
//
//                ctx.channel().close();
//                ctx.close();

                GroovyUtils.invokeMethod(codeText, "unknownAddressTypeHandler", ctx, buff, addressType, userName, isTrack);
                GroovyUtils.clearGroovyClassLoaderCache();
                return;
            }

            GroovyUtils.invokeMethod(codeText, "loggerHandler", ctx, buff, addressTypeString, userName, host, port, isTrack);
            GroovyUtils.clearGroovyClassLoaderCache();

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

    private void addBlacklist(ChannelHandlerContext ctx) throws ConfigurationException {
        Configuration config = blacklist.getConfiguration();
        String ip = ctx.channel().remoteAddress().toString().substring(1, ctx.channel().remoteAddress().toString().indexOf(":"));
        int countBlacklist = config.getInt(ip, 0) + 1;
        config.setProperty(ip, countBlacklist);
    }

}