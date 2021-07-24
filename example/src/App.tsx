import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';

import { StyleSheet, View, PermissionsAndroid, Button } from 'react-native';
import Sound from 'react-native-sound';
import ScreenAudioRecorder, {
  Options,
} from 'react-native-screen-audio-recorder';

export default function App() {
  const [state, setState] = useState({
    audioFile: '',
    recording: false,
    loaded: false,
    paused: true,
  });

  const checkPermission = useCallback(async () => {
    const p = await PermissionsAndroid.check(
      PermissionsAndroid.PERMISSIONS.RECORD_AUDIO
    );

    console.log('permission check', p);

    if (p) {
      return;
    }
    return requestPermission();
  }, []);

  const requestPermission = async () => {
    let granted;
    try {
      granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
        {
          title: '',
          message: '',
          buttonNeutral: 'Ask Me Later',
          buttonNegative: 'Cancel',
          buttonPositive: 'OK',
        }
      );
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        console.log('You can use the mic');
      } else {
        console.log('Mic permission denied');
      }
    } catch (err) {
      console.warn(err);
    }

    console.log('permission request', granted);
  };

  const start = () => {
    console.log('start record');

    setState({ ...state, audioFile: '', recording: true, loaded: false });

    ScreenAudioRecorder.start();
  };

  const stop = async () => {
    if (!state.recording) {
      return;
    }

    console.log('stop record');
    let audioFile = await ScreenAudioRecorder.stop();
    console.log('audioFile', audioFile);

    setState({ ...state, recording: false, audioFile });
  };

  const play = async () => {
    console.log('state.loaded', state.loaded);

    try {
      const auxState = { ...state };
      auxState.paused = false;

      setState(auxState);

      console.log(state.audioFile);
      console.log(JSON.stringify(state.audioFile));

      const auxSound = new Sound(state.audioFile, '', (error: any) => {
        if (error) {
          console.log('failed to load the file', error);
        } else {
          auxSound.play(() => {
            setState({ ...auxState, paused: true });
          });
        }
      });

      Sound.setCategory('Playback');
    } catch (e) {
      console.log(e);
    }
  };

  const pause = () => {
    const auxState = { ...state, paused: true };
    setState(auxState);
  };

  useEffect(() => {
    checkPermission();

    const options: Options = {
      sampleRate: 16000,
      channels: 1,
      bitsPerSample: 16,
      wavFile: 'novo.wav',
    };

    ScreenAudioRecorder.init(options);
  }, [checkPermission]);

  return (
    <View style={styles.container}>
      <View style={styles.row}>
        <Button onPress={start} title="Record" disabled={state.recording} />
        <Button onPress={stop} title="Stop" disabled={!state.recording} />
        {state.paused ? (
          <Button onPress={play} title="Play" disabled={!state.audioFile} />
        ) : (
          <Button onPress={pause} title="Pause" disabled={!state.audioFile} />
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
