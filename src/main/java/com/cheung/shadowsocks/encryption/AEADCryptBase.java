package com.cheung.shadowsocks.encryption;

import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by cheungrp on 18/9/8.
 */
@Slf4j
public abstract class AEADCryptBase implements ICrypt {

    protected static int PAYLOAD_SIZE_MASK = 0x3FFF;

    private static byte[] info = "ss-subkey".getBytes();

    private static byte[] ZERO_NONCE = new byte[getNonceLength()];

    protected final String _name;
    protected final ShadowSocksKey _ssKey;
    protected final int _keyLength;
    private boolean isForUdp = false;
    protected final AtomicBoolean _encryptSaltSet = new AtomicBoolean(Boolean.FALSE);
    protected final AtomicBoolean _decryptSaltSet = new AtomicBoolean(Boolean.FALSE);
    protected final ReentrantLock encLock = new ReentrantLock();
    protected final ReentrantLock decLock = new ReentrantLock();
    protected final AtomicReference<AEADCipher> encCipher = new AtomicReference<>();
    protected final AtomicReference<AEADCipher> decCipher = new AtomicReference<>();
    private final AtomicReference<byte[]> encSubkey = new AtomicReference<>();
    private final AtomicReference<byte[]> decSubkey = new AtomicReference<>();
    protected byte[] encNonce;
    protected byte[] decNonce;

    protected byte[] encBuffer = new byte[2 + getTagLength() + PAYLOAD_SIZE_MASK + getTagLength()];
    protected byte[] decBuffer = new byte[PAYLOAD_SIZE_MASK + getTagLength()];

    /**
     * last chunk payload len already read size
     */
    protected int payloadLenRead = 0;

    /**
     * last chunk payload already read size
     */
    protected int payloadRead = 0;

    public AEADCryptBase(String name, String password) {
        _name = name.toLowerCase();
        _keyLength = getKeyLength();
        _ssKey = new ShadowSocksKey(password, _keyLength);
        isForUdp(isForUdp);
    }

    @Override
    public void isForUdp(boolean isForUdp) {
        this.isForUdp = isForUdp;
        if (!isForUdp) {
            if (encNonce == null && decNonce == null) {
                encNonce = new byte[getNonceLength()];
                decNonce = new byte[getNonceLength()];
            }
            if (encNonce == null) {
                encNonce = new byte[getNonceLength()];
            }
            if (decNonce == null) {
                decNonce = new byte[getNonceLength()];
            }
        }
    }

    private byte[] genSubkey(byte[] salt) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA1Digest());
        hkdf.init(new HKDFParameters(_ssKey.getEncoded(), salt, info));
        byte[] okm = new byte[getKeyLength()];
        hkdf.generateBytes(okm, 0, getKeyLength());
        return okm;
    }

    protected static void increment(byte[] nonce) {
        for (int i = 0; i < nonce.length; i++) {
            ++nonce[i];
            if (nonce[i] != 0) {
                break;
            }
        }
    }


    protected CipherParameters getCipherParameters(boolean forEncryption) {
        byte[] nonce;
        if (!isForUdp) {
            nonce = forEncryption ? Arrays.copyOf(encNonce, getNonceLength()) : Arrays.copyOf(decNonce, getNonceLength());
        } else {
            nonce = ZERO_NONCE;
        }
        return new AEADParameters(
                new KeyParameter(forEncryption ? encSubkey.get() : decSubkey.get()),
                getTagLength() * 8,
                nonce
        );
    }

    @Override
    public void encrypt(byte[] data, ByteArrayOutputStream stream) {
        try {
            stream.reset();
            if (!_encryptSaltSet.get() || isForUdp) {
                byte[] salt = randomBytes(getSaltLength());
                stream.write(salt);
                encSubkey.compareAndSet(null, genSubkey(salt));
                encCipher.compareAndSet(null, getCipher(true));
                _encryptSaltSet.compareAndSet(Boolean.FALSE, Boolean.TRUE);
            }
            encLock.lockInterruptibly();
            if (!isForUdp) {
                _tcpEncrypt(data, stream);
            } else {
                _udpEncrypt(data, stream);
            }
        } catch (Exception e) {
            log.error("加密出错 :", e);
        } finally {
            if (encLock.isHeldByCurrentThread()) {
                encLock.unlock();
            }
        }
    }

    @Override
    public void encrypt(byte[] data, int length, ByteArrayOutputStream stream) {
        byte[] d = Arrays.copyOfRange(data, 0, length);
        encrypt(d, stream);
    }

    @Override
    public void decrypt(byte[] data, ByteArrayOutputStream stream) {
        byte[] temp;
        try {
            stream.reset();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            if (decCipher.get() == null || isForUdp) {
                _decryptSaltSet.compareAndSet(Boolean.FALSE, Boolean.TRUE);
                byte[] salt = new byte[getSaltLength()];
                buffer.get(salt);
                decSubkey.compareAndSet(null, genSubkey(salt));
                decCipher.compareAndSet(null, getCipher(false));
                temp = new byte[buffer.remaining()];
                buffer.get(temp);
            } else {
                temp = data;
            }
            decLock.lockInterruptibly();
            if (!isForUdp) {
                _tcpDecrypt(temp, stream);
            } else {
                _udpDecrypt(temp, stream);
            }
        } catch (Exception e) {
            log.error("解密出错 :", e);
        } finally {
            if (decLock.isHeldByCurrentThread()) {
                decLock.unlock();
            }
        }
    }

    @Override
    public void decrypt(byte[] data, int length, ByteArrayOutputStream stream) {
        byte[] d = Arrays.copyOfRange(data, 0, length);
        decrypt(d, stream);
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static int getNonceLength() {
        return 12;
    }

    protected static int getTagLength() {
        return 16;
    }

    protected abstract AEADCipher getCipher(boolean isEncrypted) throws GeneralSecurityException;

    protected abstract void _tcpEncrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException, InvalidCipherTextException;

    protected abstract void _tcpDecrypt(byte[] data, ByteArrayOutputStream stream) throws InvalidCipherTextException;

    protected abstract void _udpEncrypt(byte[] data, ByteArrayOutputStream stream) throws Exception;

    protected abstract void _udpDecrypt(byte[] data, ByteArrayOutputStream stream) throws Exception;

    protected abstract int getKeyLength();

    protected abstract int getSaltLength();


}
