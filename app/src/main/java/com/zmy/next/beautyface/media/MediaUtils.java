package com.zmy.next.beautyface.media;

import android.annotation.TargetApi;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

public class MediaUtils {

    public static final int DEFAULT_AUDIO_SAMPLE_RATE = 48000;
    public static final int DEFAULT_AUDIO_CHANNEL_COUNT = 1;
    public static final int DEFAULT_AUDIO_BIT_WIDTH = 16;

    public static boolean isBigEnding = false;

    public static class AudioFormatData {

        public static final int NO_REASON = 0;
        public static final int REASON_SAMPLE_RATE = 1;
        public static final int REASON_CHANNEL_COUNT = 3;
        public static final int REASON_BIT_WIDTH = 5;

        public int sampleRate = DEFAULT_AUDIO_SAMPLE_RATE;
        public int channelCount = DEFAULT_AUDIO_CHANNEL_COUNT;
        public int bitWidth = DEFAULT_AUDIO_BIT_WIDTH;

        /**
         * 判断是否需要做采样、转换通道、转换位宽时都用的reason这个字段
         * 采用的是reason累加，这样根据reason的值可以看具体是需要做那些
         * 如：reason = 4，则是需要做sampleRate（1）+ channelCount（3）
         * value in {1, 3, 4(1+3), 5, 6(1+5), 8(3+5), 9(1+3+5)}
         */
        public int reason = NO_REASON;

        public boolean isResample() {
            return reason == 1 || reason == 4 || reason == 6 || reason == 9;
        }

        public boolean isConvertChannel() {
            return reason == 3 || reason == 4 || reason == 8 || reason == 9;
        }

        public boolean isConvertBit() {
            return reason == 5 || reason == 6 || reason == 8 || reason == 9;
        }
    }

    public static class VideoFormatData {

        public int frameRate = 20;
        public int iframeInterval = 1;
        public long videoBitRate = (long) (1.5 * 1024 * 1024);
        public int audioBitRate = 128000;
        public long duration;
        public int width;
        public int height;

        @Override
        public String toString() {
            return getClass().getName() + " frameRate:" + frameRate + " iframeInterval:"
                    + iframeInterval + " videoBitRate:"
                    + videoBitRate + " audioBitRate:" + audioBitRate
                    + " duration = "+ duration
                    + " width = "+ width
                    + " height = "+ height;
        }
    }

