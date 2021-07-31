import { NativeModules, NativeEventEmitter } from 'react-native';

export interface Options {
  /**
   * Sample rate in hz. Default = 44100
   */
  sampleRate: number;
  /**
   * Channels, 1 = MONO, 2 = STEREO. Default = 1
   * - `1 | 2`
   */
  channels: number;
  /**
   * Bits per sample. Default = 16
   * - `8 | 16`
   */
  bitsPerSample: number;
  /**
   * File name. Default = "audio.wav"
   */
  fileName?: string;
  /**
   * Record audio from microphone instead of device. Default = false
   */
  fromMic?: boolean;
  /**
   * Save recorded audio. Default = false
   */
  saveFile?: boolean;
}

type ScreenAudioRecorderType = {
  init: (options: Options) => void;
  start: () => void;
  stop: () => Promise<string>;
  on: (event: 'data', callback: (data: string) => void) => void;
};

const { ScreenAudioRecorder } = NativeModules;
const EventEmitter = new NativeEventEmitter(ScreenAudioRecorder);
const eventsMap = {
  data: 'data',
};

const AudioRecord: ScreenAudioRecorderType = {
  init: (options: Options) => ScreenAudioRecorder.init(options),
  start: () => ScreenAudioRecorder.start(),
  stop: () => ScreenAudioRecorder.stop(),
  on: (event, callback) => {
    const nativeEvent = eventsMap[event];

    if (!nativeEvent) {
      throw new Error('Invalid event');
    }

    EventEmitter.removeAllListeners(nativeEvent);
    return EventEmitter.addListener(nativeEvent, callback);
  },
};

export default AudioRecord;
