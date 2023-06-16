package yoyo.server.login.tools;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {
	public static String enc(String toEnc) throws NoSuchAlgorithmException {
		/*MessageDigest mdEnc = MessageDigest.getInstance("MD5"); // Encryption algorithm
		mdEnc.update(toEnc.getBytes(), 0, toEnc.length());
		return new BigInteger(1, mdEnc.digest()).toString(16); // Encrypted string*/
		return toEnc;
	}
	
	public static String enc2(String input) throws NoSuchAlgorithmException{
		String result = input;
        if(input != null) {
            MessageDigest md = MessageDigest.getInstance("MD5"); //or "SHA-1"
            md.update(input.getBytes());
            BigInteger hash = new BigInteger(1, md.digest());
            result = hash.toString(16);
            if ((result.length() % 2) != 0) {
                result = "0" + result;
            }
        }
        return result;
	}
	
	public static void main(String[] args) throws NoSuchAlgorithmException{
		String pwd = "11111111";
		System.out.println("md5 = " + enc(pwd));
		System.out.println("enc2 == " + enc2(pwd));
		System.out.println(enc(pwd).equals(enc2(pwd)));
		System.out.println(enc(pwd).equals("1bbd886460827015e5d605ed44252251"));
	}
}
