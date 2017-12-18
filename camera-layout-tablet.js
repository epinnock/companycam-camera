import React, { Component, PropTypes } from 'react';

import {
  View, Text, TouchableOpacity, StyleSheet,
  Platform, AsyncStorage, Animated, Easing,
  Dimensions, DeviceEventEmitter,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
import DeviceInfo from 'react-native-device-info';
import styled from 'styled-components/native';
import CameraSettings from './camera-settings';
import { invert } from 'lodash';

import {
  PERSIST_FASTCAM_MODE,
  PERSIST_FLASH_MODE,
  PERSIST_RESOLUTION_MODE,
} from './cccam-enums';

// TODO remove what we dont use for icons...
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import FeatherIcon from 'react-native-vector-icons/Feather';

const CAMERA_MODE_PHOTO = 'photo-mode';
const CAMERA_MODE_SCAN = 'scan-mode';

const FASTCAM_ON_ICON = 'burst-mode'; // MaterialIcon set
const FASTCAM_OFF_ICON = 'photo'; // MaterialIcon set
const FLASH_ON_ICON = 'flashlight'; // MaterialCommunityIcon set
const FLASH_OFF_ICON = 'flashlight-off'; // MaterialCommunityIcon set

const ModeIndicator = styled.View`
  margin-top: 4;
  margin-bottom: 4;
  height: 4;
  width: ${props => props.isCurrentMode ? '16' : '0'};
  border-radius: 2;
  background-color: #FFB300;
`;

const ModeTitle = styled.Text`
  background-color: transparent;

  ${''/* with color.... */}
  color: ${props => props.isCurrentMode ? '#FFB300' : 'white'};
`;

const styles = StyleSheet.create({
  cameraUIContainer: {
    flex: 1,
    zIndex: 1,
    justifyContent: 'space-between',
    width: '100%',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    height: 56,
    marginTop: 0,
  },
  headerTitle: {
    color: 'white',
    backgroundColor: 'transparent',
    textAlign: 'center',
    flex: 1,
  },
  headerTitleButton: {
    flex: 1,
    flexDirection: 'row',
    paddingHorizontal: 8,
  },
  uiButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 44,
    height: 44,
    margin: 8,
    borderRadius: 22,
    backgroundColor: 'blue',
  },
  captureButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 72,
    height: 72,
    borderRadius: 36,
    borderWidth: 4,
    borderColor: 'red',
  },
  footer: {
    flex: 1,
    backgroundColor: 'green',
    borderBottomColor: 'red',
    borderBottomWidth: 1,
    borderTopColor: 'red',
    borderTopWidth: 1,
    maxHeight: '50%',
  },
  captureContainer: {
    alignItems: 'center',
    justifyContent: 'space-around',
    backgroundColor: 'orange',
  },
  modeContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'palevioletred',
  },
  modeButton: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
  },
  toast: {
    alignSelf: 'center',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.7)',
    borderRadius: 8,
    width: 280,
    padding: 24,
  },
  toastTitle: {
    textAlign: 'center',
    fontSize: 24,
    color: 'white',
    marginBottom: 8,
  },
  toastMessage: {
    textAlign: 'center',
    color: 'white',
  },

  // TODO styles for zoom level
  // zoomContainer: {
  //   alignItems: 'center',
  //   justifyContent: 'center',
  //   marginBottom: 8,
  // },
  // zoomButton: {
  //   alignItems: 'center',
  //   justifyContent: 'center',
  //   width: 40,
  //   height: 40,
  //   borderRadius: 20,
  //   borderWidth: 1,
  //   borderColor: 'white',
  // },
  // zoomText: {
  //   backgroundColor: 'transparent',
  //   color: 'white',
  // },

  // TODO delete if we dont use the triangle indicator
  // modeSelectTriangle: {
  //   marginTop: 4,
  //   width: 0,
  //   height: 0,
  //   backgroundColor: 'transparent',
  //   borderStyle: 'solid',
  //   borderLeftWidth: 6,
  //   borderRightWidth: 6,
  //   borderBottomWidth: 6,
  //   borderLeftColor: 'transparent',
  //   borderRightColor: 'transparent',
  //   borderBottomColor: 'white',
  // },
});

class CameraLayout extends Component {

  constructor(props) {
    super(props);

    this.toastTimer = null;

    this.state = {
      author: 'Jared Goertzen',
      showToast: false,
      toastTitleText: '',
      toastMessageText: '',
      showSettings: false,
      screenFlashOpacity: new Animated.Value(0),
      orientationDegrees: new Animated.Value(0),
      swapHeaderButtons: false,
      swapCameraUI: false,
      dynamicFooterStyles: {
        alignItems: 'flex-end',
        flexDirection: 'column',
        justifyContent: 'space-between',
      },
      dynamicCaptureContainerStyles: {
        alignItems: 'center',
        justifyContent: 'space-around',
      },
    };
  }

