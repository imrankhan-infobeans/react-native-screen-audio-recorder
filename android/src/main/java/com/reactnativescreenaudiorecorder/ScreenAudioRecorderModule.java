package com.reactnativescreenaudiorecorder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Objects;

@ReactModule(name = ScreenAudioRecorderModule.NAME)
public class ScreenAudioRecorderModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    public static final String NAME = "ScreenAudioRecorder";
    private final ReactApplicationContext reactContext;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private ScreenAudioRecorderService recordService;
    private ReadableMap options;

    public static final int MEDIA_PROJECTION_REQUEST_CODE = 1001;

    public ScreenAudioRecorderModule(ReactApplicationContext reactContext) {
      super(reactContext);
      this.reactContext = reactContext;
      this.reactContext.addActivityEventListener(this);
    }

    private ServiceConnection connection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName className, IBinder service) {
        ScreenAudioRecorderService.ScreenAudioRecorder binder = (ScreenAudioRecorderService.ScreenAudioRecorder) service;
        recordService = binder.getRecordService();

        recordService.setSampleRateInHz(44100);
        if (options.hasKey("sampleRate")) {
          recordService.setSampleRateInHz(options.getInt("sampleRate"));
        }

        recordService.setChannelConfig(AudioFormat.CHANNEL_IN_MONO);
        if (options.hasKey("channels")) {
          if (options.getInt("channels") == 2) {
            recordService.setChannelConfig(AudioFormat.CHANNEL_IN_STEREO);
          }
        }

        recordService.setAudioFormat(AudioFormat.ENCODING_PCM_16BIT);
        if (options.hasKey("bitsPerSample")) {
          if (options.getInt("bitsPerSample") == 8) {
            recordService.setAudioFormat(AudioFormat.ENCODING_PCM_8BIT);
          }
        }

        recordService.setAudioEmitInterval(100);
        if (options.hasKey("audioEmitInterval")) {
          recordService.setAudioEmitInterval(options.getInt("audioEmitInterval"));
        }

        Boolean fromMic = false;
        recordService.setFromMic(false);
        if (options.hasKey("fromMic")) {
          fromMic = options.getBoolean("fromMic");
          recordService.setFromMic(options.getBoolean("fromMic"));
        }

        String documentDirectoryPath = getReactApplicationContext().getFilesDir().getAbsolutePath();

        recordService.setOutFile(documentDirectoryPath + "/" + "audio.wav");
        if (options.hasKey("fileName")) {
          recordService.setOutFile(documentDirectoryPath + "/" + options.getString("fileName"));
        }

        recordService.setSaveFile(false);
        if (options.hasKey("saveFile")) {
          recordService.setSaveFile(options.getBoolean("saveFile"));
        }

        recordService.calcBufferSize();
        recordService.calcRecordingBufferSize();
        recordService.setTmpFile(documentDirectoryPath + "/" + "temp.pcm");
        recordService.setEventEmitter(reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class));

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !fromMic) {
          mediaProjectionManager = (MediaProjectionManager) getCurrentActivity().getSystemService(reactContext.MEDIA_PROJECTION_SERVICE);
          Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
          reactContext.startActivityForResult(captureIntent, MEDIA_PROJECTION_REQUEST_CODE, null);
        }
        else
        {
          recordService.startAudioCapture();
        }
      }

      @Override
      public void onServiceDisconnected(ComponentName arg0) {}
    };

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
      if(requestCode == MEDIA_PROJECTION_REQUEST_CODE && resultCode == activity.RESULT_OK) {

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
          recordService.setMediaProject(mediaProjection);
        }

        recordService.startAudioCapture();
      }
    }

    @Override
    public void onNewIntent(Intent intent) { }

    @Override
    @NonNull
    public String getName() {
      return NAME;
    }

    @ReactMethod
    public void init(ReadableMap options) {
      this.options = options;
    }

    @ReactMethod
    public void start() {
      Intent intent = new Intent(getCurrentActivity(), ScreenAudioRecorderService.class);
      getCurrentActivity().bindService(intent, connection,  getCurrentActivity().BIND_AUTO_CREATE);
    }

    @ReactMethod
    public void stop(Promise promise) {
        recordService.stopAudioCapture(promise);
        Objects.requireNonNull(getCurrentActivity()).unbindService(connection);
    }
}
