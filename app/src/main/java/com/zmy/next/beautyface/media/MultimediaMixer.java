package com.zmy.next.beautyface.media;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 媒体混合
 * 提供多视频混合，视频与音频混合，音频混合
 * Created by zmy on 2017/8/29.
 */

public class MultimediaMixer {

    private volatile static MultimediaMixer mInstance;

    private MultimediaMixer() {
    }

    public static MultimediaMixer getInstance() {
        if (mInstance == null) {
            synchronized (MultimediaMixer.class) {
                if (mInstance == null) {
                    mInstance = new MultimediaMixer();
                }
            }
        }
        return mInstance;
    }

    /**
     * 合成视频
     *
     * @param videoList
     * @param outputPath
     */
    public boolean mixingVideoByVideo(List<String> videoList, String outputPath) {
        if (videoList == null || TextUtils.isEmpty(outputPath)) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        File file = new File(outputPath);
        file.getParentFile().mkdirs();
        if (file.exists()) {
            file.delete();
        }
        long finalMovieLength = 0;

        List<Track> videoTracks = new LinkedList<Track>();
        List<Track> audioTracks = new LinkedList<Track>();

        try {
            for (int i = 0; i < videoList.size(); i++) {
                long tempLength = buildVideoMovie(videoList.get(i), videoTracks, audioTracks);
                if (tempLength != -1) {
                    finalMovieLength += tempLength;
                }
            }
            writeSimple(outputPath, videoTracks, audioTracks);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        Log.e("zmy", "mixingVideoByVideo videoList length = " + videoList.size() + " cost = " + (System.currentTimeMillis() - startTime));
        return true;
    }

    /**
     * 写入文件
     *
     * @param outputPath
     * @param videoTracks
     * @param audioTracks
     * @throws IOException
     */
    private void writeSimple(String outputPath, List<Track> videoTracks, List<Track> audioTracks) throws IOException {

        Movie resultMovie = new Movie();

        if (audioTracks != null && audioTracks.size() > 0) {
            resultMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }
        if (videoTracks != null && videoTracks.size() > 0) {
            resultMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        }

        // write out put file
        Container out = new DefaultMp4Builder().build(resultMovie);
        FileChannel fc = new RandomAccessFile(String.format(outputPath), "rw").getChannel();
        out.writeContainer(fc);
        fc.close();
    }

    /**
     * 静音视频
     *
     * @param videoPath
     * @param outputPath
     */
    public boolean mixingMuteVideo(String videoPath, String outputPath) {
        if (TextUtils.isEmpty(videoPath) || TextUtils.isEmpty(outputPath)) {
            return false;
        }
        long startTime = System.currentTimeMillis();

        File file = new File(outputPath);
        file.getParentFile().mkdirs();
        if (file.exists()) {
            file.delete();
        }

        long finalMovieLength;//视频时长

        List<Track> videoTracks = new LinkedList<Track>();//视频 video track
        try {
            //视频解析
            finalMovieLength = buildVideoMovie(videoPath, videoTracks, null);
            if (finalMovieLength == -1) {
                return false;
            }

            Log.e("zmy", "mixingVideoByAudio videoTracks = " + videoTracks.size());
            writeSimple(outputPath, videoTracks, null);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Log.e("zmy", "mixingVideoByAudio cost = " + (System.currentTimeMillis() - startTime));
        }
        return false;
    }

    /**
     * 视频与音频合成，默认需要合成音频
     *
     * @param videoPath
     * @param musicPath
     * @param outputPath
     */
    public boolean mixingVideoByAudio(String videoPath, String musicPath, String outputPath) {
        return mixingVideoByAudio(videoPath, musicPath, outputPath, true);
    }

    /**
     * 使用与音频合成
     *
     * @param videoPath
     * @param musicPath
     * @param outputPath
     * @param mixAudio   是否需要合成音频
     */
    public boolean mixingVideoByAudio(String videoPath, String musicPath, String outputPath, boolean mixAudio) {
        if (TextUtils.isEmpty(videoPath) || TextUtils.isEmpty(musicPath) || TextUtils.isEmpty(outputPath)) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        //作为本次合成的上层目录，因为合成过程中会产生很多临时文件，统一放在这个文件夹下一起删除
        String tempFileDir = TbMd5.getNameMd5FromUrl(videoPath + musicPath + outputPath) + "/";
        String tempFileFullDir = MediaConstants.TEMP_VIDEO_FULL_DIR + tempFileDir;
        File tempFile = new File(tempFileFullDir);
        tempFile.mkdirs();

        File file = new File(outputPath);
        file.getParentFile().mkdirs();
        if (file.exists()) {
            file.delete();
        }

        long finalMovieLength;//视频时长
        long simpleMusicLength;//音频时长

        List<Track> videoTracks = new LinkedList<Track>();//视频 video track
        List<Track> audioTracks = new LinkedList<Track>();//视频 audio track

        List<Track> simpleMusicTracks = new LinkedList<Track>();//音频 audio track
        //音频 audio track，最终要使用的音频，因为音频时间和视频时间可能不一致，所以需要重新生成与视频时长相等的音频
        List<Track> musicTracks = new LinkedList<Track>();

        try {
            //视频解析
            finalMovieLength = buildVideoMovie(videoPath, videoTracks, audioTracks);
            if (finalMovieLength == -1) {
                return false;
            }

            //音频解析
            simpleMusicLength = buildAudioMovie(musicPath, simpleMusicTracks);
            if (simpleMusicLength == -1) {
                return false;
            }

            //制作与视频文件大小一样的音频文件
            buildAudioByVideoLength(finalMovieLength, simpleMusicLength, simpleMusicTracks, musicTracks);

            //混合音频 外面设置需要混合&&视频里含音频
            if (mixAudio && audioTracks.size() > 0) {
                int sdkVersion = Build.VERSION.SDK_INT;
                if (sdkVersion >= Build.VERSION_CODES.JELLY_BEAN) {//16
                    //save new music
                    String tempAudioPath = tempFileFullDir + "temp_" + System.currentTimeMillis();
                    writeSimple(tempAudioPath, null, musicTracks);

                    //1.存储拼接后与视频等长的音频 2.提取视频中的音频 3.音频混合拿到混合后的编码文件(按需是否重采样) 4.音频视频合成（MP4Parser）
                    String tempVideoAudioPath = tempFileFullDir + "temp_" + System.currentTimeMillis();
                    writeSimple(tempVideoAudioPath, null, audioTracks);

                    String finalMixingPath = tempFileFullDir + "temp_" + System.currentTimeMillis() + ".acc";
                    boolean mixingResult = mixingAudio(finalMixingPath, tempFileFullDir, tempAudioPath, tempVideoAudioPath);
                    if (mixingResult) {
                        AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(finalMixingPath));

                        musicTracks.clear();
                        musicTracks.add(aacTrack);
                    }
                    Log.e("zmy", "mixingVideoByAudio mixing cost = " + (System.currentTimeMillis() - startTime));
                }
//                if (sdkVersion >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                    //save new music
//                    String tempAudioPath = tempFileFullDir + "temp_" + System.currentTimeMillis();
//                    writeSimple(tempAudioPath, null, musicTracks);
//
//                    //Plan A: 1.存储拼接后与视频等长的音频 2.解码视频文件提取音频 3.混合音频 4.音频视频合成（使用MediaMuxer)
//                    VideoMuxer videoMuxer = VideoMuxer.createVideoMuxer(outputPath);
//                    videoMuxer.mixRawAudio(videoPath, tempAudioPath, tempFileFullDir, true);
//
//                    Log.e("zmy", "VideoMuxer cost = " + (System.currentTimeMillis() - startTime));
//                    return outputPath;
//                }
            }

            // ffmpeg -i input.mp4 -i bg2.m4a -filter_complex amix=inputs=2:duration=first:dropout_transition=2 -f mp4 remix.mp4

            Log.e("zmy", "mixingVideoByAudio audioTracks = " + audioTracks.size() + " musicTracks = " + musicTracks.size() + " videoTracks = " + videoTracks.size());

            writeSimple(outputPath, videoTracks, musicTracks);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            FileHelper.deleteFileOrDir(new File(tempFileFullDir));
            Log.e("zmy", "mixingVideoByAudio cost = " + (System.currentTimeMillis() - startTime));
        }
        return false;
    }

