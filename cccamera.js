import React, { PropTypes } from 'react';
import { NativeModules, requireNativeComponent, View, StyleSheet, Text } from 'react-native';
import CameraLayout from './camera-layout';
import CameraLayoutTablet from './camera-layout-tablet';
import { isTablet, deviceSupportsARCam } from './device-info-helper';

const CameraModule = NativeModules.CCCameraModuleIOS || NativeModules.CCCameraModule;

const normalizePhotoOrigin = (photoOrigin) => {
  const validPhotoOrigin =
    photoOrigin === 'STANDARD_CAMERA' ||
    photoOrigin === 'STANDARD_CAMERA_FASTCAM' ||
    photoOrigin === 'STANDARD_CAMERA_DOCSCAN';
  return validPhotoOrigin ? photoOrigin : 'STANDARD_CAMERA';
};

const styles = StyleSheet.create({
  toast: {
    alignSelf: 'center',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.7)',
    borderRadius: 8,
    padding: 24,
    width: 280,
  },
  toastTitle: {
    textAlign: 'center',
    fontSize: 20,
    color: 'white',
    fontWeight: 'bold',
  },
  toastMessage: {
    textAlign: 'center',
    color: 'white',
  },
});

class CCCamera extends React.Component {
  static constants = {
    FlashMode: CameraModule.FlashMode, // off, on, auto, torch
    CameraMode: CameraModule.CameraMode, // fastcam, photo, scanner
    ResolutionMode: CameraModule.ResolutionMode, // normal, high, super
  };

  constructor(props) {
    super(props);

    this.toastTimer = null;

    this.state = {
      hasFlash: false,
      showToast: false,
      toastTitleText: '',
      toastMessageText: '',
    };
  }

  componentDidMount() {
    console.disableYellowBox = true;
  }

  _displayToast = (title, message) => {
    this.setState(
      {
        showToast: true,
        toastTitleText: title,
        toastMessageText: message,
      },
      () => {
        clearTimeout(this.toastTimer);
        this.toastTimer = setTimeout(() => {
          this.setState({ showToast: false });
        }, 1750);
      }
    );
  };

  _renderToast = () => {
    const { showToast, toastTitleText, toastMessageText } = this.state;

    if (!showToast) {
      return null;
    }

    return (
      <View style={styles.toast}>
        <Text style={styles.toastTitle}>{toastTitleText}</Text>
        {toastMessageText ? <Text style={styles.toastMessage}>{toastMessageText}</Text> : null}
      </View>
    );
  };

  _setCameraMode = (nextMode) => {
    const nextOpts = { ...this.props.cameraOpts };

    if (this.props.cameraOpts.cameraMode !== nextMode) {
      nextOpts.cameraMode = nextMode;

      switch (nextMode) {
        case constants.CameraMode.photo:
        case constants.CameraMode.fastcam:
          if (this.props.cameraOpts.cameraMode === constants.CameraMode.scanner) {
            this._displayToast('Take Photos', '');
          }
          break;
        case constants.CameraMode.scanner:
          this._displayToast('Scan Document', '');
          break;
        default:
          break;
      }
    }
    this.props.updateCameraOpts(nextOpts);
  };

  // currently we only allow two modes, torch and off, hence the name toggle.
  // in the future it could be changed to setFlashMode if we allow multiple types
  _toggleFlashMode = () => {
    const nextOpts = { ...this.props.cameraOpts };

    switch (this.props.cameraOpts.flashMode) {
      case constants.FlashMode.off:
        nextOpts.flashMode = constants.FlashMode.torch;
        this._displayToast('Flashlight Enabled', '');
        break;
      case constants.FlashMode.torch:
        nextOpts.flashMode = constants.FlashMode.off;
        this._displayToast('Flashlight Disabled', '');
        break;
      default:
        break;
    }

    this.props.updateCameraOpts(nextOpts);
  };

  _setResolutionMode = (nextModeString) => {
    const nextOpts = { ...this.props.cameraOpts };
    const nextMode = constants.ResolutionMode[nextModeString];

    if (this.props.cameraOpts.resolutionMode !== nextMode) {
      nextOpts.resolutionMode = nextMode;
      this.props.updateCameraOpts(nextOpts);
    }
  };

  _onFlashAvailabilityChange = (event) => {
    const { hasFlash } = event.nativeEvent;
    console.log(`_onFlashAvailabilityChange called in cccamera.js (${hasFlash})`);

    this.setState({ hasFlash });
  };

  _onClose = (event) => {
    console.log('_onClose called in cccamera.js');
    if (!this.props.onClose) {
      return;
    }

    const errmsg = event.nativeEvent.errmsg;
    const button = event.nativeEvent.button;
    this.props.onClose(errmsg, button);
  };

  _onPhotoAccepted = (event) => {
    if (!this.props.onPhotoAccepted) {
      return;
    }

    const { filename, imgWidth, imgHeight, photoOrigin } = event.nativeEvent;
    const origin = normalizePhotoOrigin(photoOrigin);

    console.log(
      `_onPhotoAccepted called in cccamera.js (dims: ${imgWidth}x${imgHeight}, origin: ${origin})`
    );
    this.props.onPhotoAccepted(filename, [imgWidth, imgHeight], origin);
  };

