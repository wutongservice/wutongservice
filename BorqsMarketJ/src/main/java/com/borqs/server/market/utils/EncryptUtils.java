package com.borqs.server.market.utils;


import org.apache.commons.codec.binary.Base64;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;


public class EncryptUtils {

    public static byte[] desEncrypt(byte[] data, String encryptKey) {
        try {
            SecureRandom sr = new SecureRandom();
            DESKeySpec dks = new DESKeySpec(encryptKey.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(dks);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key, sr);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Encrypt error");
        }
    }

    public static byte[] desDecrypt(byte[] encryptedData, String encryptKey) {
        try {
            SecureRandom sr = new SecureRandom();
            DESKeySpec dks = new DESKeySpec(encryptKey.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(dks);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key, sr);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Decrypt error");
        }
    }

    public static String desEncryptBase64(String data, String encryptKey) {
        try {
            return Base64.encodeBase64URLSafeString(desEncrypt(data.getBytes("UTF-8"), encryptKey));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encode error");
        }
    }

    public static String desDecryptBase64(String encryptedBase64Data, String encryptKey) {
        try {
            byte[] bytes = desDecrypt(Base64.decodeBase64(encryptedBase64Data), encryptKey);
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Decode error");
        }
    }
}

