package util;

import util.encrypt.AESUtil;
import util.encrypt.MD5Util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class PasswordTool {

    public static byte[] decryptPassword(String encryptedPassword, String verificationCode, String iv){
        byte[] decodedBase64 = Base64.getDecoder().decode(encryptedPassword.getBytes(StandardCharsets.UTF_8));
        byte[] decryptedMD5Password = AESUtil.decrypt(decodedBase64, verificationCode.getBytes(StandardCharsets.UTF_8), iv.getBytes(StandardCharsets.US_ASCII));
        return decryptedMD5Password;
    }

    public static String encryptWithRandomCode(byte[] md5Password, String randomCode){
        String encryptedFinalResult = MD5Util.toMD5(Arrays.toString(md5Password) + randomCode);
        return encryptedFinalResult;
    }

}
