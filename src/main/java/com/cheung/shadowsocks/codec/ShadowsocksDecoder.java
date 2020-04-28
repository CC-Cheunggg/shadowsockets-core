package com.cheung.shadowsocks.codec;

import com.cheung.shadowsocks.common.ReadState;
import com.cheung.shadowsocks.config.Config;
import com.cheung.shadowsocks.config.ConfigXmlLoader;
import com.cheung.shadowsocks.encryption.CryptFactory;
import com.cheung.shadowsocks.encryption.CryptUtil;
import com.cheung.shadowsocks.encryption.ICrypt;
import com.cheung.shadowsocks.model.SSModel;
import com.cheung.shadowsocks.proxy.ProxyHandler;
import com.cheung.shadowsocks.utils.BootContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socks.SocksAddressType;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.List;

public class ShadowsocksDecoder extends ReplayingDecoder<ReadState> {

    private static Config config;

    static {
        config = ConfigXmlLoader.loader.load();
    }

    private ICrypt _crypt = CryptFactory.factory.get(config.getMethod(), config.getPassword());
    private SSModel model = new SSModel();
    private byte domainLength;
    private SocksAddressType hostType;


    private static final Logger logger = LoggerFactory.getLogger(ShadowsocksDecoder.class);

    public ShadowsocksDecoder() {
        super(ReadState.HOST_TYPE);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        model.setCrypt(_crypt);
        model.setChannelHandlerContext(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buff = (ByteBuf) msg;
        try {
            ByteBuf data = Unpooled.copiedBuffer(CryptUtil.decrypt(_crypt, buff));
            super.channelRead(ctx, data);
        } finally {
            CryptUtil.releaseByteBufAllRefCnt(buff);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("decode error", cause);
        ctx.close();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf data, List<Object> out) throws Exception {
        // 这个方法里面的 ByteBuf 会自动释放
        switch (state()) {
            case HOST_TYPE: {
                hostType = SocksAddressType.valueOf(data.readByte());
                if ((hostType != SocksAddressType.IPv4)
                        && (hostType != SocksAddressType.IPv6)
                        && (hostType != SocksAddressType.DOMAIN)) {
                    CryptUtil.releaseByteBufAllRefCnt(data);
                    ctx.close();
                    logger.error("UNKNOWN HOST TYPE.");
                    return;
                }
            }
            case HOST_LENGTH: {
                if (hostType == SocksAddressType.DOMAIN) {
                    domainLength = data.readByte();
                }
                checkpoint(ReadState.HOST_CONTENT);
            }
            case HOST_CONTENT: {
                if (hostType == SocksAddressType.IPv4) {
                    byte[] ipBytes = new byte[4];
                    data.readBytes(ipBytes);
                    model.setHost(Inet4Address.getByAddress(ipBytes).toString().substring(1));
                }
                if (hostType == SocksAddressType.IPv6) {
                    byte[] hostBytes = new byte[16];
                    data.readBytes(hostBytes);
                    model.setHost(Inet6Address.getByAddress(hostBytes).toString().substring(1));
                }
                if (hostType == SocksAddressType.DOMAIN) {
                    byte[] hostBytes = new byte[domainLength];
                    data.readBytes(hostBytes);
                    model.setHost(new String(hostBytes));
                }
                if (hostType == SocksAddressType.UNKNOWN) {
                    logger.error("UNKNOWN HOST TYPE.");
                    CryptUtil.releaseByteBufAllRefCnt(data);
                    ctx.close();
                    return;
                }
                checkpoint(ReadState.PORT);
            }
            case PORT: {
                model.setPort(data.readShort());
                checkpoint(ReadState.DATA);
            }
            case DATA: {
                int readableLength = data.writerIndex() - data.readerIndex();
                byte[] remain = new byte[readableLength];
                data.readBytes(remain, 0, readableLength);
                model.setCacheData(remain);
                model.setTsn(ctx.channel().id().asLongText());
                logger.info("from: {} ,TSN: {}", ctx.channel().remoteAddress().toString(), model.getTsn());
                ctx.pipeline()
                        .addLast(BootContext.getApplicationContext().getBean("eventExecutors", EventLoopGroup.class),
                                "proxyHandler", new ProxyHandler(model));
                checkpoint(ReadState.HOST_TYPE);
            }
        }
    }
}
