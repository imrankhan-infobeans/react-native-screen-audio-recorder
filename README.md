# react-native-screen-audio-recorder

React Native library to record audio played by device

## Installation

```sh
npm install react-native-screen-audio-recorder
```

## Usage

```ts
import ScreenAudioRecorder, { Options } from 'react-native-screen-audio-recorder';

const options: Options = {
  sampleRate: 16000,    // default 44100
  channels: 1,          // 1 or 2, default 1
  bitsPerSample: 16,    // 8 or 16, default 16
  audioSource: 6,       // android only (see below)
  fileName: 'test.wav'  // default 'audio.wav'
  fromMic: false,       // should record audio from microphone instead of device playback
};

ScreenAudioRecorder.init(options);

ScreenAudioRecorder.start();

ScreenAudioRecorder.on('data', data => {
  // real time base64-encoded audio
});

const audioFile = await AudioRecord.stop();

```

A full code example can be found at https://github.com/Nilsantos/react-native-screen-audio-recorder/blob/master/example/src/App.tsx

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

## Credits/References
The code was developed based on the library [react-native-audio-record](https://github.com/goodatlas/react-native-audio-record)
