import { NativeModules } from 'react-native';

type ScreenAudioRecorderType = {
  multiply(a: number, b: number): Promise<number>;
};

const { ScreenAudioRecorder } = NativeModules;

export default ScreenAudioRecorder as ScreenAudioRecorderType;
