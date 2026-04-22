package com.example.gitlinked.utils;

import android.util.Base64;
import android.util.Log;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-128 encryption utility for end-to-end encrypted messaging.
 * Encrypts messages before storing in SQLite and decrypts when reading.
 */
public class EncryptionUtil {

    private static final String TAG = "EncryptionUtil";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * Encrypt a plain text message using AES-128.
     *
     * @param plainText The message to encrypt
     * @return Base64-encoded encrypted string, or original text if encryption fails
     */
    public static String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    Constants.AES_KEY.getBytes("UTF-8"), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return plainText; // Fallback to plain text
        }
    }

    /**
     * Decrypt a Base64-encoded AES-encrypted string.
     *
     * @param encryptedText Base64-encoded encrypted message
     * @return Decrypted plain text, or original text if decryption fails
     */
    public static String decrypt(String encryptedText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    Constants.AES_KEY.getBytes("UTF-8"), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.decode(encryptedText, Base64.NO_WRAP);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            return encryptedText; // Fallback to encrypted text
        }
    }

    /**
     * Check if a string appears to be encrypted (Base64 encoded).
     */
    public static boolean isEncrypted(String text) {
        try {
            Base64.decode(text, Base64.NO_WRAP);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
