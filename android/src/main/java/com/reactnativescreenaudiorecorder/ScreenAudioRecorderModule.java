package com.reactnativescreenaudiorecorder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

@ReactModule(name = ScreenAudioRecorderModule.NAME)
public class ScreenAudioRecorderModule extends ReactContextBaseJavaModule {
    public static final String NAME = "ScreenAudioRecorder";
    private final ReactApplicationContext reactContext;

    private ScreenAudioRecorderService recordService;

    private int sampleRateInHz;
    private int channelConfig;
    private int audioFormat;
    private Boolean fromMic;
    private String tmpFile;
    private String outFile;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;

    public static final int MEDIA_PROJECTION_REQUEST_CODE = 1001;

    private ServiceConnection connection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName className, IBinder service) {
        ScreenAudioRecorderService.ScreenAudioRecorder binder = (ScreenAudioRecorderService.ScreenAudioRecorder) service;
        recordService = binder.getRecordService();
      }

      @Override
      public void onServiceDisconnected(ComponentName arg0) {}
    };

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
      @RequiresApi(api = Build.VERSION_CODES.Q)
      @Override
      public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        if(requestCode == MEDIA_PROJECTION_REQUEST_CODE && resultCode == activity.RESULT_OK){
          mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, intent);
          recordService.setSampleRateInHz(sampleRateInHz);
          recordService.setChannelConfig(channelConfig);
          recordService.setAudioFormat(audioFormat);
          recordService.setOutFile(outFile);
          recordService.setTmpFile(tmpFile);
          recordService.setEventEmitter(eventEmitter);
          recordService.setBufferSize(AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat));
          recordService.setRecordingBufferSize(AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat) * 3);
          recordService.setFromMic(fromMic);
          recordService.setMediaProject(mediaProjection);
          recordService.startAudioCapture();
        }
      }
    };

    public ScreenAudioRecorderModule(ReactApplicationContext reactContext) {

      super(reactContext);
      reactContext.addActivityEventListener(mActivityEventListener);
      reactContext.startService(new Intent(reactContext, ScreenAudioRecorderService.class));
      this.reactContext = reactContext;

    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }


  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @ReactMethod
  public void init(ReadableMap options) {
    sampleRateInHz = 44100;
    if (options.hasKey("sampleRate")) {
      sampleRateInHz = options.getInt("sampleRate");
    }

    channelConfig = AudioFormat.CHANNEL_IN_MONO;
    if (options.hasKey("channels")) {
      if (options.getInt("channels") == 2) {
        channelConfig = AudioFormat.CHANNEL_IN_STEREO;
      }
    }

    audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    if (options.hasKey("bitsPerSample")) {
      if (options.getInt("bitsPerSample") == 8) {
        audioFormat = AudioFormat.ENCODING_PCM_8BIT;
      }
    }

    fromMic = false;
    if (options.hasKey("fromMic")) {
      fromMic = options.getBoolean("fromMic");
    }

    String documentDirectoryPath = getReactApplicationContext().getFilesDir().getAbsolutePath();
    outFile = documentDirectoryPath + "/" + "audio.wav";
    tmpFile = documentDirectoryPath + "/" + "temp.pcm";
    eventEmitter = reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

    if (options.hasKey("fileName")) {
      String fileName = options.getString("fileName");
      outFile = documentDirectoryPath + "/" + fileName;
    }

    mediaProjectionManager = (MediaProjectionManager) getCurrentActivity().getSystemService(reactContext.MEDIA_PROJECTION_SERVICE);
    Intent intent = new Intent(getCurrentActivity(), ScreenAudioRecorderService.class);
    getCurrentActivity().bindService(intent, connection,  getCurrentActivity().BIND_AUTO_CREATE);
  }

  @RequiresApi(api = Build.VERSION_CODES.Q)
    @ReactMethod
    public void start() {
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        getCurrentActivity().startActivityForResult(captureIntent, MEDIA_PROJECTION_REQUEST_CODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @ReactMethod
    public void stop(Promise promise) {
        recordService.stopAudioCapture(promise);
    }
}
