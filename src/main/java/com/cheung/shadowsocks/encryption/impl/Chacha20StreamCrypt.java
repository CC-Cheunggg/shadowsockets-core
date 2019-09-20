package com.cheung.shadowsocks.encryption.impl;

import com.cheung.shadowsocks.encryption.ICrypt;
import com.cheung.shadowsocks.encryption.StreamCryptBase;
import com.google.common.collect.Maps;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.ChaCha7539Engine;
import org.bouncycastle.crypto.engines.ChaChaEngine;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.util.Map;

public class Chacha20StreamCrypt extends StreamCryptBase {

    private final static String CIPHER_CHACHA20 = "chacha20";
    private final static String CIPHER_CHACHA20_IETF = "chacha20-ietf";


    public static Map<String, Class<? extends ICrypt>> getCiphers() {
        Map<String, Class<? extends ICrypt>> ciphers = Maps.newConcurrentMap();
        ciphers.put(CIPHER_CHACHA20, Chacha20StreamCrypt.class);
        ciphers.put(CIPHER_CHACHA20_IETF, Chacha20StreamCrypt.class);
        return ciphers;
    }

    public Chacha20StreamCrypt(String name, String password) {
        super(name, password);
    }

    @Override
    protected StreamCipher getCipher(boolean isEncrypted) {
        if (_name.equals(CIPHER_CHACHA20)) {
            return new ChaChaEngine();
        } else if (_name.equals(CIPHER_CHACHA20_IETF)) {
            return new ChaCha7539Engine();
        }

        return null;
    }

    @Override
    protected SecretKey getKey() {
        return new SecretKeySpec(_ssKey.getEncoded(), "AES");
    }

    @Override
    protected void _encrypt(byte[] data, ByteArrayOutputStream stream) {
        int noBytesProcessed;
        byte[] buffer = new byte[data.length];

        noBytesProcessed = encCipher.processBytes(data, 0, data.length, buffer, 0);
        stream.write(buffer, 0, noBytesProcessed);
    }

    @Override
    protected void _decrypt(byte[] data, ByteArrayOutputStream stream) {
        int BytesProcessedNum;
        byte[] buffer = new byte[data.length];
        BytesProcessedNum = decCipher.processBytes(data, 0, data.length, buffer, 0);
        stream.write(buffer, 0, BytesProcessedNum);

    }

    @Override
    public int getKeyLength() {
        if (_name.equals(CIPHER_CHACHA20) || _name.equals(CIPHER_CHACHA20_IETF)) {
            return 32;
        }
        return 0;
    }

    @Override
    public int getIVLength() {
        if (_name.equals(CIPHER_CHACHA20)) {
            return 8;
        } else if (_name.equals(CIPHER_CHACHA20_IETF)) {
            return 12;
        }
        return 0;
    }
}
