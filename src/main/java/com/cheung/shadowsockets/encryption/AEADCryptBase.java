package com.cheung.shadowsockets.encryption;

import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.bouncycastle.crypto.prng.RandomGenerator;
import org.bouncycastle.crypto.tls.*;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * Created by cheungrp on 18/9/8.
 */
public class AEADCryptBase {

    protected int keySize = 32;
    protected int saltSize = 32;
    protected int nonceSize = 12;
    protected int tagSize = 16;

    protected boolean isServer = true;

    public boolean isServer() {
        return isServer;
    }


    public AEADCryptBase() throws IOException {

        Chacha20Poly1305 chacha20Poly1305 = new Chacha20Poly1305(new TlsServerContext() {
            @Override
            public RandomGenerator getNonceRandomGenerator() {
                return new DigestRandomGenerator(new MD5Digest());
            }

            @Override
            public SecureRandom getSecureRandom() {
                return null;
            }

            @Override
            public SecurityParameters getSecurityParameters() {
                return new SecurityParameters();
            }

            @Override
            public boolean isServer() {
                return true;
            }

            @Override
            public ProtocolVersion getClientVersion() {
                return ProtocolVersion.DTLSv12;
            }

            @Override
            public ProtocolVersion getServerVersion() {
                return ProtocolVersion.DTLSv12;
            }

            @Override
            public TlsSession getResumableSession() {
                return null;
            }

            @Override
            public Object getUserObject() {
                return null;
            }

            @Override
            public void setUserObject(Object userObject) {

            }

            @Override
            public byte[] exportKeyingMaterial(String asciiLabel, byte[] context_value, int length) {
                return new byte[0];
            }
        });
    }
}
