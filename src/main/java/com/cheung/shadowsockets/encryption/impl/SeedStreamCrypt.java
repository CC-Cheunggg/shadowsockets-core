package com.cheung.shadowsockets.encryption.impl;

import com.cheung.shadowsockets.encryption.ICrypt;
import com.cheung.shadowsockets.encryption.StreamCryptBase;
import com.google.common.collect.Maps;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.engines.SEEDEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.util.Map;

/**
 * Seed cipher implementation
 */
public class SeedStreamCrypt extends StreamCryptBase {

    public final static String CIPHER_SEED_CFB = "seed-cfb";

    public static Map<String, Class<? extends ICrypt>> getCiphers() {
        Map<String, Class<? extends ICrypt>> ciphers = Maps.newConcurrentMap();
        ciphers.put(CIPHER_SEED_CFB, SeedStreamCrypt.class);

        return ciphers;
    }

    public SeedStreamCrypt(String name, String password) {
        super(name, password);
    }

    @Override
    public int getKeyLength() {
        return 16;
    }

    @Override
    protected StreamBlockCipher getCipher(boolean isEncrypted) throws InvalidAlgorithmParameterException {
        SEEDEngine engine = new SEEDEngine();
        StreamBlockCipher cipher;

        if (_name.equals(CIPHER_SEED_CFB)) {
            cipher = new CFBBlockCipher(engine, getIVLength() * 8);
        } else {
            throw new InvalidAlgorithmParameterException(_name);
        }

        return cipher;
    }

    @Override
    public int getIVLength() {
        return 16;
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
        int noBytesProcessed;
        byte[] buffer = new byte[data.length];

        noBytesProcessed = decCipher.processBytes(data, 0, data.length, buffer, 0);
        stream.write(buffer, 0, noBytesProcessed);
    }
}
