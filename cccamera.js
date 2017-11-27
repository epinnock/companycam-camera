import React, { Component, PropTypes } from 'react';
import {
  NativeModules,
  requireNativeComponent,
  View,
} from 'react-native';
import CameraLayout from './camera-layout';

const CameraModule = NativeModules.CCCameraModuleIOS || NativeModules.CCCameraModule;

function convertNativeProps(props) {
  const newProps = { ...props };

  if (typeof props.flashMode === 'string') {
    newProps.flashMode = CCCamera.constants.FlashMode[props.flashMode];
  }

  return newProps;
}

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

  constructor(props){
    super(props);

    // TODO: Implement using Settings from RN to persist modes
    this.state = {
      flashMode: constants.FlashMode.off,
      cameraMode: constants.CameraMode.photo,
      resolutionMode: constants.ResolutionMode.normal,
    };
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
    console.log(this.props);
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
              cameraState={{...this.state}}
              setCameraState={(nextState) => this.setState(nextState)}
              onClose={(e) => this._onClose(e)}

              captureButtonPress={() => { CameraModule.capture(); }}
              flipCamera={() => { CameraModule.flipCamera(); }}
            />
        }
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
};

export const constants = CCCamera.constants;


const RNCCCamera = requireNativeComponent('CompanyCamCamera', CCCamera);

export default CCCamera;
