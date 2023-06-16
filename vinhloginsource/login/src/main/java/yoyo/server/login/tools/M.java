package yoyo.server.login.tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class M {

	private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public static String md5(String text,String charSet) {
		MessageDigest msgDigest = null;

		try {
			msgDigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(
					"System doesn't support MD5 algorithm.");
		}

		try {
			msgDigest.update(text.getBytes(charSet)); // ע��Ľӿ��ǰ���utf-8������ʽ����
		} catch (UnsupportedEncodingException e) {

			throw new IllegalStateException(
					"System doesn't support your  EncodingException.");

		}

		byte[] bytes = msgDigest.digest();

		String md5Str = new String(encodeHex(bytes));

		return md5Str;
	}
	
	public static String sha256(String text,String charSet) {
		MessageDigest msgDigest = null;

		try {
			msgDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(
					"System doesn't support SHA-256 algorithm.");
		}

		try {
			msgDigest.update(text.getBytes(charSet)); // ע��Ľӿ��ǰ���utf-8������ʽ����
		} catch (UnsupportedEncodingException e) {

			throw new IllegalStateException(
					"System doesn't support your  EncodingException.");

		}

		byte[] bytes = msgDigest.digest();

//		String sha256Str = new String(encodeHex(bytes));

		return Bytes2HexString(bytes);
	}
	
	/** 
	  * 
	  * @param b byte[] 
	  * @return String 
	  */ 
	public static String Bytes2HexString(byte[] b) { 
	   String ret = ""; 
	   for (int i = 0; i < b.length; i++) { 
	     String hex = Integer.toHexString(b[i] & 0xFF); 
	     if (hex.length() == 1) { 
	       hex = '0' + hex; 
	     } 
	     ret += hex.toUpperCase(); 
	   } 
	   return ret; 
	} 

	
    public static String bytes2Hex(byte[] bts) {
        String des = "";
        String tmp = null;
        for (int i = 0; i < bts.length; i++) {
            tmp = (Integer.toHexString(bts[i] & 0xFF));
            if (tmp.length() == 1) {
                des += "0";
            }
            des += tmp;
        }
        return des;
    }

	public static char[] encodeHex(byte[] data) {

		int l = data.length;

		char[] out = new char[l << 1];

		// two characters form the hex value.
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
			out[j++] = DIGITS[0x0F & data[i]];
		}

		return out;
	}
	
	public static InputStream httpRequest(String urlvalue, String des) {
		try {
			URL url = new URL(urlvalue);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			return urlConnection.getInputStream();
		} catch (Exception e) {
			System.out.println(des + "通信异常");
			e.printStackTrace();
		}
		return null; 
	}
	
	/**
	 * 对URL进行编码，编码失败，则返回原来的URL
	 * @param url
	 * @param enc
	 * @return
	 */
	public static String getEncodeURL(String url, String enc, Class subClass) {
		try {
			return java.net.URLEncoder.encode(url, enc);
		} catch (UnsupportedEncodingException e) {
			System.out.println(subClass.getName()
					+ "中getEncodeURL(url, enc):" + enc + "为不支持的编码方式。");
			e.printStackTrace();
		}
		return url;
	}

}
