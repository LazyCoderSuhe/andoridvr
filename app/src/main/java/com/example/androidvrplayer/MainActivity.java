package com.example.androidvrplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.example.shvr.InitCall;
import com.example.shvr.InitCallParam;
import com.example.shvr.VRRenderer;

import org.rajawali3d.view.ISurface;
import org.rajawali3d.view.SurfaceView;

public class MainActivity extends AppCompatActivity {

    LinearLayout linearLayout1;
    VRRenderer renderer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linearLayout1 = findViewById(R.id.linear);
        SurfaceView surface = new SurfaceView(this);
        surface.setFrameRate(60.0);
        surface.setRenderMode(ISurface.RENDERMODE_WHEN_DIRTY);


        linearLayout1.addView(surface);
        renderer = new VRRenderer(this, new InitCall() {
            @Override
            public void init() {

            }

            @Override
            public void ctor(InitCallParam param) {
                param.url=  "https://lyflyprojectbucket.oss-cn-beijing.aliyuncs.com/aec2166d2c884399b4236cb7cfae44e5.mp4";
            }
        });;
        surface.setSurfaceRenderer(renderer);
        surface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                renderer.setVrState(!renderer.getVRState());
//                if (renderer.mMediaPlayer.isPlaying()) {
//                    renderer.mMediaPlayer.pause();
//                 //   renderer.setOrientation(true);
//                }
//                else
//                    renderer.mMediaPlayer.start();
            }
        });

    }
}