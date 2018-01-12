import DeviceInfo from 'react-native-device-info';
import { NativeModules, Dimensions, Platform } from 'react-native';

const getDeviceInfo = () => ({
  deviceModel: DeviceInfo.getModel(),
  devicePlatform: `${DeviceInfo.getSystemName()} ${DeviceInfo.getSystemVersion()}`,
  appVersion: DeviceInfo.getReadableVersion(),
});

const isTablet = Math.min(Dimensions.get('window').width, Dimensions.get('window').height) >= 768;
const isiPhoneX = Platform.OS === 'ios' &&
  (DeviceInfo.getDeviceId() === 'iPhone10,3' ||
   DeviceInfo.getDeviceId() === 'iPhone10,6' ||
   DeviceInfo.getModel() === 'iPhone X');

const deviceAtLeastMajorVersion = (v) => {
  // SystemVersion looks like e.g. '11.0' or '10.1.1': check the first number
  const sysVer = DeviceInfo.getSystemVersion();
  const sysVerMatch = /^([0-9]+).*$/.exec(sysVer);
  const sysVerMajor = parseInt(sysVerMatch[1] || -1);

  return (sysVerMajor >= v);
}

const deviceSupportsARCam = () => {
  // TODO: Eventually should write a bridged method using ARConfiguration.isSupported

  if (Platform.OS !== 'ios') { return false; }
  if (!deviceAtLeastMajorVersion(11)) { return false; }

  const deviceId = DeviceInfo.getDeviceId();

  // Is deviceId === `iPhone${A},${B}` with A >= 8?
  const iPhoneMatch = /iPhone([0-9]+),([0-9]+)/.exec(deviceId);
  if (iPhoneMatch) {
    iPhoneA = parseInt(iPhoneMatch[1] || -1);
    // iPhoneB = parseInt(iPhoneMatch[2] || -1);
    if (iPhoneA >= 8) { return true; }
  }

  // Is deviceId === `iPad${A},${B}` with A >= 6?
  const iPadMatch = /iPad([0-9]+),([0-9]+)/.exec(deviceId);
  if (iPadMatch) {
    iPadA = parseInt(iPadMatch[1] || -1);
    // iPadB = parseInt(iPadMatch[2] || -1);
    if (iPadA >= 6) { return true; }
  }

  return false;
};

export {
  getDeviceInfo,
  isTablet,
  deviceAtLeastMajorVersion,
  deviceSupportsARCam,
  isiPhoneX,
};
