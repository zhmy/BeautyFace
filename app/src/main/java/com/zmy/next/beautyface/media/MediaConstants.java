package com.zmy.next.beautyface.media;

/**
 * Created by zmy on 2017/9/1.
 */

public class MediaConstants {
    //视频配乐文件上级路径
    public static final String MUSIC_DIR = ".music/";
    //视频配乐文件完整路径
    public static final String MUSIC_FULL_DIR = FileHelper.EXTERNAL_STORAGE_DIRECTORY + "/" + FileHelper.DIR_ROOT + "/" + MUSIC_DIR;
    //视频封面临时文件
    public static final String TEMP_VIDEO_COVER_PATH = FileHelper.EXTERNAL_STORAGE_DIRECTORY + "/" + FileHelper.DIR_ROOT + "/videoCover.jpg";

    //视频编辑文件上级路径
    public static final String MUSIC_VIDEO_DIR = "tbVideo/";
    //视频编辑后生成的新视频完整临时路径
    public static final String TEMP_VIDEO_FULL_DIR = FileHelper.EXTERNAL_STORAGE_DIRECTORY + "/" + FileHelper.DIR_ROOT + "/";
    //视频编辑后生成的新视频完整路径
    public static final String VIDEO_FULL_DIR = FileHelper.EXTERNAL_STORAGE_DIRECTORY + "/" + FileHelper.DIR_ROOT + "/";

}
