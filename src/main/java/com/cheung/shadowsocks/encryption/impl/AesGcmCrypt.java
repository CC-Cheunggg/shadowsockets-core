package com.cheung.shadowsocks.encryption.impl;

import com.cheung.shadowsocks.encryption.AEADCryptBase;
import com.cheung.shadowsocks.encryption.CryptName;
import com.cheung.shadowsocks.encryption.ICrypt;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;

@Slf4j
@CryptName({"aes-128-gcm", "aes-256-gcm"})
public class AesGcmCrypt extends AEADCryptBase {

    public final static String CIPHER_AEAD_128_GCM = "aes-128-gcm";
    public final static String CIPHER_AEAD_256_GCM = "aes-256-gcm";

    public AesGcmCrypt(String name, String password) {
        super(name, password);
    }

    //	Nonce Size
    @Override
    public int getKeyLength() {
        switch (_name) {
            case CIPHER_AEAD_128_GCM:
                return 16;
            case CIPHER_AEAD_256_GCM:
                return 32;
        }
        return 0;
    }

    @Override
    protected AEADCipher getCipher(boolean isEncrypted)
            throws GeneralSecurityException {
        switch (_name) {
            case CIPHER_AEAD_128_GCM:
            case CIPHER_AEAD_256_GCM:
                return new GCMBlockCipher(new AESEngine());
            default:
                throw new InvalidAlgorithmParameterException(_name);
        }
    }

    @Override
    public int getSaltLength() {
        switch (_name) {
            case CIPHER_AEAD_128_GCM:
                return 16;
            case CIPHER_AEAD_256_GCM:
                return 32;
        }
        return 0;
    }

    /**
     * TCP:[encrypted payload length][length tag][encrypted payload][payload tag]
     * UDP:[salt][encrypted payload][tag]
     * //TODO need return multi chunks
     *
     * @param data
     * @param stream
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @Override
    protected void _tcpEncrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException, InvalidCipherTextException {
//        byte[] buffer = new byte[data.length];
//        int noBytesProcessed = encCipher.processBytes(data, 0, data.length, buffer, 0);
//        stream.write(buffer, 0, noBytesProcessed);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            int nr = Math.min(buffer.remaining(), PAYLOAD_SIZE_MASK);
            ByteBuffer.wrap(encBuffer).putShort((short) nr);
            encCipher.get().init(true, getCipherParameters(true));
            encCipher.get().doFinal(
                    encBuffer,
                    encCipher.get().processBytes(encBuffer, 0, 2, encBuffer, 0)
            );
            stream.write(encBuffer, 0, 2 + getTagLength());
            increment(this.encNonce);

            buffer.get(encBuffer, 2 + getTagLength(), nr);

            encCipher.get().init(true, getCipherParameters(true));
            encCipher.get().doFinal(
                    encBuffer,
                    2 + getTagLength() + encCipher.get().processBytes(encBuffer, 2 + getTagLength(), nr, encBuffer, 2 + getTagLength())
            );
            increment(this.encNonce);

            stream.write(encBuffer, 2 + getTagLength(), nr + getTagLength());
        }
    }

    /**
     * @param data
     * @param stream
     * @throws InvalidCipherTextException
     */
    @Override
    protected void _tcpDecrypt(byte[] data, ByteArrayOutputStream stream) throws InvalidCipherTextException {
//        byte[] buffer = new byte[data.length];
//        int noBytesProcessed = decCipher.processBytes(data, 0, data.length, buffer,
//                0);
//        logger.debug("remaining _tcpDecrypt");
//        stream.write(buffer, 0, noBytesProcessed);
//        logger.debug("ciphertext len:{}", data.length);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            log.debug("id:{} remaining {} payloadLenRead:{} payloadRead:{}", hashCode(), buffer.hasRemaining(), payloadLenRead, payloadRead);
            if (payloadRead == 0) {
//                [encrypted payload length][length tag]
                int wantLen = 2 + getTagLength() - payloadLenRead;
                int remaining = buffer.remaining();
                if (wantLen <= remaining) {
                    buffer.get(decBuffer, payloadLenRead, wantLen);
                } else {
                    buffer.get(decBuffer, payloadLenRead, remaining);
                    payloadLenRead += remaining;
                    return;
                }
                decCipher.get().init(false, getCipherParameters(false));
                decCipher.get().doFinal(
                        decBuffer,
                        decCipher.get().processBytes(decBuffer, 0, 2 + getTagLength(), decBuffer, 0)
                );
                increment(decNonce);
            }


//            [encrypted payload length][length tag]
            int size = ByteBuffer.wrap(decBuffer, 0, 2).getShort();
            log.debug("payload length:{},remaining:{},payloadRead:{}", size, buffer.remaining(), payloadRead);
            if (size == 0) {
                //TODO exists?
                return;
            } else {
                int wantLen = getTagLength() + size - payloadRead;
                int remaining = buffer.remaining();
                if (wantLen <= remaining) {
                    buffer.get(decBuffer, 2 + getTagLength() + payloadRead, wantLen);
                } else {
                    buffer.get(decBuffer, 2 + getTagLength() + payloadRead, remaining);
                    payloadRead += remaining;
                    return;
                }
            }

            decCipher.get().init(false, getCipherParameters(false));
            decCipher.get().doFinal(
                    decBuffer,
                    (2 + getTagLength()) + decCipher.get().processBytes(decBuffer, 2 + getTagLength(), size + getTagLength(), decBuffer, 2 + getTagLength())
            );
            increment(decNonce);

            payloadLenRead = 0;
            payloadRead = 0;

            stream.write(decBuffer, 2 + getTagLength(), size);
        }
    }

    @Override
    protected void _udpEncrypt(byte[] data, ByteArrayOutputStream stream) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int remaining = buffer.remaining();
        buffer.get(encBuffer, 0, remaining);
        encCipher.get().init(true, getCipherParameters(true));
        encCipher.get().doFinal(
                encBuffer,
                encCipher.get().processBytes(encBuffer, 0, remaining, encBuffer, 0)
        );
        stream.write(encBuffer, 0, remaining + getTagLength());
    }

    @Override
    protected void _udpDecrypt(byte[] data, ByteArrayOutputStream stream) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int remaining = buffer.remaining();
        buffer.get(decBuffer, 0, remaining);
        decCipher.get().init(false, getCipherParameters(false));
        decCipher.get().doFinal(
                decBuffer,
                decCipher.get().processBytes(decBuffer, 0, remaining, decBuffer, 0)
        );
        stream.write(decBuffer, 0, remaining - getTagLength());
    }
}
