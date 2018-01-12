import React, { Component, PropTypes } from 'react';

import {
  View, Text, TouchableOpacity, StyleSheet,
  Platform, AsyncStorage, Animated, Easing,
  Dimensions, DeviceEventEmitter, Image,
} from 'react-native';

import LinearGradient from 'react-native-linear-gradient';
import Orientation from 'react-native-orientation';
import DeviceInfo from 'react-native-device-info';
import styled from 'styled-components/native';
import CameraSettings from './camera-settings';
import CameraTray from './camera-tray';
import { invert } from 'lodash';

import {
  PERSIST_FASTCAM_MODE,
  PERSIST_FLASH_MODE,
  PERSIST_RESOLUTION_MODE,
} from './cccam-enums';

// TODO remove what we dont use for icons...
import { blankImage } from './images';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import FeatherIcon from 'react-native-vector-icons/Feather';

const chevronDown = (
  <MaterialIcon name="keyboard-arrow-down" size={24} style={{marginTop: 2}} color="white" />
);
const chevronUp= (
  <MaterialIcon name="expand-less" size={24} color="white" />
);
const chevronLeft = (
  <MaterialIcon name="chevron-left" size={32} color="white" />
);
const closeIcon = (
  <MaterialIcon name="close" size={24} color="white" />
);

const CAMERA_MODE_PHOTO = 'photo-mode';
const CAMERA_MODE_SCAN = 'scan-mode';

const isiPhoneX = Platform.OS === 'ios' && DeviceInfo.getDeviceId() === 'iPhone10,3';

const FASTCAM_ON_ICON = 'burst-mode'; // MaterialIcon set
const FASTCAM_OFF_ICON = 'photo'; // MaterialIcon set
// const FASTCAM_ON_ICON = 'visibility'; // MaterialIcon set
// const FASTCAM_OFF_ICON = 'visibility-off'; // MaterialIcon set
const FLASH_ON_ICON = 'flashlight'; // MaterialCommunityIcon set
const FLASH_OFF_ICON = 'flashlight-off'; // MaterialCommunityIcon set

const ModeIndicator = styled.View`
  margin-top: 4;
  height: 4;
  width: ${props => props.isCurrentMode ? '16' : '0'};
  border-radius: 2;
  background-color: #FFB300;
`;

