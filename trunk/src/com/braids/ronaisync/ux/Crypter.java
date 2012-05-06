package com.braids.ronaisync.ux;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import com.google.gdata.util.common.util.Base64;
import com.google.gdata.util.common.util.Base64DecoderException;

/**
 * PagaVCS is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.<br>
 * <br>
 * PagaVCS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.<br>
 * <br>
 * You should have received a copy of the GNU General Public License along with
 * PagaVCS; If not, see http://www.gnu.org/licenses/.
 */
public class Crypter {

	private static final String ENCODING_MODE_1 = ">>>x5";
	private static final String P1 = "xcg432AdfgSDG35aljadJA";
	private static final String P2 = "cJASD7sgm";
	private static final String P3 = "346sgbnalja7sgm";

	private static final byte[] SALT = { (byte) 0xae, (byte) 0x34, (byte) 0x10,
			(byte) 0x18, (byte) 0xde, (byte) 0x73, (byte) 0x10, (byte) 0x12, };

	private static char[] password;

	private static char[] getPassword() {
		if (password == null) {
			password = ("zwru" + P1 + "peek" + P2 + "poke" + P3).toCharArray();
		}
		return password;
	}

	public static String encrypt(String property)
			throws GeneralSecurityException {
		SecretKeyFactory keyFactory = SecretKeyFactory
				.getInstance("PBEWithMD5AndDES");
		SecretKey key = keyFactory
				.generateSecret(new PBEKeySpec(getPassword()));
		Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
		pbeCipher
				.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
		return ENCODING_MODE_1
				+ base64Encode(pbeCipher.doFinal(property.getBytes()));
	}

	private static String base64Encode(byte[] bytes) {
		return Base64.encode(bytes);
	}

	public static String decrypt(String property)
			throws GeneralSecurityException, IOException,
			Base64DecoderException {
		if (property == null) {
			return property;
		}
		if (!property.startsWith(ENCODING_MODE_1)) {
			return property;
		}
		property = property.substring(ENCODING_MODE_1.length());
		SecretKeyFactory keyFactory = SecretKeyFactory
				.getInstance("PBEWithMD5AndDES");
		SecretKey key = keyFactory
				.generateSecret(new PBEKeySpec(getPassword()));
		Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
		pbeCipher
				.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(SALT, 20));
		return new String(pbeCipher.doFinal(base64Decode(property)));
	}

	private static byte[] base64Decode(String property) throws IOException,
			Base64DecoderException {
		return Base64.decode(property.getBytes());
	}

}
