/*
 * Copyright (c) 2015, Blake
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The name of the author may not be used to endorse or promote
 * products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.cheung.shadowsocks.encryption;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Crypt base class implementation
 */
public abstract class StreamCryptBase implements ICrypt {

    protected abstract StreamCipher getCipher(boolean isEncrypted) throws InvalidAlgorithmParameterException;

    protected abstract SecretKey getKey();

    protected abstract void _encrypt(byte[] data, ByteArrayOutputStream stream);

    protected abstract void _decrypt(byte[] data, ByteArrayOutputStream stream);


    protected CipherParameters getCipherParameters(byte[] iv, int ivLength) {
        byte[] _iv = new byte[ivLength];
        System.arraycopy(iv, 0, _iv, 0, ivLength);
        return new ParametersWithIV(new KeyParameter(_key.getEncoded()), _iv);
    }

    protected final String _name;
    protected final SecretKey _key;
    protected final ShadowSocksKey _ssKey;
    protected final int _ivLength;
    protected final int _keyLength;
    protected final SecureRandom secureRandom = new SecureRandom();
    protected boolean _encryptIVSet;
    protected boolean _decryptIVSet;

    protected boolean isForUdp;

    protected final ReentrantLock encLock = new ReentrantLock();
    protected final ReentrantLock decLock = new ReentrantLock();
    protected StreamCipher encCipher;
    protected StreamCipher decCipher;

    private static Logger logger = LoggerFactory.getLogger(StreamCryptBase.class.getName());

    public StreamCryptBase(String name, String password) {
        _name = name.toLowerCase();
        _ivLength = getIVLength();
        _keyLength = getKeyLength();
        _ssKey = new ShadowSocksKey(password, _keyLength);
        _key = getKey();
    }

    protected abstract int getKeyLength();

    protected abstract int getIVLength();

    protected void setIV(byte[] iv, boolean isEncrypt) {
        if (_ivLength == 0) {
            return;
        }

        CipherParameters cipherParameters = getCipherParameters(iv, _ivLength);

        try {
            if (isEncrypt) {
                encCipher = getCipher(Boolean.TRUE);
                encCipher.init(Boolean.TRUE, cipherParameters);
            } else {
                decCipher = getCipher(Boolean.FALSE);
                decCipher.init(Boolean.FALSE, cipherParameters);
            }
        } catch (InvalidAlgorithmParameterException e) {
            logger.error("非法算法参数异常", e);
        }
    }

    @Override
    public void encrypt(byte[] data, ByteArrayOutputStream stream) {
        try {
            encLock.lockInterruptibly();
            stream.reset();
            if (!_encryptIVSet) {
                _encryptIVSet = true;
                byte[] iv = new byte[_ivLength];
                secureRandom.nextBytes(iv);
                setIV(iv, true);

                stream.write(iv);
            }

            _encrypt(data, stream);
        } catch (Exception e) {
            logger.error("加密数据包出错 !", e);
        } finally {
            if (encLock.isHeldByCurrentThread()) {
                encLock.unlock();
            }
        }
    }

    @Override
    public void encrypt(byte[] data, int length, ByteArrayOutputStream stream) {
        byte[] d = new byte[length];
        System.arraycopy(data, 0, d, 0, length);
        encrypt(d, stream);
    }

    @Override
    public void decrypt(byte[] data, ByteArrayOutputStream stream) {
        byte[] temp;

        try {
            decLock.lockInterruptibly();
            stream.reset();
            if (!_decryptIVSet) {
                _decryptIVSet = true;
                setIV(data, false);
                temp = new byte[data.length - _ivLength];
                System.arraycopy(data, _ivLength, temp, 0, data.length - _ivLength);
            } else {
                temp = data;
            }

            _decrypt(temp, stream);
        } catch (Exception e) {
            logger.error("解密数据包出错 !", e);
        } finally {
            if (decLock.isHeldByCurrentThread()) {
                decLock.unlock();
            }
        }
    }

    @Override
    public void decrypt(byte[] data, int length, ByteArrayOutputStream stream) {
        byte[] d = new byte[length];
        System.arraycopy(data, 0, d, 0, length);
        decrypt(d, stream);
    }

    @Override
    public void isForUdp(boolean isForUdp) {
        this.isForUdp = isForUdp;
    }

    public static byte[] md5Digest(byte[] input) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return md5.digest(input);
        } catch (Exception e) {
            logger.error("digest error, so that return null", e);
            return new byte[0];
        }
    }
}