  _onPhotoTaken = (event) => {
    if (!this.props.onPhotoTaken) {
      return;
    }

    const { filename, imgWidth, imgHeight, photoOrigin } = event.nativeEvent;
    const origin = normalizePhotoOrigin(photoOrigin);

    console.log(
      `_onPhotoTaken called in cccamera.js (dims: ${imgWidth}x${imgHeight}, origin: ${origin})`
    );
    this.props.onPhotoTaken(filename, [imgWidth, imgHeight], origin);
  };

  _onAuxModeClicked = () => {
    console.log('_onAuxModeClicked called in cccamera.js');
    if (!this.props.onAuxModeClicked) {
      return;
    }

    this.props.onAuxModeClicked();
  };

  render() {
    const { cameraOpts } = this.props;
    const CCCameraLayout = isTablet ? CameraLayoutTablet : CameraLayout;

    return (
      <RNCCCamera
        {...this.props}
        hideNativeUI
        onClose={this._onClose}
        onPhotoAccepted={this._onPhotoAccepted}
        onPhotoTaken={this._onPhotoTaken}
        onFlashAvailabilityChange={this._onFlashAvailabilityChange}
        flashMode={cameraOpts.flashMode}
        cameraMode={cameraOpts.cameraMode}
        resolutionMode={cameraOpts.resolutionMode}
      >
        {!this.props.hideCameraLayout && (
          <CCCameraLayout
            cameraOpts={cameraOpts}
            cameraConstants={constants}
            onClose={(errmsg, button) => {
              if (!this.props.onClose) {
                return;
              }
              this.props.onClose(errmsg, button);
            }}
            flipCamera={() => CameraModule.flipCamera()}
            hasFlash={this.state.hasFlash}
            projectName={cameraOpts.projectName}
            orientation={cameraOpts.orientation}
            deviceSupportsARCam={deviceSupportsARCam()}
            cameraTrayData={this.props.cameraTrayData}
            cameraTrayVisible={this.props.cameraTrayVisible}
            onSelectTrayItem={this.props.onSelectTrayItem}
            setCameraTrayVisible={this.props.setCameraTrayVisible}
            arModePress={this.props.arModePress}
            baModePress={this.props.baModePress}
            captureButtonPress={() => {
              CameraModule.capture();
              this.props.captureButtonPress();
            }}
            setCameraMode={this._setCameraMode}
            setResolutionMode={this._setResolutionMode}
            toggleFlashMode={this._toggleFlashMode}
            renderToast={this._renderToast}
            settingsComponent={this.props.settingsComponent}
          />
        )}
        {this.props.children}
      </RNCCCamera>
    );
  }
}

CCCamera.propTypes = {
  storagePath: PropTypes.string,
  hideNativeUI: PropTypes.bool,
  hideCameraLayout: PropTypes.bool,

  cameraOpts: PropTypes.shape({
    projectName: PropTypes.string,
    projectAddress: PropTypes.string,
    exifLat: PropTypes.number,
    exifLon: PropTypes.number,
    exifLocTimestamp: PropTypes.number,
    hideCameraLayout: PropTypes.bool,
    orientation: PropTypes.number,
    flashMode: PropTypes.number,
    cameraMode: PropTypes.number,
    resolutionMode: PropTypes.number,
  }),

  onClose: PropTypes.func,
  onPhotoAccepted: PropTypes.func,
  onPhotoTaken: PropTypes.func,
  onFlashAvailabilityChange: PropTypes.func,
  ...View.propTypes,

  arModePress: PropTypes.func,
  baModePress: PropTypes.func,
  captureButtonPress: PropTypes.func,

  cameraTrayData: PropTypes.array,
  cameraTrayVisible: PropTypes.bool,
  onSelectTrayItem: PropTypes.func,
  setCameraTrayVisible: PropTypes.func,

  settingsComponent: PropTypes.func,

  // duplicated from above to make RN happy. not happy myself
  projectName: PropTypes.string,
  projectAddress: PropTypes.string,
  exifLat: PropTypes.number,
  exifLon: PropTypes.number,
  exifLocTimestamp: PropTypes.number,
  orientation: PropTypes.number,
  flashMode: PropTypes.number,
  cameraMode: PropTypes.number,
  resolutionMode: PropTypes.number,
};

CCCamera.defaultProps = {
  hideNativeUI: true,

  cameraOpts: {
    projectName: '',
    projectAddress: '',
    exifLat: 0,
    exifLon: 0,
    exifLocTimestamp: 0,
    hideCameraLayout: false,
    orientation: 0,
    flashMode: CameraModule.FlashMode.off,
    cameraMode: CameraModule.CameraMode.fastcam,
    resolutionMode: CameraModule.ResolutionMode.normal,
  },

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
