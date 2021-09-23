import * as React from 'react';
import { useEffect, useState } from 'react';
import { StyleSheet, View, Button, PermissionsAndroid } from 'react-native';
import Sound from 'react-native-sound';
import ScreenAudioRecorder from 'react-native-screen-audio-recorder';

export default function App() {
  const [audioFile, setAudioFile] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [isPaused, setIsPause] = useState(true);

  const requestRecordPermission = async () => {
    const recordPermission = await PermissionsAndroid.check(
      PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
    );

    if (!recordPermission) {
      let granted;
      try {
        granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
        );

        if (granted === PermissionsAndroid.RESULTS.GRANTED) {
          console.log('You can use the mic');
        } else {
          console.log('Mic permission denied');
        }
      } catch (err) {
        console.log(err);
      }
    }
  };

  useEffect(() => {
    ScreenAudioRecorder.init({
      sampleRate: 16000,
      channels: 1,
      bitsPerSample: 16,
      fileName: 'novo.wav',
      fromMic: false,
      saveFile: true,
      audioEmitInterval: 10000,
    });

    ScreenAudioRecorder.on('data', (data) => {
      console.log(data);
    });

    requestRecordPermission();
  }, []);

  const start = () => {
    setIsRecording(true);
    ScreenAudioRecorder.start();
  };

  const pause = () => {
    setIsPause(true);
  };

  const stop = async () => {
    const filePath = await ScreenAudioRecorder.stop();
    setIsRecording(false);
    setAudioFile(filePath);
  };

  const play = async () => {
    setIsPause(false);

    Sound.setCategory('Playback');
    const auxSound = new Sound(audioFile, '', (error: any) => {
      if (error) {
        console.log('failed to load the file', error);
      } else {
        auxSound.play(() => {
          setIsPause(true);
        });
      }
    });
  };

  return (
    <View style={styles.container}>
      <View style={styles.row}>
        <Button onPress={start} title="Record" disabled={isRecording} />
        <Button onPress={stop} title="Stop" disabled={!isRecording} />
        {isPaused ? (
          <Button
            onPress={play}
            title="Play"
            disabled={!audioFile || isRecording}
          />
        ) : (
          <Button
            onPress={pause}
            title="Pause"
            disabled={!audioFile || isRecording}
          />
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-evenly',
  },
});