    /**
     * 合并音轨 音轨采样率以第一个音轨为准
     *
     * @param finalMixingPath
     * @param tempDir
     * @param audioPaths
     */
    public boolean mixingAudio(final String finalMixingPath, String tempDir, String... audioPaths) {
        //默认最少两条音轨
        if (audioPaths == null || audioPaths.length < 2) {
            return false;
        }
        final String mixingAudioPath = tempDir + "temp_" + System.currentTimeMillis();

        //这里默认只处理视频和音频两个音轨合成，以音频音轨数据为基准
        File[] rawAudioFiles = new File[audioPaths.length];
        try {
            MediaUtils.AudioFormatData defaultAudioFormatData = MediaUtils.getAudioFormat(audioPaths[0]);
            if (defaultAudioFormatData == null) {
                return false;
            }
            MediaUtils.AudioFormatData audioFormatData = new MediaUtils.AudioFormatData();
            boolean isMatch = true;
            for (int i = 0; i < audioPaths.length; i++) {
                if (i != 0) {
                    audioFormatData = MediaUtils.getAudioFormat(audioPaths[i]);
                    if (audioFormatData == null) {
                        return false;
                    }
                    isMatch = MediaUtils.isMatchAudioFormat(defaultAudioFormatData, audioFormatData);
                }
                AndroidAudioDecoder rawAudioDecoder = new AndroidAudioDecoder(audioPaths[i]);
                String tempPath = tempDir + "temp_" + i + "_" + System.currentTimeMillis();
                AudioDecoder.RawAudioInfo decodeResult = rawAudioDecoder.decodeToFile(tempPath, isMatch, defaultAudioFormatData, audioFormatData);
                if (decodeResult == null) {
                    continue;
                }
                if (!isMatch && i != 0 && audioFormatData.isResample()) {
                    String resampleAudioPath = tempDir + "resample_" + System.currentTimeMillis();
                    long startTime = System.currentTimeMillis();
                    boolean result = MediaUtils.resampling(tempPath, resampleAudioPath,
                            audioFormatData.sampleRate, defaultAudioFormatData.sampleRate);
                    Log.e("zmy", "resample cost = " + (System.currentTimeMillis() - startTime));
                    if (result) {
                        tempPath = resampleAudioPath;
                    }
                }
                rawAudioFiles[i] = new File(tempPath);
            }

            MultiAudioMixer audioMixer = MultiAudioMixer.createAudioMixer();
            audioMixer.setOnAudioMixListener(new MultiAudioMixer.OnAudioMixListener() {

                FileOutputStream fosRawMixAudio = new FileOutputStream(mixingAudioPath);

                @Override
                public void onMixing(byte[] mixBytes) throws IOException {
                    if (fosRawMixAudio != null) {
                        fosRawMixAudio.write(mixBytes);
                    }
                }

                @Override
                public void onMixError(int errorCode) {
                    try {
                        if (fosRawMixAudio != null) {
                            fosRawMixAudio.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMixComplete() {
                    try {
                        if (fosRawMixAudio != null) {
                            fosRawMixAudio.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            });
            audioMixer.mixAudios(rawAudioFiles);

            AudioEncoder accEncoder = AudioEncoder.createAccEncoder(mixingAudioPath);
            accEncoder.setSampleRate(defaultAudioFormatData.sampleRate);
            accEncoder.setChannelCount(defaultAudioFormatData.channelCount);
            accEncoder.encodeToFile(finalMixingPath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @param finalMovieLength
     * @param simpleMusicLength
     * @param simpleMusicTracks
     * @param musicTracks
     * @throws Exception
     */
    private void buildAudioByVideoLength(long finalMovieLength, long simpleMusicLength, List<Track> simpleMusicTracks, List<Track> musicTracks) throws Exception {
        //制作与视频文件大小一样的音频文件
        Movie musicMovie = new Movie();
        long finalMusicLength = 0;
        while (finalMovieLength > finalMusicLength) {
            long sub = finalMovieLength - finalMusicLength;
            if (sub >= simpleMusicLength) {
                musicMovie.addTrack(new AppendTrack(simpleMusicTracks.toArray(new Track[simpleMusicTracks.size()])));
                finalMusicLength += simpleMusicLength;
            } else {

                double startTime1 = 0;
                double endTime1 = sub / 1000;

                boolean timeCorrected = false;

                // Here we try to find a track that has sync samples. Since we can only start decoding
                // at such a sample we SHOULD make sure that the start of the new fragment is exactly
                // such a frame
                for (Track track : simpleMusicTracks) {
                    if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                        if (timeCorrected) {
                            // This exception here could be a false positive in case we have multiple tracks
                            // with sync samples at exactly the same positions. E.g. a single movie containing
                            // multiple qualities of the same video (Microsoft Smooth Streaming file)

                            throw new RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.");
                        }
                        startTime1 = correctTimeToSyncSample(track, startTime1, false);
                        endTime1 = correctTimeToSyncSample(track, endTime1, true);
                        timeCorrected = true;
                    }
                }

                for (Track track : simpleMusicTracks) {
                    long currentSample = 0;
                    double currentTime = 0;
                    double lastTime = -1;
                    long startSample1 = -1;
                    long endSample1 = -1;

                    for (int i = 0; i < track.getSampleDurations().length; i++) {
                        long delta = track.getSampleDurations()[i];


                        if (currentTime > lastTime && currentTime <= startTime1) {
                            // current sample is still before the new starttime
                            startSample1 = currentSample;
                        }
                        if (currentTime > lastTime && currentTime <= endTime1) {
                            // current sample is after the new start time and still before the new endtime
                            endSample1 = currentSample;
                        }
                        lastTime = currentTime;
                        currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
                        currentSample++;
                    }
                    musicMovie.addTrack(new CroppedTrack(track, startSample1, endSample1));
                }
                finalMusicLength += sub;
            }
        }

        //获取track
        for (Track t : musicMovie.getTracks()) {
            if (t.getHandler().equals("soun")) {
                musicTracks.add(t);
            }
        }
    }


    /**
     * 同步时间
     *
     * @param track
     * @param cutHere
     * @param next
     * @return
     */
    private double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1
                timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }

    /**
     * 视频解析
     *
     * @param videoPath
     * @param videoTracks
     * @param audioTracks
     * @return
     */
    private long buildVideoMovie(String videoPath, List<Track> videoTracks, List<Track> audioTracks) {
        //视频解析
        Movie m;
        long finalMovieLength = 0;
        try {
            m = MovieCreator.build(videoPath);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        for (Track t : m.getTracks()) {
            if (t.getHandler().equals("soun")) {
                audioTracks.add(t);
            }
            if (t.getHandler().equals("vide")) {
                videoTracks.add(t);
                // calculate accurate time length;
                finalMovieLength += (t.getDuration() * 1000l / t.getTrackMetaData().getTimescale());
            }
        }
        return finalMovieLength;
    }

    /**
     * 音频解析
     *
     * @param musicPath
     * @param simpleMusicTracks
     * @return
     */
    private long buildAudioMovie(String musicPath, List<Track> simpleMusicTracks) {
        //音频解析
        Movie music;
        long simpleMusicLength = 0;
        try {
            music = MovieCreator.build(musicPath);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        for (Track t : music.getTracks()) {
            if (t.getHandler().equals("soun")) {
                simpleMusicTracks.add(t);
                simpleMusicLength += (t.getDuration() * 1000l / t.getTrackMetaData().getTimescale());
            }
        }
        return simpleMusicLength;
    }
}
