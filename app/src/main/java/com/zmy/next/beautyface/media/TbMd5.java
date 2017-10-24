package com.zmy.next.beautyface.media;

import android.content.pm.PackageInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.MessageDigest;

public class TbMd5 {
	
	private static final char[] HEX_DIGITS = { 48, 49, 50, 51, 52, 53, 54, 55,
		56, 57, 97, 98, 99, 100, 101, 102 };
	
	/**
	 * 根据packageInfo值计算signmd5值：
	 * 
	 * @return
	 */
	public static String creatSignInt(PackageInfo packageInfo)
			throws NumberFormatException {
		String md5 = getSignMd5(packageInfo);
		// // TODO TEST
		// md5 = "673004cf2f6efdec2385c8116c1e8c14";
		if (md5 == null || md5.length() < 32) {
			return "-1";
		}

		String sign = md5.substring(8, 8 + 16);
		long id1 = 0;
		long id2 = 0;
		String s = "";

		for (int i = 0; i < 8; i++) {
			id2 *= 16;
			s = sign.substring(i, i + 1);
			id2 += Integer.parseInt(s, 16);
		}

		for (int i = 8; i < sign.length(); i++) {
			id1 *= 16;
			s = sign.substring(i, i + 1);
			id1 += Integer.parseInt(s, 16);
		}

		long id = (id1 + id2) & 0xFFFFFFFFL;
		return String.valueOf(id);
	}
	/**
	 * 根据packageInfo计算签名的MD5值
	 * 
	 * @param packageInfo
	 * @return
	 */
	private static String getSignMd5(PackageInfo packageInfo) {
		if (packageInfo == null) {
			return null;
		}
		
		if (packageInfo.signatures == null) {
			return null;
		}
		
		if (packageInfo.signatures.length == 0) {
			return null;
		}
		
		if (packageInfo.signatures[0] == null) {
			return null;
		}
		
		try {
			return Md5.toMd5(packageInfo.signatures[0].toCharsString().getBytes());
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 得到第三方apk文件的md5
	 * 
	 * @return
	 */
	public static String getAPKHexMD5(byte[] paramArrayOfByte) {
		try {
			MessageDigest localMessageDigest = MessageDigest.getInstance("MD5");
			localMessageDigest.update(paramArrayOfByte);
			byte[] arrayOfByte = localMessageDigest.digest();
			char[] arrayOfChar = new char[32];
			int i = 0;
			int j = 0;
			while (true)
			{
				if (i >= 16)
					return new String(arrayOfChar);
				byte k = arrayOfByte[i];
				int m = j + 1;
				arrayOfChar[j] = HEX_DIGITS[(0xF & k >>> 4)];
				j = m + 1;
				arrayOfChar[m] = HEX_DIGITS[(k & 0xF)];
				i++;
			}
		} catch (Exception localException) {
			localException.printStackTrace();
		}
		return null;
	}

	/**
	 * 得到apk文件的md5
	 * 
	 * @return
	 */
	public static String getAPKMd5(PackageInfo packageInfo) {
		if (packageInfo == null) {
			return null;
		}
		// 得到apk的路径
		String dir = packageInfo.applicationInfo.publicSourceDir;
		File file = new File(dir);
		if (file.exists()) {
			try {
				return Md5.toMd5(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				return null;
			}
		}
		return null;
	}
	/**
	 * 从url的到md5
	 * @param url
	 * @return
	 */
	static public String getNameMd5FromUrl(String url) {
		return Md5.toMd5(url);
	}
}
