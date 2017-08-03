package com.zmy.next.beautyface;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zmy.next.beautylib.utils.BeautyConfig;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Button glSurface, surface, texture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBeautify();
        setContentView(R.layout.activity_main);
        glSurface = (Button) findViewById(R.id.glSurface);
        surface = (Button) findViewById(R.id.surface);
        texture = (Button) findViewById(R.id.texture);

        glSurface.setOnClickListener(this);
        surface.setOnClickListener(this);
        texture.setOnClickListener(this);
    }

    private void initBeautify() {
        BeautyConfig.context = getApplicationContext();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.glSurface:
                startActivity(new Intent(this, GlSurfaceActivity.class));
                break;
            case R.id.surface:
                break;
            case R.id.texture:
                break;
        }
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
