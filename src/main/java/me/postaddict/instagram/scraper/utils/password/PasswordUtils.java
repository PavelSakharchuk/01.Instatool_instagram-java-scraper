package me.postaddict.instagram.scraper.utils.password;

import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;


public class PasswordUtils {

    @SneakyThrows
    public static String getEncPassword(String password, WebEncryption webEncryption) {
        String time = String.valueOf(System.currentTimeMillis() / 1000);
        int key = webEncryption.getKeyId();
        String publicKey = webEncryption.getPublicKey();
        int version = webEncryption.getVersion();

        int overheadLength = 48;
        byte[] pkeyArray = new byte[publicKey.length() / 2];
        for (int i = 0; i < pkeyArray.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(publicKey.substring(index, index + 2), 16);
            pkeyArray[i] = (byte) j;
        }

        byte[] y = new byte[password.length() + 36 + 16 + overheadLength];

        int f = 0;
        y[f] = 1;
        y[f += 1] = (byte) key;
        f += 1;

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);

        // Generate Key
        SecretKey secretKey = keyGenerator.generateKey();
        byte[] IV = new byte[12];

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, IV);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
        cipher.updateAAD(time.getBytes());

        byte[] sealed = SealedBoxUtility.crypto_box_seal(secretKey.getEncoded(), pkeyArray);
        byte[] cipherText = cipher.doFinal(password.getBytes());
        y[f] = (byte) (255 & sealed.length);
        y[f + 1] = (byte) (sealed.length >> 8 & 255);
        f += 2;
        for (int j = f; j < f + sealed.length; j++) {
            y[j] = sealed[j - f];
        }
        f += 32;
        f += overheadLength;

        byte[] c = Arrays.copyOfRange(cipherText, cipherText.length - 16, cipherText.length);
        byte[] h = Arrays.copyOfRange(cipherText, 0, cipherText.length - 16);

        for (int j = f; j < f + c.length; j++) {
            y[j] = c[j - f];
        }
        f += 16;
        for (int j = f; j < f + h.length; j++) {
            y[j] = h[j - f];
        }
        String encPassword = Base64.getEncoder().encodeToString(y);
        return String.format("#PWD_INSTAGRAM_BROWSER:%s:%s:%s", version, time, encPassword);
    }
}