  componentDidMount() {
    DeviceEventEmitter.addListener('CCCameraOrientationChange', this._orientationChange);
  }

  componentWillUnmount() {
    DeviceEventEmitter.addListener('CCCameraOrientationChange', this._orientationChange);
  }

  _orientationChange = (changeEvent) => {
      const constants = {...this.props.cameraConstants};
      const { orientation } = changeEvent;

      let nextDegree = 0;
      let swapHeaderButtons = false;
      let swapCameraUI = false;
      let dynamicFooterStyles = {
        alignItems: 'flex-end',
        flexDirection: 'column',
        justifyContent: 'center',
      };
      let dynamicCaptureContainerStyles = {
        alignItems: 'center',
        // justifyContent: 'space-around',
      };

      switch (orientation) {
        case constants.Orientation.portrait:
          nextDegree = 0;
          dynamicCaptureContainerStyles = {
            paddingHorizontal: 32,
          };
          break;
        case constants.Orientation.landscapeleft:
          nextDegree = 90;
          swapHeaderButtons = true;
          dynamicFooterStyles = {
            alignItems: 'flex-end',
            flexDirection: 'row',
            justifyContent: 'center',
          };
          dynamicCaptureContainerStyles = {
            flexDirection: 'row-reverse',
            paddingVertical: 32,
          };
          break;
        case constants.Orientation.landscaperight:
          nextDegree = -90;
          dynamicFooterStyles = {
            alignItems: 'flex-end',
            flexDirection: 'row-reverse',
            justifyContent: 'center',
          };
          dynamicCaptureContainerStyles = {
            flexDirection: 'row',
            paddingVertical: 32,
          };
          break;
        case constants.Orientation.portraitupsidedown:
          nextDegree = 180;
          swapCameraUI = true;
          dynamicCaptureContainerStyles = {
            flexDirection: 'column-reverse',
            paddingHorizontal: 32,
          };
          break;
        default: break;
      }

      Animated.timing(this.state.orientationDegrees, {
        toValue: nextDegree,
        duration: 100,
      }).start();

      this.setState({
        swapHeaderButtons, swapCameraUI,
        dynamicCaptureContainerStyles, dynamicFooterStyles
      });
    }

  setCameraMode = async (nextMode) => {
    const constants = {...this.props.cameraConstants};
    const nextState = { ...this.props.cameraState };

    if (this.props.cameraState.cameraMode !== nextMode) {
      nextState.cameraMode = nextMode;

      switch (nextMode) {
        case constants.CameraMode.photo:
          if (this.props.cameraState.cameraMode === constants.CameraMode.scanner) {
            this.displayToast('Photo mode', '');
          } else {
            this.displayToast('Fastcam disabled', '');
          }
          break;
        case constants.CameraMode.scanner:
          this.displayToast('Scanner mode', '');
          break;
        case constants.CameraMode.fastcam:
          this.displayToast('Fastcam enabled', '');
          break;
        default: break;
      }

      // if we aren't in scanner mode, store mode to be persisted later
      if (nextMode !== constants.CameraMode.scanner) {
        try {
          await AsyncStorage.setItem(PERSIST_FASTCAM_MODE, nextMode.toString());
        } catch (error) {
          console.warn('error storing camera mode', error);
        }
      }
    }

    this.props.setCameraState(nextState);
    this.forceUpdate(); // since the state is in the parent, we need this to rerender icons
  }

  setResolutionMode = async (nextModeString) => {
    const constants = {...this.props.cameraConstants};
    const nextState = { ...this.props.cameraState };

    const nextMode = constants.ResolutionMode[nextModeString];

    if (this.props.cameraState.resolutionMode !== nextMode) {
      nextState.resolutionMode = nextMode;
      this.props.setCameraState(nextState);

      try {
        await AsyncStorage.setItem(PERSIST_RESOLUTION_MODE, nextMode.toString());
      } catch (error) {
        console.warn('error storing resolution mode', error);
      }
    }
  }

  toggleFlashMode = async () => {
    const constants = {...this.props.cameraConstants};
    const nextState = { ...this.props.cameraState };

    switch (this.props.cameraState.flashMode) {
      case constants.FlashMode.off:
        nextState.flashMode = constants.FlashMode.torch;
        this.displayToast('Flash enabled', 'Flash is now on');
        break;
      case constants.FlashMode.torch:
        nextState.flashMode = constants.FlashMode.off;
        this.displayToast('Flash disabled', 'Flash is now off');
        break;
      default: break;
    }

    try {
      await AsyncStorage.setItem(PERSIST_FLASH_MODE, nextState.flashMode.toString());
    } catch (error) {
      console.warn('error storing flash mode', error);
    }

    this.props.setCameraState(nextState);
    this.forceUpdate(); // since the state is in the parent, we need this to rerender icons
  }

