package com.zmy.next.beautyface.media;

import android.os.Environment;

import java.io.File;

/**
 * 文件辅助类
 */
public class FileHelper {
	public static final File EXTERNAL_STORAGE_DIRECTORY = Environment
			.getExternalStorageDirectory();
	public static final String DIR_ROOT = "zmy";

	/**
	 * 删除文件或文件夹
	 *
	 * @param file
	 */
	public static void deleteFileOrDir(File file) {
		try {
			if (file.exists()) {
				if (file.isDirectory()) {
					File[] files = file.listFiles();
					for (int i = 0, len = files.length; i < len; i++) {
						if (files[i].isFile()) {
							files[i].delete();
						} else {
							deleteFileOrDir(files[i]);
						}
					}
				}
				file.delete();
			}
		} catch (Exception e) {
		}
	}
}
