import React, { Component, PropTypes } from 'react';
import {
  NativeModules,
  requireNativeComponent,
  View,
  AsyncStorage,
  Dimensions,
} from 'react-native';
import CameraLayout from './camera-layout';
import CameraLayoutTablet from './camera-layout-tablet';

import {
  PERSIST_FASTCAM_MODE,
  PERSIST_FLASH_MODE,
  PERSIST_RESOLUTION_MODE,
} from './cccam-enums';

const CameraModule = NativeModules.CCCameraModuleIOS || NativeModules.CCCameraModule;
const isTablet = Math.min(Dimensions.get('window').width, Dimensions.get('window').height) >= 768;

const normalizePhotoOrigin = (photoOrigin) => {
  const validPhotoOrigin = (
    photoOrigin === 'STANDARD_CAMERA' ||
    photoOrigin === 'STANDARD_CAMERA_FASTCAM' ||
    photoOrigin === 'STANDARD_CAMERA_DOCSCAN'
  );
  return validPhotoOrigin ? photoOrigin : 'STANDARD_CAMERA';
};


class CCCamera extends React.Component {

  static constants = {
    FlashMode: CameraModule.FlashMode, // off, on, auto, torch
    CameraMode: CameraModule.CameraMode, // fastcam, photo, scanner
    ResolutionMode: CameraModule.ResolutionMode, // normal, high, super
  };

  constructor(props) {
    super(props);

    this.state = {
      hasPersistedModes: false,
    };
  }

  componentDidMount() {
    console.disableYellowBox = true;
    this._persistCameraModes();
  }

  _persistCameraModes = async () => {
    try {
      const persistedModes = await AsyncStorage.multiGet([PERSIST_FASTCAM_MODE, PERSIST_FLASH_MODE, PERSIST_RESOLUTION_MODE]);
      const nextState = { ...this.state };

      for (const modeKeyValuePair of persistedModes) {
        const persistMode = modeKeyValuePair[0];
        const value = modeKeyValuePair[1];

        switch (persistMode) {
          case PERSIST_FLASH_MODE:
            nextState.flashMode = value ? parseInt(value, 10) : constants.FlashMode.off;
            break;
          case PERSIST_FASTCAM_MODE:
            nextState.cameraMode = value ? parseInt(value, 10) : constants.CameraMode.photo;
            break;
          case PERSIST_RESOLUTION_MODE:
            nextState.resolutionMode = value ? parseInt(value, 10) : constants.ResolutionMode.normal;
            break;
          default: break;
        }
      }

      this.setState({
        ...nextState,
        hasPersistedModes: true,
      });
    } catch (error) {
      console.warn('error persisting modes', error);
      this.setState({
        hasPersistedModes: true,
        flashMode: constants.FlashMode.off,
        cameraMode: constants.CameraMode.photo,
        resolutionMode: constants.ResolutionMode.normal,
      });
    }
  }

  _onClose = (event) => {
    console.log("_onClose called in cccamera.js");
    if (!this.props.onClose) { return; }

    const errmsg = event.nativeEvent.errmsg;
    const button = event.nativeEvent.button;
    this.props.onClose(errmsg, button);
  }

  _onPhotoAccepted = (event) => {
    if (!this.props.onPhotoAccepted) { return; }

    const { filename, imgWidth, imgHeight, photoOrigin } = event.nativeEvent;
    const origin = normalizePhotoOrigin(photoOrigin);

    console.log(`_onPhotoAccepted called in cccamera.js (dims: ${imgWidth}x${imgHeight}, origin: ${origin})`);
    this.props.onPhotoAccepted(filename, [imgWidth, imgHeight], origin);
  }

  _onPhotoTaken = (event) => {
    if (!this.props.onPhotoTaken) { return; }

    const { filename, imgWidth, imgHeight, photoOrigin } = event.nativeEvent;
    const origin = normalizePhotoOrigin(photoOrigin);

    console.log(`_onPhotoTaken called in cccamera.js (dims: ${imgWidth}x${imgHeight}, origin: ${origin})`);
    this.props.onPhotoTaken(filename, [imgWidth, imgHeight], origin);
  }

  _onAuxModeClicked = () => {
    console.log('_onAuxModeClicked called in cccamera.js');
    if (!this.props.onAuxModeClicked) { return; }

    this.props.onAuxModeClicked();
  }

  render() {
    const { hideCameraLayout } = this.props;
    const CCCameraLayout = isTablet ? CameraLayoutTablet : CameraLayout;

    return (
      <RNCCCamera
        {...this.props}
        hideNativeUI

        onClose={this._onClose}
        onPhotoAccepted={this._onPhotoAccepted}
        onPhotoTaken={this._onPhotoTaken}
        onAuxModeClicked={this._onAuxModeClicked}

        flashMode={this.state.flashMode}
        cameraMode={this.state.cameraMode}
        resolutionMode={this.state.resolutionMode}
      >
        {
          !hideCameraLayout &&
            <CameraLayout
              cameraConstants={constants}
              cameraState={{ ...this.state }}
              setCameraState={(nextState) => this.setState(nextState)}
              onClose={(errmsg, button) => {
                if (!this.props.onClose) { return; }
                this.props.onClose(errmsg, button);
              }}

              captureButtonPress={() => {
                CameraModule.capture();
                this.props.captureButtonPress();
              }}
              flipCamera={() => { CameraModule.flipCamera(); }}

              projectName={this.props.projectName}

              cameraTrayData={this.props.cameraTrayData}
              cameraTrayVisible={this.props.cameraTrayVisible}
              onSelectTrayItem={this.props.onSelectTrayItem}
              setCameraTrayVisible={this.props.setCameraTrayVisible}

              arModePress={this.props.arModePress}
              baModePress={this.props.baModePress}
            />
        }
        {this.props.children}
      </RNCCCamera>
    );
  }
}

CCCamera.propTypes = {
  storagePath: PropTypes.string,
  projectName: PropTypes.string,
  projectAddress: PropTypes.string,

  exifLat: PropTypes.number,
  exifLon: PropTypes.number,
  exifLocTimestamp: PropTypes.number,

  auxModeCaption: PropTypes.string,
  onAuxModeClicked: PropTypes.func,

  hideNativeUI: PropTypes.bool,
  hideCameraLayout: PropTypes.bool,

  onClose: PropTypes.func,
  onPhotoAccepted: PropTypes.func,
  onPhotoTaken: PropTypes.func,
  ...View.propTypes,

  flashMode: PropTypes.number,
  cameraMode: PropTypes.number,
  resolutionMode: PropTypes.number,

  arModePress: PropTypes.func,
  baModePress: PropTypes.func,
  captureButtonPress: PropTypes.func,

  cameraTrayData: PropTypes.array,
  cameraTrayVisible: PropTypes.bool,
  onSelectTrayItem: PropTypes.func,
  setCameraTrayVisible: PropTypes.func,
};

CCCamera.defaultProps = {
  projectName: '',
  projectAddress: '',

  exifLat: 0,
  exifLon: 0,
  exifLocTimestamp: 0,

  auxModeCaption: '',
  onAuxModeClicked: () => {},

  hideNativeUI: false,
  hideCameraLayout: false,

  arModePress: () => {},
  baModePress: () => {},
  captureButtonPress: () => {},

  cameraTrayData: [],
  cameraTrayVisible: false,
  onSelectTrayItem: () => {},
  setCameraTrayVisible: () => {},
};

export const constants = CCCamera.constants;


const RNCCCamera = requireNativeComponent('CompanyCamCamera', CCCamera);

export default CCCamera;
