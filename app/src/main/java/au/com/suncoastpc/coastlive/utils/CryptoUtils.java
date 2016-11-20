package au.com.suncoastpc.coastlive.utils;

import android.util.Base64;

import java.security.AlgorithmParameters;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
	public static String encrypt(String data, String key, String salt) {
		try {
			//SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			//KeySpec spec = new PBEKeySpec(key.toCharArray(), salt.getBytes(), 65536, 256);
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			KeySpec spec = new PBEKeySpec(key.toCharArray(), salt.getBytes(), 65536, 128);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secret);
			AlgorithmParameters params = cipher.getParameters();
			
			byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
			byte[] cipherBytes = cipher.doFinal(data.getBytes());
		
			String ivText = Base64.encodeToString(iv, Base64.DEFAULT);
			String cipherText = Base64.encodeToString(cipherBytes, Base64.DEFAULT);
			
			return ivText + "|" + cipherText;
		}
		catch (Exception e) {
			if (Environment.isDebugMode()) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public static String decrypt(String data, String key, String salt) {
		try {
			String[] parts = data.split("\\|");
			String ivText = parts[0];
			String cipherText = parts[1];
			
			byte[] iv = Base64.decode(ivText, Base64.DEFAULT);
			byte[] cipherBytes = Base64.decode(cipherText, Base64.DEFAULT);
			
			//SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			//KeySpec spec = new PBEKeySpec(key.toCharArray(), salt.getBytes(), 65536, 256);
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			KeySpec spec = new PBEKeySpec(key.toCharArray(), salt.getBytes(), 65536, 128);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
			
			return new String(cipher.doFinal(cipherBytes));
		}
		catch (Exception e) {
			if (Environment.isDebugMode()) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
}