  displayToast = (title, message) => {
    this.setState({
      showToast: true,
      toastTitleText: title,
      toastMessageText: message,
    }, () => {
      clearTimeout(this.toastTimer);
      this.toastTimer = setTimeout(() => {
        this.setState({ showToast: false });
      }, 1750);
    });
  }

  doFlashAnimation = () => {
    Animated.sequence([
      Animated.timing(this.state.screenFlashOpacity, {
        toValue: 1,
        duration: 120,
        // easing: Easing.cubic,
      }),
      Animated.timing(this.state.screenFlashOpacity, {
        toValue: 0,
        duration: 120,
        // easing: Easing.cubic,
      }),
    ]).start();
  }

  render() {
    const constants = {...this.props.cameraConstants};
    const { flashMode, cameraMode, resolutionMode } = this.props.cameraState;

    const TorchIsOn = flashMode === constants.FlashMode.torch;
    const PrimaryModeIsScan = cameraMode === constants.CameraMode.scanner;

    const invertedResolutionModes = invert(constants.ResolutionMode);

    return (
      <View
        style={[styles.cameraUIContainer, {
          flexDirection: this.state.swapCameraUI ? 'column-reverse' : 'column',
        }]}
      >
        <View
          style={[styles.header, {
            flexDirection: this.state.swapHeaderButtons ? 'row-reverse' : 'row',
            transform: [{ rotate: this.state.swapCameraUI ? '180deg' : '0deg'}],
          }]}
        >
          <TouchableOpacity
            onPress={(e) => this.props.onClose(e)}
            style={styles.uiButton}
          >
            <MaterialIcon name="close" size={24} color="white" />
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => {}}
            style={styles.headerTitleButton}
          >
            <Text
              numberOfLines={1}
              style={styles.headerTitle}
            >
              {this.props.projectName || ''}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => this.setState({ showSettings: true })}
            style={styles.uiButton}
          >
            <MaterialIcon name="settings" size={24} color="white" />
          </TouchableOpacity>
        </View>

        {
          this.state.showToast ?
            <View style={styles.toast}>
              <Text style={styles.toastTitle}>{this.state.toastTitleText}</Text>
              <Text style={styles.toastMessage}>{this.state.toastMessageText}</Text>
            </View> : null
        }

        {/* flash animation */}
        <Animated.View
          pointerEvents="none"
          style={{
            position: 'absolute',
            backgroundColor: 'white',
            height: '100%',
            width: '100%',
            opacity: this.state.screenFlashOpacity.interpolate({
              inputRange: [0, 1],
              outputRange: [0, 1],
              extrapolate: 'clamp',
            }),
          }}
        />

        <View
          style={[styles.footer, this.state.dynamicFooterStyles]}
        >

          {/*
            TODO eventually would be nice to track if zoomed in...
            + pinch to zoom would update the magnification (ie 2.8x etc)
            + would be cool if you had two lenses you could tap on icon
              to switch them
          */}
          {/* <View style={styles.zoomContainer}>
            <TouchableOpacity
              style={styles.zoomButton}
            >
              <Text style={styles.zoomText}>1x</Text>
            </TouchableOpacity>
          </View> */}

          <View style={[styles.captureContainer, this.state.dynamicCaptureContainerStyles]}>

            {/* Preview tray */}
            {
              !PrimaryModeIsScan &&
                <TouchableOpacity onPress={() => {}}>
                  <Animated.View
                    style={[styles.uiButton, {
                      transform: [{
                        rotate: this.state.orientationDegrees.interpolate({
                          inputRange: [0, 1],
                          outputRange: ['0deg', '1deg'],
                        }),
                      }]
                    }]}
                  >
                    <MaterialIcon name="crop-portrait" size={32} color="white" />
                  </Animated.View>
                </TouchableOpacity>
            }

