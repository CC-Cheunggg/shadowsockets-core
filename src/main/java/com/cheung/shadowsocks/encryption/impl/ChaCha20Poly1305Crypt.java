package com.cheung.shadowsocks.encryption.impl;

import com.cheung.shadowsocks.encryption.AEADCryptBase;
import com.cheung.shadowsocks.encryption.ICrypt;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.modes.AEADCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ChaCha20Poly1305Crypt extends AEADCryptBase {

    public final static String AEAD_CHACHA20_POLY1305 = "chacha20-ietf-poly1305";

    public ChaCha20Poly1305Crypt(String name, String password) {
        super(name, password);
    }

    public static Map<String, Class<? extends ICrypt>> getCiphers() {
        Map<String, Class<? extends ICrypt>> ciphers = new HashMap<>();
        ciphers.put(AEAD_CHACHA20_POLY1305, ChaCha20Poly1305Crypt.class);
        return ciphers;
    }

    @Override
    protected AEADCipher getCipher(boolean isEncrypted) throws GeneralSecurityException {
        return new ChaCha20Poly1305();
    }

    @Override
    protected void _tcpEncrypt(byte[] data, ByteArrayOutputStream stream) throws GeneralSecurityException, IOException, InvalidCipherTextException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            int nr = Math.min(buffer.remaining(), PAYLOAD_SIZE_MASK);
            ByteBuffer.wrap(encBuffer).putShort((short) nr);
            encCipher.init(true, getCipherParameters(true));
            encCipher.doFinal(
                    encBuffer,
                    encCipher.processBytes(encBuffer, 0, 2, encBuffer, 0)
            );
            stream.write(encBuffer, 0, 2 + getTagLength());
            increment(this.encNonce);

            buffer.get(encBuffer, 2 + getTagLength(), nr);

            encCipher.init(true, getCipherParameters(true));
            encCipher.doFinal(
                    encBuffer,
                    2 + getTagLength() + encCipher.processBytes(encBuffer, 2 + getTagLength(), nr, encBuffer, 2 + getTagLength())
            );
            increment(this.encNonce);

            stream.write(encBuffer, 2 + getTagLength(), nr + getTagLength());
        }
    }

    @Override
    protected void _tcpDecrypt(byte[] data, ByteArrayOutputStream stream) throws InvalidCipherTextException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            log.debug("id:{} remaining {} payloadLenRead:{} payloadRead:{}", hashCode(), buffer.hasRemaining(), payloadLenRead, payloadRead);
            if (payloadRead == 0) {
                int wantLen = 2 + getTagLength() - payloadLenRead;
                int remaining = buffer.remaining();
                if (wantLen <= remaining) {
                    buffer.get(decBuffer, payloadLenRead, wantLen);
                } else {
                    buffer.get(decBuffer, payloadLenRead, remaining);
                    payloadLenRead += remaining;
                    return;
                }
                decCipher.init(false, getCipherParameters(false));
                decCipher.doFinal(
                        decBuffer,
                        decCipher.processBytes(decBuffer, 0, 2 + getTagLength(), decBuffer, 0)
                );
                increment(decNonce);
            }

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

            decCipher.init(false, getCipherParameters(false));
            decCipher.doFinal(
                    decBuffer,
                    (2 + getTagLength()) + decCipher.processBytes(decBuffer, 2 + getTagLength(), size + getTagLength(), decBuffer, 2 + getTagLength())
            );
            increment(decNonce);

            payloadLenRead = 0;
            payloadRead = 0;

            stream.write(decBuffer, 2 + getTagLength(), size);
        }
    }

    @Override
    protected void _udpEncrypt(byte[] data, ByteArrayOutputStream stream) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _udpDecrypt(byte[] data, ByteArrayOutputStream stream) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int getKeyLength() {
        return 32;
    }

    @Override
    protected int getSaltLength() {
        return 32;
    }
}
