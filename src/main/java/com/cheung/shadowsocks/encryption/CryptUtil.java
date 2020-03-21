package com.cheung.shadowsocks.encryption;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CryptUtil {

    private static final Logger logger = LoggerFactory.getLogger(CryptUtil.class);

    public static byte[] encrypt(ICrypt crypt, ByteBuf bytebuff) {
        byte[] data = null;
        ByteArrayOutputStream _remoteOutStream = null;
        try {
            _remoteOutStream = new ByteArrayOutputStream();
            if (!bytebuff.hasArray()) {
                int len = bytebuff.readableBytes();
                byte[] arr = new byte[len];
                bytebuff.getBytes(0, arr);
                crypt.encrypt(arr, arr.length, _remoteOutStream);
                data = _remoteOutStream.toByteArray();
            }
        } finally {
            if (_remoteOutStream != null) {
                try {
                    _remoteOutStream.close();
                } catch (IOException e) {
                    logger.error("close error", e);
                }
            }
        }
        return data;
    }

    public static byte[] decrypt(ICrypt crypt, ByteBuf bytebuff) {
        byte[] data = null;
        ByteArrayOutputStream _localOutStream = null;
        try {
            _localOutStream = new ByteArrayOutputStream();
            if (!bytebuff.hasArray()) {
                int len = bytebuff.readableBytes();
                byte[] arr = new byte[len];
                bytebuff.getBytes(0, arr);
                crypt.decrypt(arr, arr.length, _localOutStream);
                data = _localOutStream.toByteArray();
            }
        } finally {

            if (_localOutStream != null) {
                try {
                    _localOutStream.close();
                } catch (IOException e) {
                    logger.error("close error", e);
                }
            }
        }
        return data;
    }

    public static void releaseByteBufAllRefCnt(ByteBuf buf) {
        synchronized (CryptUtil.class) {
            if (buf == null) {
                return;
            }
            int ref = buf.refCnt();
            if (ref > 0) {
                buf.release(ref);
            }
        }
    }


}
