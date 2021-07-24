package com.reactnativescreenaudiorecorder;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ScreenAudioRecorderService extends Service {
  private DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter;
  private MediaProjection mediaProjection;

  private AudioRecord recorder;
  private boolean isRecording;
  private int sampleRateInHz;
  private int channelConfig;
  private int audioFormat;
  private int bufferSize;
  private int recordingBufferSize;
  private Boolean fromMic;
  private String tmpFile;
  private String outFile;

  private Promise stopRecordingPromise;

  private int SERVICE_ID = 123;
  private String NOTIFICATION_CHANNEL_ID = "AudioCapture channel";

  @RequiresApi(api = Build.VERSION_CODES.O)
  // @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    isRecording = false;
    createNotificationChannel();
    startForeground(SERVICE_ID, new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).build());
    return new ScreenAudioRecorder();
  }

  @RequiresApi(api = Build.VERSION_CODES.O)
  private void createNotificationChannel() {
    NotificationChannel serviceChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Audio Capture Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
    NotificationManager manager = (NotificationManager) getSystemService(NotificationManager.class);
    manager.createNotificationChannel(serviceChannel);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  // @Nullable
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_STICKY;
  }

  public void startAudioCapture() {
    if(mediaProjection != null) {
      isRecording = true;

      if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !fromMic) {
        AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration
          .Builder(mediaProjection)
          .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
          .build();

        recorder = new AudioRecord
          .Builder()
          .setAudioPlaybackCaptureConfig(config)
          .setAudioFormat(
            new AudioFormat
              .Builder()
              .setSampleRate(sampleRateInHz)
              .setChannelMask(channelConfig)
              .setEncoding(audioFormat)
              .build()
          )
          .build();
      }
      else
      {
        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRateInHz, channelConfig, audioFormat, recordingBufferSize);
      }

      recorder.startRecording();

      Thread recordingThread = new Thread(new Runnable() {
        public void run() {
          try {
            int bytesRead;
            int count = 0;
            String base64Data;
            byte[] buffer = new byte[bufferSize];
            FileOutputStream os = new FileOutputStream(tmpFile);

            while (isRecording) {
              bytesRead = recorder.read(buffer, 0, buffer.length);

              // skip first 2 buffers to eliminate "click sound"
              if (bytesRead > 0 && ++count > 2) {
                base64Data = android.util.Base64.encodeToString(buffer, Base64.NO_WRAP);
                Log.d("ScreenAudioRecorder", base64Data);
                eventEmitter.emit("data", base64Data);
                os.write(buffer, 0, bytesRead);
              }
            }

            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !fromMic)
            {
              mediaProjection.stop();
            }

            recorder.stop();
            os.close();
            saveAsWav();
            stopSelf();
            stopRecordingPromise.resolve(outFile);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });

      recordingThread.start();
    }
  }

  public void stopAudioCapture(Promise promise) {
    stopRecordingPromise = promise;
    isRecording = false;
  }

  private void saveAsWav() {
    try {
      FileInputStream in = new FileInputStream(tmpFile);
      FileOutputStream out = new FileOutputStream(outFile);
      long totalAudioLen = in.getChannel().size();;
      long totalDataLen = totalAudioLen + 36;

      addWavHeader(out, totalAudioLen, totalDataLen);

      byte[] data = new byte[bufferSize];
      int bytesRead;
      while ((bytesRead = in.read(data)) != -1) {
        out.write(data, 0, bytesRead);
      }
      Log.d("ScreenAudioRecorder", "file path:" + outFile);
      Log.d("ScreenAudioRecorder", "file size:" + out.getChannel().size());

      in.close();
      out.close();
      deleteTempFile();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void addWavHeader(FileOutputStream out, long totalAudioLen, long totalDataLen)
    throws Exception {

    long sampleRate = sampleRateInHz;
    int channels = channelConfig == AudioFormat.CHANNEL_IN_MONO ? 1 : 2;
    int bitsPerSample = audioFormat == AudioFormat.ENCODING_PCM_8BIT ? 8 : 16;
    long byteRate =  sampleRate * channels * bitsPerSample / 8;
    int blockAlign = channels * bitsPerSample / 8;

    byte[] header = new byte[44];

    header[0] = 'R';                                    // RIFF chunk
    header[1] = 'I';
    header[2] = 'F';
    header[3] = 'F';
    header[4] = (byte) (totalDataLen & 0xff);           // how big is the rest of this file
    header[5] = (byte) ((totalDataLen >> 8) & 0xff);
    header[6] = (byte) ((totalDataLen >> 16) & 0xff);
    header[7] = (byte) ((totalDataLen >> 24) & 0xff);
    header[8] = 'W';                                    // WAVE chunk
    header[9] = 'A';
    header[10] = 'V';
    header[11] = 'E';
    header[12] = 'f';                                   // 'fmt ' chunk
    header[13] = 'm';
    header[14] = 't';
    header[15] = ' ';
    header[16] = 16;                                    // 4 bytes: size of 'fmt ' chunk
    header[17] = 0;
    header[18] = 0;
    header[19] = 0;
    header[20] = 1;                                     // format = 1 for PCM
    header[21] = 0;
    header[22] = (byte) channels;                       // mono or stereo
    header[23] = 0;
    header[24] = (byte) (sampleRate & 0xff);            // samples per second
    header[25] = (byte) ((sampleRate >> 8) & 0xff);
    header[26] = (byte) ((sampleRate >> 16) & 0xff);
    header[27] = (byte) ((sampleRate >> 24) & 0xff);
    header[28] = (byte) (byteRate & 0xff);              // bytes per second
    header[29] = (byte) ((byteRate >> 8) & 0xff);
    header[30] = (byte) ((byteRate >> 16) & 0xff);
    header[31] = (byte) ((byteRate >> 24) & 0xff);
    header[32] = (byte) blockAlign;                     // bytes in one sample, for all channels
    header[33] = 0;
    header[34] = (byte) bitsPerSample;                  // bits in a sample
    header[35] = 0;
    header[36] = 'd';                                   // beginning of the data chunk
    header[37] = 'a';
    header[38] = 't';
    header[39] = 'a';
    header[40] = (byte) (totalAudioLen & 0xff);         // how big is this data chunk
    header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
    header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
    header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

    out.write(header, 0, 44);
  }

  private void deleteTempFile() {
    File file = new File(tmpFile);
    file.delete();
  }

  public void setSampleRateInHz(int sampleRateInHz){
    this.sampleRateInHz = sampleRateInHz;
  }

  public void setChannelConfig(int channelConfig){
    this.channelConfig = channelConfig;
  }

  public void setAudioFormat(int audioFormat){
    this.audioFormat = audioFormat;
  }

  public void setTmpFile(String tmpFile){
    this.tmpFile = tmpFile;
  };

  public void setOutFile(String outFile){
    this.outFile = outFile;
  };

  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  public void setRecordingBufferSize(int recordingBufferSize) {
    this.recordingBufferSize = recordingBufferSize;
  }

  public void setEventEmitter(DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter) {
    this.eventEmitter = eventEmitter;
  }

  public void setMediaProject(MediaProjection mediaProjection) {
    this.mediaProjection = mediaProjection;
  }

  public void setFromMic(Boolean fromMic) {
    this.fromMic = fromMic;
  }

  public class ScreenAudioRecorder extends Binder {
    public ScreenAudioRecorderService getRecordService() {
      return ScreenAudioRecorderService.this;
    }
  }
}