    /**
     * 初始化数组排序方式
     */
    public static void initBigEnding() {
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) {
            isBigEnding = true;
        } else {
            isBigEnding = false;
        }
    }

    /**
     * @param audioFormatDatas
     * @return
     */
    public static boolean isMatchAudioFormat(AudioFormatData... audioFormatDatas) {
        if (audioFormatDatas == null || audioFormatDatas.length < 2) {
            return false;
        }

        AudioFormatData defaultAudioFormatData = audioFormatDatas[0];
        if (defaultAudioFormatData == null) {
            return false;
        }

        boolean result = true;

        for (int i = 1; i < audioFormatDatas.length; i++) {
            if (defaultAudioFormatData.sampleRate != audioFormatDatas[i].sampleRate) {
                audioFormatDatas[i].reason += AudioFormatData.REASON_SAMPLE_RATE;
                result = false;
            }
            if (defaultAudioFormatData.channelCount != audioFormatDatas[i].channelCount) {
                audioFormatDatas[i].reason += AudioFormatData.REASON_CHANNEL_COUNT;
                result = false;
            }
            if (defaultAudioFormatData.bitWidth != audioFormatDatas[i].bitWidth) {
                audioFormatDatas[i].reason += AudioFormatData.REASON_BIT_WIDTH;
                result = false;
            }
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static AudioFormatData getAudioFormat(String audioFile) {
        MediaExtractor mex = new MediaExtractor();
        try {
            mex.setDataSource(audioFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        MediaFormat mf = null;
        for (int i = 0; i < mex.getTrackCount(); i++) {
            MediaFormat format = mex.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                mex.selectTrack(i);
                mf = format;
                break;
            }
        }

        if (mf == null) {
            mex.release();
            return null;
        }

        AudioFormatData audioFormatData = new AudioFormatData();
        audioFormatData.sampleRate =
                (mf.containsKey(MediaFormat.KEY_SAMPLE_RATE) ? mf.getInteger(MediaFormat.KEY_SAMPLE_RATE) : DEFAULT_AUDIO_SAMPLE_RATE);
        audioFormatData.channelCount =
                (mf.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : DEFAULT_AUDIO_CHANNEL_COUNT);
        audioFormatData.bitWidth =
                (mf.containsKey("bit-width") ? mf.getInteger("bit-width") : DEFAULT_AUDIO_BIT_WIDTH);

        mex.release();

        return audioFormatData;
    }

    /**
     * 获取视频formatdata
     *
     * @param videoFile
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static VideoFormatData getVideoFormat(String videoFile) {
        MediaExtractor mex = new MediaExtractor();
        try {
            mex.setDataSource(videoFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        VideoFormatData videoFormatData = new VideoFormatData();

        for (int i = 0; i < mex.getTrackCount(); i++) {
            MediaFormat format = mex.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                videoFormatData.frameRate =
                        (format.containsKey(MediaFormat.KEY_FRAME_RATE) ? format.getInteger(MediaFormat.KEY_FRAME_RATE) : 20);
                videoFormatData.iframeInterval =
                        (format.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL) ? format.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL) : 1);
                videoFormatData.videoBitRate =
                        format.containsKey(MediaFormat.KEY_BIT_RATE) ? format.getLong(MediaFormat.KEY_BIT_RATE) : (long) (1.5 * 1024 * 1024);
                videoFormatData.duration = (format.containsKey(MediaFormat.KEY_DURATION) ? format.getLong(MediaFormat.KEY_DURATION) : 0);
                videoFormatData.width = (format.containsKey(MediaFormat.KEY_WIDTH) ? format.getInteger(MediaFormat.KEY_WIDTH) : 0);
                videoFormatData.height = (format.containsKey(MediaFormat.KEY_HEIGHT) ? format.getInteger(MediaFormat.KEY_HEIGHT) : 0);
            }

            if (mime.startsWith("audio/")) {
                videoFormatData.audioBitRate =
                        (format.containsKey(MediaFormat.KEY_BIT_RATE) ? format.getInteger(MediaFormat.KEY_BIT_RATE) : 128000);
            }
        }
        mex.release();

        return videoFormatData;
    }

    /**
     * 音频重采样
     */
    public static boolean resampling(String srcPath, String destPath, int sampleRate, int resampleRate) {
        Log.e("zmy", "resampling sampleRate = " + sampleRate + " resampleRate = " + resampleRate);
        if (resampleRate == sampleRate) {
            return false;
        }

        File beforeSampleChangedFile = new File(srcPath);
        File sampleChangedFile = new File(destPath);
        try {
            FileInputStream fis = new FileInputStream(beforeSampleChangedFile);
            FileOutputStream fos = new FileOutputStream(sampleChangedFile);
            //同样低采样率转高采样率也是可以的，改下面参数就行。
            new SSRC(fis, fos, sampleRate, resampleRate, 2, 2, 1, Integer.MAX_VALUE, 0, 0, true);

            fis.close();
            fos.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 转换通道数
     * 需确认先initBigEnding
     *
     * @param sourceChannelCount
     * @param outputChannelCount
     * @param bitWidth
     * @param sourceByteArray
     * @return
     */
    public static byte[] convertChannelCount(int sourceChannelCount, int outputChannelCount, int bitWidth,
                                             byte[] sourceByteArray) {
        Log.e("zmy", "convertChannelCount sourceChannelCount = " + sourceChannelCount + " outputChannelCount = " + outputChannelCount);
        if (sourceChannelCount == outputChannelCount) {
            return sourceByteArray;
        }

        switch (bitWidth) {
            case 1:
            case 2:
                break;
            default:
                return sourceByteArray;
        }

        int sourceByteArrayLength = sourceByteArray.length;

        byte[] byteArray;

        switch (sourceChannelCount) {
            case 1:
                switch (outputChannelCount) {
                    case 2:
                        byteArray = new byte[sourceByteArrayLength * 2];

                        byte firstByte;
                        byte secondByte;

                        switch (bitWidth) {
                            case 1:
                                for (int index = 0; index < sourceByteArrayLength; index += 1) {
                                    firstByte = sourceByteArray[index];

                                    byteArray[2 * index] = firstByte;
                                    byteArray[2 * index + 1] = firstByte;
                                }
                                break;
                            case 2:
                                for (int index = 0; index < sourceByteArrayLength; index += 2) {
                                    firstByte = sourceByteArray[index];
                                    secondByte = sourceByteArray[index + 1];

                                    byteArray[2 * index] = firstByte;
                                    byteArray[2 * index + 1] = secondByte;
                                    byteArray[2 * index + 2] = firstByte;
                                    byteArray[2 * index + 3] = secondByte;
                                }
                                break;
                        }

                        return byteArray;
                }
                break;
            case 2:
                switch (outputChannelCount) {
                    case 1:
                        int outputByteArrayLength = sourceByteArrayLength / 2;

                        byteArray = new byte[outputByteArrayLength];

                        switch (bitWidth) {
                            case 1:
                                for (int index = 0; index < outputByteArrayLength; index += 2) {
                                    short averageNumber =
                                            (short) ((short) sourceByteArray[2 * index] + (short)
                                                    sourceByteArray[2 *
                                                            index + 1]);
                                    byteArray[index] = (byte) (averageNumber >> 1);
                                }
                                break;
                            case 2:
                                for (int index = 0; index < outputByteArrayLength; index += 2) {
                                    byte resultByte[] = averageShortByteArray
                                            (sourceByteArray[2 * index],
                                                    sourceByteArray[2 * index + 1],
                                                    sourceByteArray[2 *
                                                            index + 2],
                                                    sourceByteArray[2 * index + 3], isBigEnding);

                                    byteArray[index] = resultByte[0];
                                    byteArray[index + 1] = resultByte[1];
                                }
                                break;
                        }

                        return byteArray;
                }
                break;
        }

        return sourceByteArray;
    }

    /**
     * 转换位宽
     * 需确认先initBigEnding
     *
     * @param sourceByteWidth
     * @param outputByteWidth
     * @param sourceByteArray
     * @return
     */
    public static byte[] convertByteWidth(int sourceByteWidth, int outputByteWidth, byte[]
            sourceByteArray) {
        Log.e("zmy", "convertChannelCount sourceByteWidth = " + sourceByteWidth + " outputByteWidth = " + outputByteWidth);
        if (sourceByteWidth == outputByteWidth) {
            return sourceByteArray;
        }

        int sourceByteArrayLength = sourceByteArray.length;

        byte[] byteArray;

        switch (sourceByteWidth) {
            case 1:
                switch (outputByteWidth) {
                    case 2:
                        byteArray = new byte[sourceByteArrayLength * 2];

                        byte resultByte[];

                        for (int index = 0; index < sourceByteArrayLength; index += 1) {
                            resultByte = getBytes((short) (sourceByteArray[index]
                                    * 256), isBigEnding);

                            byteArray[2 * index] = resultByte[0];
                            byteArray[2 * index + 1] = resultByte[1];
                        }

                        return byteArray;
                }
                break;
            case 2:
                switch (outputByteWidth) {
                    case 1:
                        int outputByteArrayLength = sourceByteArrayLength / 2;

                        byteArray = new byte[outputByteArrayLength];

                        for (int index = 0; index < outputByteArrayLength; index += 1) {
                            byteArray[index] = (byte) (getShort(sourceByteArray[2
                                            * index],
                                    sourceByteArray[2 * index + 1], isBigEnding) / 256);
                        }

                        return byteArray;
                }
                break;
        }

        return sourceByteArray;
    }

    public static byte[] averageShortByteArray(byte firstShortHighByte, byte firstShortLowByte,
                                               byte secondShortHighByte, byte secondShortLowByte,
                                               boolean bigEnding) {
        short firstShort = getShort(firstShortHighByte, firstShortLowByte, bigEnding);
        short secondShort = getShort(secondShortHighByte, secondShortLowByte, bigEnding);
        return getBytes((short) (firstShort / 2 + secondShort / 2), bigEnding);
    }

    public static short getShort(byte firstByte, byte secondByte, boolean bigEnding) {
        short shortValue = 0;

        if (bigEnding) {
            shortValue |= (firstByte & 0x00ff);
            shortValue <<= 8;
            shortValue |= (secondByte & 0x00ff);
        } else {
            shortValue |= (secondByte & 0x00ff);
            shortValue <<= 8;
            shortValue |= (firstByte & 0x00ff);
        }

        return shortValue;
    }

    public static byte[] getBytes(short shortValue, boolean bigEnding) {
        byte[] byteArray = new byte[2];

        if (bigEnding) {
            byteArray[1] = (byte) (shortValue & 0x00ff);
            shortValue >>= 8;
            byteArray[0] = (byte) (shortValue & 0x00ff);
        } else {
            byteArray[0] = (byte) (shortValue & 0x00ff);
            shortValue >>= 8;
            byteArray[1] = (byte) (shortValue & 0x00ff);
        }
        return byteArray;
    }
}
