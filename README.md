# react-native-screen-audio-recorder

React Native library to record audio played by device

Record audio played by the device is available starting on Android 10 (Q = 29). For older Android versions the library will record microphone audio.

## Installation

```sh
npm install react-native-screen-audio-recorder
```

## Usage

```ts
import ScreenAudioRecorder, { Options } from 'react-native-screen-audio-recorder';

const options: Options = {
  sampleRate: 16000,
  channels: 1,
  bitsPerSample: 16,
  fileName: 'novo.wav',
  fromMic: true,
  saveFile: true,
};

ScreenAudioRecorder.init(options);

ScreenAudioRecorder.start();

ScreenAudioRecorder.on('data', data => {
  // real time base64-encoded audio
});

const audioFile = await AudioRecord.stop();

```

A full code example can be found at https://github.com/Nilsantos/react-native-screen-audio-recorder/blob/master/example/src/App.tsx


## Options 

| Name | Description | Default |
|------|-----------------------------------|-----------|
|sampleRate| Sample Rate in hz. | 44100 |
|channels| Channels, 1 = MONO, 2 = STEREO. | 1 |
|bitsPerSample| Bits per sample. | 16 |
|fileName| Output file name. (Don't forget ".wav") | audio.wav |
|fromMic| Record audio from microphone instead of device. For android before 10 even if the option is true, the audio will be captured from the microphone, because android doesn't support. | false |
|saveFile | The captured audio must be recorded. | false |

## Contributors

<table>
  <tr>
    <td align="center"><a href="https://github.com/Mdiaas"><img src="https://avatars.githubusercontent.com/u/49025512?v=4" width="100px;" alt="Mdiaas"/><br><sub><b>Mdiaas</b></sub></a></td>
  </tr>
</table>

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

## Credits/References
The code was developed based on the library [react-native-audio-record](https://github.com/goodatlas/react-native-audio-record)