const ModeTitle = styled.Text`
  background-color: transparent;
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
    marginTop: isiPhoneX ? 30 : 0,
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
  cancelButton: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  uiButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'transparent',
  },
  uiButtonSmall: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 32,
    height: 32,
    borderRadius: 32 / 2,
    borderColor: 'white',
    borderWidth: 2,
  },
  emptyUIbutton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 44,
    height: 32,
    borderRadius: 22,
    backgroundColor: 'transparent',
  },
  captureButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 72,
    height: 72,
    borderRadius: 36,
    borderWidth: 4,
    borderColor: 'white',
  },
  emptyCaptureButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 72,
    height: 32,
    borderWidth: 4,
    borderColor: 'transparent',
    backgroundColor: 'transparent',
  },
  footer: {
    paddingBottom: isiPhoneX ? 34 : 4,
  },
  captureContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
    paddingHorizontal: 8,
  },
  modeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
  },
  modeButton: {
    alignItems: 'center',
    flex: 1,
  },
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
  trayMostRecentImage: {
    height: 32,
    width: 32,
    borderRadius: Platform.OS === 'ios' ? 6 : 0,
    borderWidth: 2,
    borderColor: 'white',
    resizeMode: 'cover',
  },
  trayMostRecentImageOveraly: {
    alignItems: 'center',
    justifyContent: 'center',
    flex: 1,
    width: '100%',
    backgroundColor: 'rgba(0,0,0,0.2)',
  },
  settingsOverlay: {
    zIndex: 2,
    position: 'absolute',
    top: 0,
    left: 0,
    height: '100%',
    width: '100%',
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

const TRAY_EMPTY_TEXT_SCANNER = 'Hold camera steady over document.\nIt will automagically be scanned.';
const TRAY_EMPTY_TEXT_CAMERA = 'Photos you take will show in this tray\nand will reset when you close your camera.';

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
      isLandscape: false,
    };
  }

  componentDidMount() {
    const initialDeg = this.getDegreesForOrientation(this.props.orientation);
    this.state.orientationDegrees.setValue(initialDeg);
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.orientation !== this.props.orientation) {
      this._orientationChange(nextProps.orientation);
    }
  }

  getDegreesForOrientation = (orientation) => {
    const orientationEnum = Orientation.getOrientations();
    let deg = 0;
    switch (orientation) {
      case orientationEnum.landscapeleft:
        deg = 90;
        break;
      case orientationEnum.landscaperight:
        deg = -90;
        break;
      case orientationEnum.portraitupsidedown:
        deg = 180;
        break;
      case orientationEnum.portrait:
      default: break;
    }
    return deg;
  }

  _orientationChange = (orientation) => {
    const orientationEnum = Orientation.getOrientations();

    this.setState({ swapHeaderButtons: orientation === orientationEnum.landscapeleft });

    const nextDeg = this.getDegreesForOrientation(orientation);
    Animated.timing(this.state.orientationDegrees, {
      toValue: nextDeg,
      duration: 100,
    }).start();
  }

  setCameraMode = async (nextMode) => {
    const constants = { ...this.props.cameraConstants };
    const nextState = { ...this.props.cameraState };

    if (this.props.cameraState.cameraMode !== nextMode) {
      nextState.cameraMode = nextMode;

      switch (nextMode) {
        case constants.CameraMode.photo:
          if (this.props.cameraState.cameraMode === constants.CameraMode.scanner) {
            this.displayToast('Take Photos', '');
          } else {
            this.displayToast('FastCam Off', 'Photos must be confirmed to upload.');
          }
          break;
        case constants.CameraMode.scanner:
          this.displayToast('Scan Document', '');
          break;
        case constants.CameraMode.fastcam:
          this.displayToast('FastCam On', 'Photos immediately upload when captured.');
          break;
        default: break;
      }

      // store mode to be persisted later
      try {
        await AsyncStorage.setItem(PERSIST_FASTCAM_MODE, nextMode.toString());
      } catch (error) {
        console.warn('error storing camera mode', error);
      }
    }

    this.props.setCameraState(nextState);
    this.forceUpdate(); // since the state is in the parent, we need this to rerender icons
  }

  setResolutionMode = async (nextModeString) => {
    const constants = { ...this.props.cameraConstants };
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
    const constants = { ...this.props.cameraConstants };
    const nextState = { ...this.props.cameraState };

    switch (this.props.cameraState.flashMode) {
      case constants.FlashMode.off:
        nextState.flashMode = constants.FlashMode.torch;
        this.displayToast('Flashlight Enabled', '');
        break;
      case constants.FlashMode.torch:
        nextState.flashMode = constants.FlashMode.off;
        this.displayToast('Flashlight Disabled', '');
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
        duration: 100,
        // easing: Easing.cubic,
      }),
      Animated.timing(this.state.screenFlashOpacity, {
        toValue: 0,
        duration: 120,
        // easing: Easing.cubic,
      }),
    ]).start();
  }

  setToPhotoMode = () => {
    const constants = { ...this.props.cameraConstants };
    this.setCameraMode(constants.CameraMode.photo);
    this.props.setCameraTrayVisible(!this.props.cameraTrayVisible);
  }

  render() {
    const constants = { ...this.props.cameraConstants };
    const { flashMode, cameraMode } = this.props.cameraState;

    const TorchIsOn = flashMode === constants.FlashMode.torch;
    const PrimaryModeIsScan = cameraMode === constants.CameraMode.scanner;

    const invertedResolutionModes = invert(constants.ResolutionMode);

    const filteredCameraTrayData = this.props.cameraTrayData.filter((data) =>
      data.isDocument === PrimaryModeIsScan
    );

    let trayImageCount = '';
    let trayMostRecentImage = blankImage;
    if (filteredCameraTrayData.length > 0) {
      trayImageCount = filteredCameraTrayData.length;
      const recentURL = filteredCameraTrayData[0].url;
      trayMostRecentImage = { uri: recentURL };
    }

    return (
      <View style={styles.cameraUIContainer}>
        <LinearGradient
          colors={
            isiPhoneX ?
              ['rgba(0,0,0,0.35)', 'transparent'] :
              ['rgba(0,0,0,0.35)', 'rgba(0,0,0,0.05)', 'transparent']
          }
        >
          <View
            style={[styles.header, {
              flexDirection: this.state.swapHeaderButtons ? 'row-reverse' : 'row',
            }]}
          >
            {
              !PrimaryModeIsScan ?
                <TouchableOpacity
                  onPress={() => { this.props.onClose('', 'close'); }}
                  style={styles.uiButton}
                >
                  {closeIcon}
                </TouchableOpacity> :
                <TouchableOpacity onPress={this.setToPhotoMode}>
                  <Animated.View
                    style={[styles.uiButton, {
                      transform: [{
                        rotate: this.state.orientationDegrees.interpolate({
                          inputRange: [0, 1],
                          outputRange: ['0deg', '1deg'],
                        }),
                      }],
                    }]}
                  >
                    {chevronLeft}
                  </Animated.View>
                </TouchableOpacity>
            }

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

            <View style={styles.emptyUIbutton} />
            {/* <TouchableOpacity
              onPress={() => this.setState({ showSettings: true })}
              style={styles.uiButton}
            >
              <MaterialIcon name="settings" size={24} color="white" />
            </TouchableOpacity> */}
          </View>
        </LinearGradient>

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

        <LinearGradient
          colors={
            isiPhoneX ?
              ['transparent', 'rgba(0,0,0,0.35)'] :
              ['transparent', 'rgba(0,0,0,0.05)', 'rgba(0,0,0,0.35)']
          }
        >
          <View style={styles.footer}>

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


            <View
              style={[
                styles.captureContainer,
                { paddingBottom: this.props.cameraTrayVisible ? 8 : 24 }
              ]}
            >

              {/* show tray */}
              {
                !PrimaryModeIsScan ?
                (
                  filteredCameraTrayData.length > 0 ?
                    <TouchableOpacity
                      onPress={() => {
                        this.props.setCameraTrayVisible(!this.props.cameraTrayVisible);
                      }}
                    >
                      {
                        this.props.cameraTrayVisible ?
                          <View style={styles.uiButtonSmall}>
                            {chevronDown}
                          </View> :
                          <Animated.View
                            style={[styles.uiButton, {
                              transform: [{
                                rotate: this.state.orientationDegrees.interpolate({
                                  inputRange: [0, 1],
                                  outputRange: ['0deg', '1deg'],
                                }),
                              }],
                            }]}
                          >
                            <Image
                              style={styles.trayMostRecentImage}
                              source={trayMostRecentImage}
                            >
                              <View style={styles.trayMostRecentImageOveraly}>
                                <Text style={{ color: 'white' }}>
                                  {trayImageCount}
                                </Text>
                              </View>
                            </Image>
                          </Animated.View>
                      }
                    </TouchableOpacity> :

                    <TouchableOpacity
                      onPress={() => {
                        this.props.setCameraTrayVisible(!this.props.cameraTrayVisible);
                      }}
                    >
                      <View style={styles.uiButtonSmall}>
                        {
                          this.props.cameraTrayVisible ?
                            chevronDown :
                            chevronUp
                        }
                      </View>
                    </TouchableOpacity>
                ) : null
              }

              {/* Flash mode button */}
              <TouchableOpacity onPress={() => { this.toggleFlashMode(); }}>
                <Animated.View
                  style={[styles.uiButton, {
                    transform: [{
                      rotate: this.state.orientationDegrees.interpolate({
                        inputRange: [0, 1],
                        outputRange: ['0deg', '1deg'],
                      }),
                    }],
                  }]}
                >
                  <MaterialCommunityIcon
                    name={TorchIsOn ? FLASH_ON_ICON : FLASH_OFF_ICON}
                    size={24}
                    color="white"
                  />
                </Animated.View>
              </TouchableOpacity>

              {/* Capture button */}
              {
                !PrimaryModeIsScan ?
                  <TouchableOpacity
                    onPress={() => {
                      this.doFlashAnimation();
                      setTimeout(() => {
                        this.props.captureButtonPress(this.state.isLandscape);
                      }, 350); // flash takes 300ms
                    }}
                    style={styles.captureButton}
                  /> :
                  <View style={styles.emptyCaptureButton} />
              }

              {/* Front/back camera button */}
              {
                !PrimaryModeIsScan ?
                  <TouchableOpacity onPress={() => { this.props.flipCamera(); }}>
                    <Animated.View
                      style={[styles.uiButton, {
                        transform: [{
                          rotate: this.state.orientationDegrees.interpolate({
                            inputRange: [0, 1],
                            outputRange: ['0deg', '1deg'],
                          }),
                        }],
                      }]}
                    >
                      <FeatherIcon name="repeat" size={24} color="white" />
                    </Animated.View>
                  </TouchableOpacity> :
                  <View style={styles.emptyUIbutton} />
              }

              <TouchableOpacity
                onPress={() => this.setState({ showSettings: true })}
                style={styles.uiButton}
              >
                <MaterialIcon name="settings" size={24} color="white" />
              </TouchableOpacity>

            </View>

            {/* Photo mode buttons */}
            {this.props.cameraTrayVisible ? null :
              <View style={styles.modeContainer}>

                {/* Photo mode button */}
                <TouchableOpacity
                  onPress={() => this.setCameraMode(constants.CameraMode.photo)}
                  style={styles.modeButton}
                >
                  <ModeTitle isCurrentMode={!PrimaryModeIsScan}>
                    PHOTO
                  </ModeTitle>
                  <ModeIndicator isCurrentMode={!PrimaryModeIsScan} />
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
                  onPress={() => {
                    this.setCameraMode(constants.CameraMode.scanner);
                    this.props.setCameraTrayVisible(true);
                  }}
                  style={styles.modeButton}
                >
                  <ModeTitle isCurrentMode={PrimaryModeIsScan}>
                    SCAN
                  </ModeTitle>
                  <ModeIndicator isCurrentMode={PrimaryModeIsScan} />
                </TouchableOpacity>

                {/* AR mode button */}
                {
                  this.props.deviceSupportsARCam &&
                    <TouchableOpacity
                      onPress={() => this.props.arModePress()}
                      style={styles.modeButton}
                    >
                      <ModeTitle>AR</ModeTitle>
                      <ModeIndicator />
                    </TouchableOpacity>
                }

                {/* Before after mode button */}
                <TouchableOpacity
                  onPress={() => this.props.baModePress()}
                  style={styles.modeButton}
                >
                  <ModeTitle>B/A</ModeTitle>
                  <ModeIndicator />
                </TouchableOpacity>
              </View>
          }
          </View>

          <CameraTray
            visible={this.props.cameraTrayVisible}
            documentTrayHeaderVisible={PrimaryModeIsScan}
            primaryModeIsScan={PrimaryModeIsScan}
            emptyText={PrimaryModeIsScan ? TRAY_EMPTY_TEXT_SCANNER : TRAY_EMPTY_TEXT_CAMERA}
            trayItems={filteredCameraTrayData}
            onSelectTrayItem={this.props.onSelectTrayItem}
            onHideTray={() => {
              this.props.setCameraTrayVisible(false);
            }}
            setToPhotoMode={this.setToPhotoMode}
          />

        </LinearGradient>

        {
          this.state.showSettings &&
            <View style={styles.settingsOverlay}>
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
  orientation: PropTypes.number,
  deviceSupportsARCam: PropTypes.bool,

  cameraTrayData: PropTypes.array,
  cameraTrayVisible: PropTypes.bool,
  onSelectTrayItem: PropTypes.func,
  setCameraTrayVisible: PropTypes.func,

  arModePress: PropTypes.func,
  baModePress: PropTypes.func,
};

CameraLayout.defaultProps = {
  cameraTrayData: [],
  cameraTrayVisible: false,
  onSelectTrayItem: () => {},
  setCameraTrayVisible: () => {},
  arModePress: () => {},
  baModePress: () => {},
};

export default CameraLayout;
