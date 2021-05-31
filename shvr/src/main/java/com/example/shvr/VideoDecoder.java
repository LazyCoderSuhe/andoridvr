package com.example.shvr;

import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;




public class VideoDecoder  {



    private boolean isRuning;
    private byte[] input;
    private int width;
    private int height;
    private MediaCodec mediaCodec;
    private MediaMuxer mediaMuxer;
    private int mVideoTrack=-1;
    private long nanoTime;
    MediaFormat videoFormat;

    public void init(int width, int heigth) {
        nanoTime = System.nanoTime();
        this.width = width;
        this.height = heigth;

         videoFormat = MediaFormat.createVideoFormat("video/avc", width, heigth);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * heigth * 5);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);


    }
    public  void start()
    {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            mediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            //  mediaCodec.setInputSurface();
            mediaCodec.start();

            mediaMuxer = new MediaMuxer("sdcard/aaapcm/camer1.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
