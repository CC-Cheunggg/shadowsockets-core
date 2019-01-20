package com.cheung.shadowsockets.encryption;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

/**
 * crypt 加密
 * 
 * @author
 * 
 */
public interface ICrypt extends Serializable{
	
	void encrypt(byte[] data, ByteArrayOutputStream stream);

	void encrypt(byte[] data, int length, ByteArrayOutputStream stream);

	void decrypt(byte[] data, ByteArrayOutputStream stream);

	void decrypt(byte[] data, int length, ByteArrayOutputStream stream);

}
