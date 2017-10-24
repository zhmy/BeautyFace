package com.zmy.next.beautyface.media;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/**
 * Android SDK 支持的编码器
 *
 * @author Darcy
 * @version android.os.Build.VERSION.SDK_INT >= 16
 */
public class AndroidAudioDecoder extends AudioDecoder {

    AndroidAudioDecoder(String encodefile) {
        super(encodefile);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public RawAudioInfo decodeToFile(String outFile, boolean isMatch,
                                     MediaUtils.AudioFormatData defaultAudioFormatData,
                                     MediaUtils.AudioFormatData audioFormatData) throws Exception {
        if (TextUtils.isEmpty(outFile) || defaultAudioFormatData == null || audioFormatData == null) {
            return null;
        }

        long beginTime = System.currentTimeMillis();

        final String encodeFile = mEncodeFile;
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(encodeFile);

        MediaFormat mediaFormat = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                extractor.selectTrack(i);
                mediaFormat = format;
                break;
            }
        }

        if (mediaFormat == null) {
            Log.e("zmy", "not a valid file with audio track..");
            extractor.release();
            return null;
        }

        Log.e("zmy", "mediaFormat " + mediaFormat);
        RawAudioInfo rawAudioInfo = new RawAudioInfo();
        rawAudioInfo.channel = audioFormatData.channelCount;
        rawAudioInfo.sampleRate = audioFormatData.sampleRate;
        rawAudioInfo.bitWidth = audioFormatData.bitWidth;
        rawAudioInfo.tempRawFile = outFile;
        FileOutputStream fosDecoder = new FileOutputStream(rawAudioInfo.tempRawFile);

        String mediaMime = mediaFormat.getString(MediaFormat.KEY_MIME);
        MediaCodec codec = MediaCodec.createDecoderByType(mediaMime);
        codec.configure(mediaFormat, null, null, 0);
        codec.start();

        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

        final double audioDurationUs = mediaFormat.getLong(MediaFormat.KEY_DURATION);
        final long kTimeOutUs = 5000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int totalRawSize = 0;
        try {
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                        int sampleSize = extractor.readSampleData(dstBuf, 0);
                        if (sampleSize < 0) {
                            Log.i("zmy", "saw input EOS.");
                            sawInputEOS = true;
                            codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            long presentationTimeUs = extractor.getSampleTime();
                            codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0);
                            extractor.advance();
                        }
                    }
                }
                int res = codec.dequeueOutputBuffer(info, kTimeOutUs);
                if (res >= 0) {

                    int outputBufIndex = res;
                    // Simply ignore codec config buffers.
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        Log.i("zmy", "audio encoder: codec config buffer");
                        codec.releaseOutputBuffer(outputBufIndex, false);
                        continue;
                    }

                    if (info.size != 0) {

                        ByteBuffer outBuf = codecOutputBuffers[outputBufIndex];

                        outBuf.position(info.offset);
                        outBuf.limit(info.offset + info.size);
                        byte[] data = new byte[info.size];
                        outBuf.get(data);
                        totalRawSize += data.length;

                        byte[] convertData = null;
                        byte[] finalConvertData = null;
                        if (!isMatch) {
                            if (audioFormatData.isConvertBit()) {
                                convertData = MediaUtils.convertByteWidth(audioFormatData.bitWidth / 8, defaultAudioFormatData.bitWidth / 8, data);
                            }
                            if (audioFormatData.isConvertChannel()) {
                                finalConvertData = MediaUtils.convertChannelCount(audioFormatData.channelCount, defaultAudioFormatData.channelCount,
                                        defaultAudioFormatData.bitWidth / 8, convertData == null ? data : convertData);
                            }
                        }
                        fosDecoder.write(finalConvertData == null ? convertData == null ? data : convertData : finalConvertData);
                        if (mOnAudioDecoderListener != null)
                            mOnAudioDecoderListener.onDecode(data, info.presentationTimeUs / audioDurationUs);
                        Log.i("zmy", mEncodeFile + " presentationTimeUs : " + info.presentationTimeUs);
                    }

                    codec.releaseOutputBuffer(outputBufIndex, false);

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.i("zmy", "saw output EOS.");
                        sawOutputEOS = true;
                    }

                } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    codecOutputBuffers = codec.getOutputBuffers();
                    Log.i("zmy", "output buffers have changed.");
                } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat oformat = codec.getOutputFormat();
//                    rawAudioInfo.sampleRate = oformat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
//                            oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE) :
//                            MediaUtils.DEFAULT_AUDIO_SAMPLE_RATE;
//                    rawAudioInfo.channel = oformat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ?
//                            oformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) :
//                            MediaUtils.DEFAULT_AUDIO_CHANNEL_COUNT;
                    Log.e("zmy", "output format has changed to " + oformat);
                }
            }
            rawAudioInfo.size = totalRawSize;

            if (mOnAudioDecoderListener != null)
                mOnAudioDecoderListener.onDecode(null, 1);

            Log.i("zmy", "decode " + outFile + " cost " + (System.currentTimeMillis() - beginTime) + " milliseconds !");

            return rawAudioInfo;
        } finally {
            fosDecoder.close();
            codec.stop();
            codec.release();
            extractor.release();
        }
    }
}
