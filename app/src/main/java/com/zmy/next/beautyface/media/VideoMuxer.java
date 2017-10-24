package com.zmy.next.beautyface.media;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 视频混合音频
 *
 * @author Darcy
 */
public abstract class VideoMuxer {

    String mOutputVideo;

    private VideoMuxer(String outputVideo) {
        this.mOutputVideo = outputVideo;
    }

    public final static VideoMuxer createVideoMuxer(String outputVideo) {
        return new Mp4Muxer(outputVideo);
    }

    /**
     * mix raw audio into video
     *
     * @param videoPath
     * @param rawAudioPath
     * @param includeAudioInVideo
     */
    public abstract void mixRawAudio(String videoPath, String rawAudioPath, String tempDir, boolean includeAudioInVideo);

    /**
     * use android sdk MediaMuxer
     *
     * @author Darcy
     * @version API >= 18
     */
    private static class Mp4Muxer extends VideoMuxer {

        private final static String AUDIO_MIME = "audio/mp4a-latm";
        private final static long audioBytesPerSample = 44100 * 16 / 8;

        private long rawAudioSize;

        public Mp4Muxer(String outputVideo) {
            super(outputVideo);
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void mixRawAudio(String videoFilePath, String rawAudioPath, String tempDir, boolean includeAudioInVideo) {
            MediaMuxer videoMuxer = null;
            try {
                videoMuxer = new MediaMuxer(mOutputVideo, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                //获取视频media format
                MediaFormat videoFormat = null;
                MediaFormat audioFormat = null;
                MediaExtractor videoExtractor = new MediaExtractor();
                videoExtractor.setDataSource(videoFilePath);


                for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
                    MediaFormat format = videoExtractor.getTrackFormat(i);
                    String mime = format.getString(MediaFormat.KEY_MIME);
                    if (mime.startsWith("video/")) {
                        videoExtractor.selectTrack(i);
                        videoFormat = format;
                    }
                    if (mime.startsWith("audio/")) {
                        audioFormat = format;
                    }
                }

                int videoTrackIndex = videoMuxer.addTrack(videoFormat);
                int audioTrackIndex = 0;

                //extract and decode audio
                FileInputStream fisExtractAudio = null;
                FileInputStream fisMixAudio = null;

                MediaUtils.AudioFormatData defaultAudioFormatData = MediaUtils.getAudioFormat(videoFilePath);
                MediaUtils.AudioFormatData audioFormatData = MediaUtils.getAudioFormat(rawAudioPath);

                boolean isMatch = MediaUtils.isMatchAudioFormat(defaultAudioFormatData, audioFormatData);

                // 音频 解码到FileInputStream
                AndroidAudioDecoder rawAudioDecoder = new AndroidAudioDecoder(rawAudioPath);
                String rawExtractAudioFilePath = tempDir + "temp_" + System.currentTimeMillis();
                rawAudioDecoder.decodeToFile(rawExtractAudioFilePath, isMatch, defaultAudioFormatData, audioFormatData);

                if (!isMatch && audioFormatData.isResample()) {
                    String resampleAudioPath = tempDir + "resample_" + System.currentTimeMillis();
                    long startTime = System.currentTimeMillis();
                    boolean result = MediaUtils.resampling(rawExtractAudioFilePath, resampleAudioPath,
                            audioFormatData.sampleRate, defaultAudioFormatData.sampleRate);
                    if (result) {
                        rawExtractAudioFilePath = resampleAudioPath;
                    }
                }

                File rawExtractAudioFile = new File(rawExtractAudioFilePath);
                fisMixAudio = new FileInputStream(rawExtractAudioFile);

                //包含视频里的音频 解码到FileInputStream
                if (includeAudioInVideo) {
                    AndroidAudioDecoder audioDecoder = new AndroidAudioDecoder(videoFilePath);
                    String extractAudioFilePath = tempDir + "temp_" + System.currentTimeMillis();

                    audioDecoder.decodeToFile(extractAudioFilePath, isMatch, defaultAudioFormatData, defaultAudioFormatData);

                    File extractAudioFile = new File(extractAudioFilePath);
                    fisExtractAudio = new FileInputStream(extractAudioFile);
                }

                boolean readExtractAudioEOS = !includeAudioInVideo;
                boolean readMixAudioEOS = false;
                byte[] extractAudioBuffer = new byte[4096];
                byte[] mixAudioBuffer = new byte[4096];
                int extractAudioReadCount = 0;
                int mixAudioReadCount = 0;

                final MultiAudioMixer audioMixer = MultiAudioMixer.createAudioMixer();
                final byte[][] twoAudioBytes = new byte[2][];

                final MediaCodec audioEncoder = createACCAudioDecoder(audioFormat);
                audioEncoder.start();

                ByteBuffer[] audioInputBuffers = audioEncoder.getInputBuffers();
                ByteBuffer[] audioOutputBuffers = audioEncoder.getOutputBuffers();
                boolean sawInputEOS = false;
                boolean sawOutputEOS = false;
                long audioTimeUs = 0;
                BufferInfo outBufferInfo = new BufferInfo();

                int inputBufIndex, outputBufIndex;
                while (!sawOutputEOS) {
                    if (!sawInputEOS) {
                        inputBufIndex = audioEncoder.dequeueInputBuffer(10000);
                        if (inputBufIndex >= 0) {
                            ByteBuffer inputBuffer = audioInputBuffers[inputBufIndex];
                            inputBuffer.clear();

                            int bufferSize = inputBuffer.remaining();
                            if (bufferSize != extractAudioBuffer.length) {
                                extractAudioBuffer = new byte[bufferSize];
                                mixAudioBuffer = new byte[bufferSize];
                            }

                            if (!readExtractAudioEOS) {
                                extractAudioReadCount = fisExtractAudio.read(extractAudioBuffer);
                                if (extractAudioReadCount == -1) {
                                    readExtractAudioEOS = true;
                                }
                            }

                            if (!readMixAudioEOS) {
                                mixAudioReadCount = fisMixAudio.read(mixAudioBuffer);
                                if (mixAudioReadCount == -1) {
                                    readMixAudioEOS = true;
                                }
                            }

                            if (readExtractAudioEOS && readMixAudioEOS) {
                                audioEncoder.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                sawInputEOS = true;
                            } else {

                                byte[] mixAudioBytes;
                                if (!readExtractAudioEOS && !readMixAudioEOS) {
                                    if (extractAudioReadCount == mixAudioReadCount) {
                                        twoAudioBytes[0] = extractAudioBuffer;
                                        twoAudioBytes[1] = mixAudioBuffer;
                                    } else if (extractAudioReadCount > mixAudioReadCount) {
                                        twoAudioBytes[0] = extractAudioBuffer;
                                        Arrays.fill(mixAudioBuffer, mixAudioReadCount - 1, bufferSize, (byte) 0);
                                    } else {
                                        Arrays.fill(extractAudioBuffer, extractAudioReadCount - 1, bufferSize, (byte) 0);
                                    }
                                    //合成音轨
                                    mixAudioBytes = audioMixer.mixRawAudioBytes(twoAudioBytes);
                                    if (mixAudioBytes == null) {
                                        Log.e("zmy", "mix audio : null");
                                    }
                                    inputBuffer.put(mixAudioBytes);
                                    rawAudioSize += mixAudioBytes.length;
                                    audioEncoder.queueInputBuffer(inputBufIndex, 0, mixAudioBytes.length, audioTimeUs, 0);
                                } else if (!readExtractAudioEOS && readMixAudioEOS) {
                                    inputBuffer.put(extractAudioBuffer, 0, extractAudioReadCount);
                                    rawAudioSize += extractAudioReadCount;
                                    audioEncoder.queueInputBuffer(inputBufIndex, 0, extractAudioReadCount, audioTimeUs, 0);
                                } else {
                                    inputBuffer.put(mixAudioBuffer, 0, mixAudioReadCount);
                                    rawAudioSize += mixAudioReadCount;
                                    audioEncoder.queueInputBuffer(inputBufIndex, 0, mixAudioReadCount, audioTimeUs, 0);
                                }

                                audioTimeUs = (long) (1000000 * (rawAudioSize / 2.0) / audioBytesPerSample);
                            }
                        }
                    }

                    outputBufIndex = audioEncoder.dequeueOutputBuffer(outBufferInfo, 10000);
                    if (outputBufIndex >= 0) {

                        // Simply ignore codec config buffers.
                        if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            Log.e("zmy", "audio encoder: codec config buffer");
                            audioEncoder.releaseOutputBuffer(outputBufIndex, false);
                            continue;
                        }

                        if (outBufferInfo.size != 0) {
                            ByteBuffer outBuffer = audioOutputBuffers[outputBufIndex];
                            outBuffer.position(outBufferInfo.offset);
                            outBuffer.limit(outBufferInfo.offset + outBufferInfo.size);
                            Log.e("zmy", String.format(" writing audio sample : size=%s , presentationTimeUs=%s", outBufferInfo.size, outBufferInfo.presentationTimeUs));
                            if (lastAudioPresentationTimeUs < outBufferInfo.presentationTimeUs) {
                                videoMuxer.writeSampleData(audioTrackIndex, outBuffer, outBufferInfo);
                                lastAudioPresentationTimeUs = outBufferInfo.presentationTimeUs;
                            } else {
                                Log.e("zmy", "error sample! its presentationTimeUs should not lower than before.");
                            }
                        }

                        audioEncoder.releaseOutputBuffer(outputBufIndex, false);

                        if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            sawOutputEOS = true;
                        }
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        audioOutputBuffers = audioEncoder.getOutputBuffers();
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        audioFormat = audioEncoder.getOutputFormat();
                        audioTrackIndex = videoMuxer.addTrack(audioFormat);
                        videoMuxer.start(); //start muxer
                    }
                }

                if (fisExtractAudio != null) {
                    fisExtractAudio.close();
                }

                fisMixAudio.close();
                audioEncoder.stop();
                audioEncoder.release();

                //mix video
                boolean videoMuxDone = false;
                // 压缩帧大小 < 原始图片大小
                int videoWidth = videoFormat.getInteger(MediaFormat.KEY_WIDTH);
                int videoHeight = videoFormat.getInteger(MediaFormat.KEY_HEIGHT);
                ByteBuffer videoSampleBuffer = ByteBuffer.allocateDirect(videoWidth * videoHeight);
                BufferInfo videoBufferInfo = new BufferInfo();
                int sampleSize;
                while (!videoMuxDone) {
                    videoSampleBuffer.clear();
                    sampleSize = videoExtractor.readSampleData(videoSampleBuffer, 0);
                    if (sampleSize < 0) {
                        videoMuxDone = true;
                    } else {
                        videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                        videoBufferInfo.flags = videoExtractor.getSampleFlags();
                        videoBufferInfo.size = sampleSize;
                        videoSampleBuffer.limit(sampleSize);
                        videoMuxer.writeSampleData(videoTrackIndex, videoSampleBuffer, videoBufferInfo);
                        videoExtractor.advance();
                    }
                }

                videoExtractor.release();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (videoMuxer != null) {
                    videoMuxer.stop();
                    videoMuxer.release();
                    Log.e("zmy", "video mix complete.");
                }
            }
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        private MediaCodec createACCAudioDecoder(MediaFormat audioFormat) throws IOException {
            MediaCodec codec = MediaCodec.createEncoderByType(AUDIO_MIME);
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, AUDIO_MIME);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
            format.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            return codec;
        }

        private long lastAudioPresentationTimeUs = -1;
    }
}
