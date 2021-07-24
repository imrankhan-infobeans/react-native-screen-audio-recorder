import { NativeModules } from 'react-native';

type ScreenAudioRecorderType = {
  init: (options: Options) => void;
  start: () => void;
  stop: () => Promise<string>;
  on: (event: 'data', callback: (data: string) => void) => void;
};

export interface Options {
  sampleRate: number;
  /**
   * - `1 | 2`
   */
  channels: number;
  /**
   * - `8 | 16`
   */
  bitsPerSample: number;
  /**
   * - `6`
   */
  fileName: string;

  fromMic?: boolean;
}

const { ScreenAudioRecorder } = NativeModules;

export default ScreenAudioRecorder as ScreenAudioRecorderType;
