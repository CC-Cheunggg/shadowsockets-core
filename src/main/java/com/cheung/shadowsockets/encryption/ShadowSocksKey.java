package com.cheung.shadowsockets.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.security.MessageDigest;

/**
 * Shadowsocks key generator
 */
public class ShadowSocksKey implements SecretKey {

	private static final long serialVersionUID = 1L;
	private static Logger logger = LoggerFactory.getLogger(ShadowSocksKey.class);
	private final static int KEY_LENGTH = 32;
	private byte[] _key;
	private int _length;

	public ShadowSocksKey(String password) {
		_length = KEY_LENGTH;
		_key = init(password);
	}

	public ShadowSocksKey(String password, int length) {
		_length = length;
		_key = init(password);
	}

	private byte[] init(String password) {
		MessageDigest md;
		byte[] keys = new byte[KEY_LENGTH];
		byte[] temp = null;
		byte[] hash = null;
		byte[] passwordBytes;

		try {
			md = MessageDigest.getInstance("MD5");
			passwordBytes = password.getBytes();
		} catch (Exception e) {
			logger.error("init error", e);
			return null;
		}

		for(int i = 0;i < keys.length;i += hash.length){
			if (i == 0) {
				hash = md.digest(passwordBytes);
				temp = new byte[passwordBytes.length + hash.length];
			} else {
				System.arraycopy(hash, 0, temp, 0, hash.length);
				System.arraycopy(passwordBytes, 0, temp, hash.length, passwordBytes.length);
				hash = md.digest(temp);
			}
			System.arraycopy(hash, 0, keys, i, hash.length);
		}

		if (_length != KEY_LENGTH) {
			byte[] keysl = new byte[_length];
			System.arraycopy(keys, 0, keysl, 0, _length);
			return keysl;
		}
		return keys;
	}

	@Override
	public String getAlgorithm() {
		return "shadowsocks";
	}

	@Override
	public String getFormat() {
		return "RAW";
	}

	@Override
	public byte[] getEncoded() {
		return _key;
	}
}
