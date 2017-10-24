package com.zmy.next.beautyface.media;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5处理类
 */
public class Md5 {
	public String str;
	private static final char HEX_DIGITS[] = { '0', '1', '2', '3', '4', '5',
		'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	
	public static String toMd5(byte[] paramArrayOfByte, boolean paramBoolean)
	{
		try
		{
			MessageDigest localMessageDigest = MessageDigest.getInstance("MD5");
			localMessageDigest.reset();
			localMessageDigest.update(paramArrayOfByte);
			return toHexString(localMessageDigest.digest(), "", paramBoolean);
		} catch (NoSuchAlgorithmException localNoSuchAlgorithmException)
		{
			throw new RuntimeException(localNoSuchAlgorithmException);
		}
	}

	public static String toHexString(byte[] paramArrayOfByte, String paramString, boolean paramBoolean)
	{
		StringBuilder localStringBuilder = new StringBuilder();
		for (int k : paramArrayOfByte)
		{
			String str = Integer.toHexString(0xFF & k);
			if (paramBoolean)
				str = str.toUpperCase();
			if (str.length() == 1)
				localStringBuilder.append("0");
			localStringBuilder.append(str).append(paramString);
		}
		return localStringBuilder.toString();
	}
	/**
	 * 将byte转换为md5
	 * @param source
	 * @return
	 */
	public static String toMd5(byte[] source) {
		String s = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(source);
			s = toHexString(md.digest()); // 换后的结果转换为字符串
		} catch (Exception e) {
			Log.e("zmy", e.getMessage());
		}
		return s;
	}
	

	/**
	 * 将byte转换为16进制
	 * @param b
	 * @return
	 */
	public static String toHexString(byte[] b) {
		if (b == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
			sb.append(HEX_DIGITS[b[i] & 0x0f]);
		}
		return sb.toString();
	}
	/**
	 * 将InputStream转换位md5
	 * @param in
	 * @return
	 */

	public static String toMd5(InputStream in) {
		String ret = null;
		if (in == null) {
			return null;
		}
		try {
			byte[] buffer = new byte[1024];
			int numRead = 0;
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			while ((numRead = in.read(buffer)) > 0) {
				md5.update(buffer, 0, numRead);
			}
			ret = toHexString(md5.digest());
		} catch (Exception ex) {
			Log.e("zmy", ex.toString());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Log.e("zmy", e.getMessage());
				}
			}
		}
		return ret;
	}
	
	/**
	 * MD5加密字符串
	 * 
	 * @param str
	 * @return
	 */
	public static String toMd5(String str) {
		if (str == null) {
			return null;
		}

		String ret = null;
		try {
			byte[] bMsg = str.getBytes("UTF-8");
			ret = toMd5(bMsg);
		} catch (Exception e) {
		}
		return ret;
	}
}
