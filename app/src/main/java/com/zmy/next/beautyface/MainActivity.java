package com.zmy.next.beautyface;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.zmy.next.beautyface.media.MediaConstants;
import com.zmy.next.beautyface.media.MultimediaMixer;
import com.zmy.next.beautyface.moov.QtFastStart;
import com.zmy.next.beautylib.utils.BeautyConfig;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button glSurface, video, mixer, moov, other;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBeautify();
        setContentView(R.layout.activity_main);
        glSurface = (Button) findViewById(R.id.glSurface);
        video = (Button) findViewById(R.id.video);
        mixer = (Button) findViewById(R.id.mixer);
        moov = (Button) findViewById(R.id.moov);
        other = (Button) findViewById(R.id.other);

        glSurface.setOnClickListener(this);
        video.setOnClickListener(this);
        mixer.setOnClickListener(this);
        moov.setOnClickListener(this);
        other.setOnClickListener(this);
    }

    private void initBeautify() {
        BeautyConfig.context = getApplicationContext();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (grantResults.length != 1 || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            int id = 0;
            switch (requestCode) {
                case 0x01:
                    id = glSurface.getId();
                    break;
                case 0x02:
                    id = video.getId();
                    break;
                case 0x03:
                    id = mixer.getId();
                    break;
                case 0x04:
                    id = moov.getId();
                    break;
                case 0x05:
                    id = other.getId();
                    break;
            }
            startActivity(id);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View v) {
        int requestCode = 0;
        switch (v.getId()) {
            case R.id.glSurface:
                requestCode = 0x01;
                break;
            case R.id.video:
                requestCode = 0x02;
                break;
            case R.id.mixer:
                requestCode = 0x03;
                break;
            case R.id.moov:
                requestCode = 0x04;
                break;
            case R.id.other:
                requestCode = 0x05;
                break;
        }
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
                || PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
        } else {
            startActivity(v.getId());
        }
    }

    private void startActivity(int id) {
        switch (id) {
            case R.id.glSurface:
                startActivity(new Intent(this, GlSurfaceActivity.class));
                break;
            case R.id.video:
                mergeVideo();
                break;
            case R.id.mixer:
                mergeMusic();
                break;
            case R.id.moov:
                testMoov();
                break;
            case R.id.other:
                testDir();
                break;
        }
    }

    private void testMoov() {
//        String src = "/sdcard/tieba/f_44.mp4";
        String src = "/sdcard/DCIM/Camera/20170816_112246.mp4";
        String dest = MediaConstants.TEMP_VIDEO_FULL_DIR + "zmy_moov" + System.currentTimeMillis() + ".mp4";

        boolean success = false;
        long startTime = System.currentTimeMillis();
        try {
            success = QtFastStart.fastStart(src, dest);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (QtFastStart.MalformedFileException e) {
            e.printStackTrace();
        } catch (QtFastStart.UnsupportedFileException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "success = " + success + " cost : " + (System.currentTimeMillis() - startTime) + "ms", Toast.LENGTH_SHORT).show();
    }

    private void testDir() {
        String outputPath = "/sdcard/zzz/hh2/1.mp4";
        File file = new File(outputPath);
        file.getParentFile().mkdirs();
        if (file.exists()) {
            file.delete();
        }
    }

    public void mergeVideo() {
        List<String> videoPathList = new ArrayList<>();
        videoPathList.add("/sdcard/DCIM/Camera/20170816_112246.mp4");
        videoPathList.add("/sdcard/DCIM/Camera/20170816_112255.mp4");
        videoPathList.add("/sdcard/DCIM/Camera/20170816_112304.mp4");

        String videoPath = MediaConstants.TEMP_VIDEO_FULL_DIR + "zmy_video" + System.currentTimeMillis() + ".mp4";
        long startTime = System.currentTimeMillis();

        boolean success = MultimediaMixer.getInstance().mixingVideoByVideo(videoPathList, videoPath);

        Toast.makeText(this, "success = " + success + " cost : " + (System.currentTimeMillis() - startTime) + "ms", Toast.LENGTH_SHORT).show();
    }

    public void mergeMusic() {
//        String videoPath = "/sdcard/DCIM/zmy/zmy_1502854482153.mp4";//无声
        String videoPath = "/sdcard/DCIM/zmy/zmy_1504235309157.mp4";
        String musicPath = "/sdcard/DCIM/2.m4a";
        String outputPath = MediaConstants.TEMP_VIDEO_FULL_DIR + "zmy_audio" + System.currentTimeMillis() + ".mp4";
        long startTime = System.currentTimeMillis();

        boolean success = MultimediaMixer.getInstance().mixingVideoByAudio(videoPath, musicPath, outputPath);

        Toast.makeText(this, "success = " + success + " cost : " + (System.currentTimeMillis() - startTime) + "ms", Toast.LENGTH_SHORT).show();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
