package com.example.shvr;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import org.rajawali3d.view.ISurface;
import org.rajawali3d.view.SurfaceView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    LinearLayout linearLayout1;
    VRRenderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        linearLayout1 = findViewById(R.id.leanere);
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
                param.url = "https://lyflyprojectbucket.oss-cn-beijing.aliyuncs.com/aec2166d2c884399b4236cb7cfae44e5.mp4";
                param.autoplay = true;
                param.flength = 98;

            }
        });
        ;
        surface.setSurfaceRenderer(renderer);
        Timer timer = new Timer();
        float[] f = {0.5f};
        surface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // renderer.setVrState(!renderer.getVRState());


                Log.d("ssssss", "onClick: =" + renderer.getFlength());
              //  renderer.setFlength(renderer.getFlength()-1);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (VRRenderer.mMediaPlayer.isPlaying())
                            VRRenderer.mMediaPlayer.pause();
//                        if (renderer.getFlength() <= 0) {
//                            f[0] = 0.5f;
//                        }
                        if (renderer.getFlength() >= 99) {
                            f[0] = -0.5f;
                        }
                        renderer.setFlength(renderer.getFlength()+f[0]);
                        Log.d("ssssss", "run: renderer" + renderer.getFlength());
                    }
                }, 0, 40);

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