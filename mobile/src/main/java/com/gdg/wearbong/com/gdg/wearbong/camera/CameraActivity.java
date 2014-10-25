package com.gdg.wearbong.com.gdg.wearbong.camera;

import android.app.Activity;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceView;

import com.gdg.wearbong.R;

/**
 * Created by owner on 14. 10. 25..
 */
public class CameraActivity extends Activity {

    private SurfaceView mSvCamera;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();
    }

    private void initViews() {
        mSvCamera = (SurfaceView) findViewById(R.id.sv_camera);
    }
}
