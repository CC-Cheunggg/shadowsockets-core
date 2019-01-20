package com.cheung.shadowsockets.encryption.impl;

import com.cheung.shadowsockets.encryption.ICrypt;
import com.cheung.shadowsockets.encryption.StreamCryptBase;
import com.google.common.collect.Maps;
import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.engines.CamelliaEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.util.Map;

/**
 * Camellia cipher implementation
 */
public class CamelliaStreamCrypt extends StreamCryptBase {

    public final static String CIPHER_CAMELLIA_128_CFB = "camellia-128-cfb";
    public final static String CIPHER_CAMELLIA_192_CFB = "camellia-192-cfb";
    public final static String CIPHER_CAMELLIA_256_CFB = "camellia-256-cfb";

    public static Map<String, Class<? extends ICrypt>> getCiphers() {
        Map<String, Class<? extends ICrypt>> ciphers = Maps.newConcurrentMap();
        ciphers.put(CIPHER_CAMELLIA_128_CFB, CamelliaStreamCrypt.class);
        ciphers.put(CIPHER_CAMELLIA_192_CFB, CamelliaStreamCrypt.class);
        ciphers.put(CIPHER_CAMELLIA_256_CFB, CamelliaStreamCrypt.class);

        return ciphers;
    }

    public CamelliaStreamCrypt(String name, String password) {
        super(name, password);
    }

    @Override
    public int getKeyLength() {
        if(_name.equals(CIPHER_CAMELLIA_128_CFB)) {
            return 16;
        }
        else if (_name.equals(CIPHER_CAMELLIA_192_CFB)) {
            return 24;
        }
        else if (_name.equals(CIPHER_CAMELLIA_256_CFB)) {
            return 32;
        }

        return 0;
    }

    @Override
    protected StreamBlockCipher getCipher(boolean isEncrypted) throws InvalidAlgorithmParameterException {
        CamelliaEngine engine = new CamelliaEngine();
        StreamBlockCipher cipher;

        if (_name.equals(CIPHER_CAMELLIA_128_CFB)) {
            cipher = new CFBBlockCipher(engine, getIVLength() * 8);
        }
        else if (_name.equals(CIPHER_CAMELLIA_192_CFB)) {
            cipher = new CFBBlockCipher(engine, getIVLength() * 8);
        }
        else if (_name.equals(CIPHER_CAMELLIA_256_CFB)) {
            cipher = new CFBBlockCipher(engine, getIVLength() * 8);
        }
        else {
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