            {/* Fast cam toggle button */}
            {
              !PrimaryModeIsScan &&
                <TouchableOpacity
                  onPress={() => {
                    if (cameraMode === constants.CameraMode.fastcam) {
                      this.setCameraMode(constants.CameraMode.photo);
                    } else {
                      this.setCameraMode(constants.CameraMode.fastcam);
                    }
                  }}
                >
                  <Animated.View
                    style={[styles.uiButton, {
                      transform: [{
                        rotate: this.state.orientationDegrees.interpolate({
                          inputRange: [0, 1],
                          outputRange: ['0deg', '1deg'],
                        }),
                      }]
                    }]}
                  >
                    <MaterialIcon
                      name={cameraMode === constants.CameraMode.fastcam ? FASTCAM_ON_ICON : FASTCAM_OFF_ICON}
                      size={24}
                      color="white"
                    />
                  </Animated.View>
                </TouchableOpacity>
            }

            {/* Capture button */}
            {
              !PrimaryModeIsScan &&
                <TouchableOpacity
                  onPress={() => {
                    this.doFlashAnimation();
                    this.props.captureButtonPress();
                  }}
                  style={styles.captureButton}
                />
            }

            {/* Front/back camera button */}
            {
              !PrimaryModeIsScan &&
                <TouchableOpacity onPress={() => { this.props.flipCamera(); }}>
                  <Animated.View
                    style={[styles.uiButton, {
                      transform: [{
                        rotate: this.state.orientationDegrees.interpolate({
                          inputRange: [0, 1],
                          outputRange: ['0deg', '1deg'],
                        }),
                      }]
                    }]}
                  >
                    <FeatherIcon name="repeat" size={24} color="white" />
                  </Animated.View>
                </TouchableOpacity>
            }

            {/* Magic invisible view for when scanner mode is active */}
            {/* {
              PrimaryModeIsScan &&
                <View style={[styles.captureButton, { opacity: 0 }]} />
            } */}

            {/* Flash mode button */}
            <TouchableOpacity onPress={() => {this.toggleFlashMode(); }}>
              <Animated.View
                style={[styles.uiButton, {
                  transform: [{
                    rotate: this.state.orientationDegrees.interpolate({
                      inputRange: [0, 1],
                      outputRange: ['0deg', '1deg'],
                    }),
                  }]
                }]}
              >
                <MaterialCommunityIcon
                  name={TorchIsOn ? FLASH_ON_ICON : FLASH_OFF_ICON}
                  size={24}
                  color="white"
                />
              </Animated.View>
            </TouchableOpacity>
          </View>

          <View style={styles.modeContainer}>

            {/* Photo mode button */}
            <TouchableOpacity
              onPress={() => this.setCameraMode(constants.CameraMode.photo)}
              style={styles.modeButton}
            >
              <ModeTitle isCurrentMode={!PrimaryModeIsScan}>
                PHOTO
              </ModeTitle>
              {/* <ModeIndicator isCurrentMode={!PrimaryModeIsScan}/> */}
            </TouchableOpacity>

            {/* TODO video video video... */}
            {/* <TouchableOpacity
              onPress={() => {}}
              style={styles.modeButton}
            >
              <ModeTitle>VIDEO</ModeTitle>
              <ModeIndicator />
            </TouchableOpacity> */}

            {/* Scanner mode button */}
            <TouchableOpacity
              onPress={() => this.setCameraMode(constants.CameraMode.scanner)}
              style={styles.modeButton}
            >
              <ModeTitle isCurrentMode={PrimaryModeIsScan}>
                SCAN
              </ModeTitle>
              {/* <ModeIndicator isCurrentMode={PrimaryModeIsScan}/> */}
            </TouchableOpacity>

            {/* AR mode button */}
            <TouchableOpacity
              onPress={() => this.props.arModePress()}
              style={styles.modeButton}
            >
              <ModeTitle>AR</ModeTitle>
              {/* <ModeIndicator /> */}
            </TouchableOpacity>

            {/* Before after mode button */}
            <TouchableOpacity
              onPress={() => this.props.baModePress()}
              style={styles.modeButton}
            >
              <ModeTitle>B/A</ModeTitle>
              {/* <ModeIndicator /> */}
            </TouchableOpacity>
          </View>

        </View>

        {
          this.state.showSettings &&
            <View style={{ zIndex: 2, position: 'absolute', top: 0, left: 0, height: '100%', width: '100%' }}>
              <CameraSettings
                resolutionModeString={invertedResolutionModes[this.props.cameraState.resolutionMode]}
                setResolutionMode={(mode) => this.setResolutionMode(mode)}
                closeSelf={() => this.setState({ showSettings: false })}
              />
            </View>
        }

      </View>
    );
  }
}

CameraLayout.propTypes = {
  cameraConstants: PropTypes.object,
  cameraState: PropTypes.object,
  setCameraState: PropTypes.func,

  captureButtonPress: PropTypes.func,
  flipCamera: PropTypes.func,
  onClose: PropTypes.func,

  projectName: PropTypes.string,
};

export default CameraLayout;
