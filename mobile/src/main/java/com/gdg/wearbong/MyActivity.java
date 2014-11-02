package com.gdg.wearbong;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;


public class MyActivity extends ActionBarActivity implements
        DataApi.DataListener{

    private String TAG = "jul";

    private GoogleApiClient mGoogleApiClient;
    private Camera mCamera;
    private Node mWearNode = null;

    private int CapturePhotoTime = 0;

    protected MessageApi.MessageListener mMessageListener = new MessageApi.MessageListener(){ // listener
        @Override

        public void onMessageReceived(MessageEvent messageEvent) {
            Scanner s = new Scanner(messageEvent.getPath());
            String command = s.next();
            if(command.compareTo("capture") == 0){ // to capture
                takePhoto();
            } else if(command.compareTo("timer") == 0){
              if(s.hasNextInt()) {
                  int args = s.nextInt();
                  setTimer(args);
              }
            } else {
                //getPreview(~~);
            }

            s.close();
        }
    };

    private void setTimer(int args){ CapturePhotoTime = args * 1000; }

    private void takePhoto(){
        TimerTask taken;
        Timer waitTime = new Timer();
        setTimer(3);
        taken = new TimerTask() {
            @Override
            public void run() {
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        sendToWear("result", data, null);
                    }
                });
            }
        };
        waitTime.schedule(taken, CapturePhotoTime);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_my);
        SurfaceHolder surfaceholder;
        SurfaceView surfaceview = (SurfaceView)findViewById(R.id.surface);
        surfaceholder=surfaceview.getHolder();
        surfaceholder.addCallback(new SurfaceHolder.Callback(){
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mCamera=Camera.open();
                try{
                    mCamera.setPreviewDisplay(holder);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
            public void surfaceChanged(SurfaceHolder holder,int type, int w,int h ){
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setRotation(90);
                parameters.setPreviewSize(w, h);
                mCamera.setParameters(parameters);
                mCamera.startPreview();
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();

                        int[] rgb = decodeYUV420SP(data, previewSize.width, previewSize.height);
                        Bitmap bmp = Bitmap.createBitmap(rgb, previewSize.width, previewSize.height, Bitmap.Config.ARGB_8888);
                        int smallWidth, smallHeight;
                        int dimension = 200;
                        if(previewSize.width > previewSize.height) {
                            smallWidth = dimension;
                            smallHeight = dimension*previewSize.height/previewSize.width;
                        } else {
                            smallHeight = dimension;
                            smallWidth = dimension*previewSize.width/previewSize.height;
                        }

                        Matrix matrix = new Matrix();

                        Bitmap bmpSmall = Bitmap.createScaledBitmap(bmp, smallWidth, smallHeight, false);
                        Bitmap bmpSmallRotated = Bitmap.createBitmap(bmpSmall, 0, 0, smallWidth, smallHeight, matrix, false);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bmpSmallRotated.compress(Bitmap.CompressFormat.WEBP, 80, baos);
                        sendToWear("preview", baos.toByteArray(), new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult result) {
                            }
                        });
                        bmp.recycle();
                        bmpSmall.recycle();
                        bmpSmallRotated.recycle();

                    }
                });

                mCamera.startPreview();
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera=null;

            }
        });
        surfaceholder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        findWearNode();
                        Wearable.MessageApi.addListener(mGoogleApiClient, mMessageListener);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                    }
                })
                .build();
    }


    public int[] decodeYUV420SP( byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        int rgb[]=new int[width*height];
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);
                if (r < 0) r = 0; else if (r > 262143) r = 262143;
                if (g < 0) g = 0; else if (g > 262143) g = 262143;
                if (b < 0) b = 0; else if (b > 262143) b = 262143;
                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
        return rgb;   }


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
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
            }
        }
    }
    private Asset createAssetFromBitmap(Bitmap bitmap){
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        return Asset.createFromBytes(bos.toByteArray());
    }

    void findWearNode() {
        PendingResult<NodeApi.GetConnectedNodesResult> pending = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        pending.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                if(result.getNodes().size()>0) {
                    mWearNode = result.getNodes().get(0);
                } else {
                    mWearNode = null;
                }
            }
        });
    }

    private void sendToWear(String path, byte[] data, final ResultCallback<MessageApi.SendMessageResult> callback) {
        if (mWearNode != null) {
            Log.d(TAG, "send image = "+data.length);
            PendingResult<MessageApi.SendMessageResult> pending = Wearable.MessageApi.sendMessage(mGoogleApiClient, mWearNode.getId(), path, data);
            pending.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult result) {
                    if (callback != null) {
                        callback.onResult(result);
                    }
                    if (!result.getStatus().isSuccess()) {
                        Log.d(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                }
            });
        } else {
            Log.d(TAG, "ERROR: tried to send message before device was found");
        }
    }
}
