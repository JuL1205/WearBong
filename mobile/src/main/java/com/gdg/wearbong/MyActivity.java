package com.gdg.wearbong;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import java.io.ByteArrayOutputStream;
public class MyActivity extends ActionBarActivity implements
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private Camera mcamera;
    private GoogleApiClient mGoogleApiClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        SurfaceHolder surfaceholder;
        SurfaceView surfaceview = (SurfaceView)findViewById(R.id.surface);
        surfaceholder=surfaceview.getHolder();
        surfaceholder.addCallback(new SurfaceHolder.Callback(){
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mcamera=Camera.open();
                try{
                    mcamera.setPreviewDisplay(holder);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            public void surfaceChanged(SurfaceHolder holder,int type, int w,int h ){
                Camera.Parameters parameters = mcamera.getParameters();
                parameters.setRotation(90);
                Log.d("fuck", parameters.getHorizontalViewAngle() + " 90");
                //parameters.setPreviewSize(w,h);
                mcamera.setParameters(parameters);
                mcamera.startPreview();
                mcamera.takePicture(null,null,new Camera.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data, Camera camera) {
                                ;
                            }
                        }
                );
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mcamera.release();
                mcamera=null;

            }
        });
        surfaceholder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("jul", "onConnectionFailed");
    }
    @Override
    public void onConnectionSuspended(int i) {
        Log.e("jul", "onConnectionSuspended");
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.e("jul", "onConnected");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }
    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d("jul", "DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d("jul", "DataItem changed: " + event.getDataItem().getUri());
            }
        }
    }
    private Asset createAssetFromBitmap(Bitmap bitmap){
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return Asset.createFromBytes(bos.toByteArray());
    }
}
