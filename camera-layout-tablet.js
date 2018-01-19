import React, { Component, PropTypes } from 'react';

import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  AsyncStorage,
  Animated,
  Easing,
  Image,
  Platform,
} from 'react-native';
import styled from 'styled-components/native';
import CameraSettings from './camera-settings';
import CameraTray from './camera-tray';
import { invert } from 'lodash';

// TODO remove what we dont use for icons...
import { blankImage } from './images';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import FeatherIcon from 'react-native-vector-icons/Feather';

import Orientation from 'react-native-orientation';

const chevronDown = (
  <MaterialIcon
    name="keyboard-arrow-down"
    size={24}
    style={{ marginTop: 2 }}
    color="white"
  />
);
const chevronUp = <MaterialIcon name="expand-less" size={24} color="white" />;
const chevronLeft = <MaterialIcon name="chevron-left" size={32} color="white" />;

const CAMERA_MODE_PHOTO = 'photo-mode';
const CAMERA_MODE_SCAN = 'scan-mode';

const FASTCAM_ON_ICON = 'burst-mode'; // MaterialIcon set
const FASTCAM_OFF_ICON = 'photo'; // MaterialIcon set
const FLASH_ON_ICON = 'flashlight'; // MaterialCommunityIcon set
const FLASH_OFF_ICON = 'flashlight-off'; // MaterialCommunityIcon set

const TRAY_EMPTY_TEXT_SCANNER =
  'Hold camera steady over document.\nIt will automagically be scanned.';
const TRAY_EMPTY_TEXT_CAMERA =
  'Photos you take will show in this tray\nand will reset when you close your camera.';

const ModeIndicator = styled.View`
  margin-top: 4;
  margin-bottom: 4;
  height: 4;
  width: ${(props) => (props.isCurrentMode ? '16' : '0')};
  border-radius: 2;
  background-color: #ffb300;
`;

const ModeTitle = styled.Text`
  background-color: transparent;
  font-size: 16;
  margin-top: 2;
  margin-bottom: 2;

  ${'' /* with color.... */} color: ${(props) =>
      props.isCurrentMode ? '#FFB300' : 'white'};
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
    backgroundColor: 'purple',
  },
  headerTitleWrapper: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'transparent',
  },
  headerTitle: {
    color: 'white',
    backgroundColor: 'transparent',
    textAlign: 'center',
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
  emptyCaptureButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 72,
    height: 32,
    borderWidth: 4,
    borderColor: 'transparent',
    backgroundColor: 'transparent',
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
    backgroundColor: 'rgba(0,200,0,0.4)',
    borderBottomColor: 'red',
    borderBottomWidth: 1,
    borderTopColor: 'red',
    borderTopWidth: 1,
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
    height: 144,
    width: 144,
  },
  modeButton: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
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

class CameraLayoutTablet extends Component {
  constructor(props) {
    super(props);

    const orientationEnum = Orientation.getOrientations();

    this.state = {
      author: 'Jared Goertzen',
      showSettings: false,
      screenFlashOpacity: new Animated.Value(0),
      rotationDeg: this.getDegreesForOrientation(props.orientation),
      swapHeaderButtons: false,
      swapCameraUI: false,
      dynamicFooterStyles: {
        alignItems: 'flex-end',
        flexDirection: 'column',
        justifyContent: 'center',
      },
      dynamicCaptureContainerStyles: {
        alignItems: 'center',
        justifyContent: 'space-around',
      },
      isLandscape:
        props.orientation === orientationEnum.landscapeleft ||
        props.orientation === orientationEnum.landscaperight,
    };
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.orientation !== this.props.orientation) {
      this._orientationChange(nextProps.orientation);
    }
  }

  getDegreesForOrientation = (orientation) => {
    const orientationEnum = Orientation.getOrientations();
    let deg = '0deg';
    switch (orientation) {
      case orientationEnum.landscapeleft:
        deg = '90deg';
        break;
      case orientationEnum.landscaperight:
        deg = '-90deg';
        break;
      case orientationEnum.portraitupsidedown:
        deg = '180deg';
        break;
      case orientationEnum.portrait:
      default:
        break;
    }
    return deg;
  };

  _orientationChange = (orientation) => {
    const orientationEnum = Orientation.getOrientations();

    let swapHeaderButtons = false;
    let swapCameraUI = false;
    let isLandscape = false;
    let outermostContainerFlex = 'column';
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
      case orientationEnum.portrait:
        dynamicCaptureContainerStyles = {
          paddingHorizontal: 32,
        };
        break;
      case orientationEnum.landscapeleft:
        swapHeaderButtons = true;
        isLandscape = true;
        outermostContainerFlex = 'row-reverse';
        dynamicFooterStyles = {
          alignItems: 'flex-end',
          flexDirection: 'row-reverse',
          justifyContent: 'center',
        };
        dynamicCaptureContainerStyles = {
          flexDirection: 'row-reverse',
          paddingVertical: 32,
        };
        break;
      case orientationEnum.landscaperight:
        swapCameraUI = true;
        isLandscape = true;
        dynamicFooterStyles = {
          alignItems: 'flex-start',
          flexDirection: 'row',
          justifyContent: 'center',
        };
        dynamicCaptureContainerStyles = {
          flexDirection: 'row',
          paddingVertical: 32,
        };
        outermostContainerFlex = 'row';
        break;
      case orientationEnum.portraitupsidedown:
        swapCameraUI = true;
        dynamicFooterStyles = {
          alignItems: 'flex-start',
          justifyContent: 'center',
          flexDirection: 'column-reverse',
        };
        dynamicCaptureContainerStyles = {
          flexDirection: 'column-reverse',
          paddingHorizontal: 32,
        };
        outermostContainerFlex = 'column-reverse';
        break;
      default:
        break;
    }

    this.setState({
      swapHeaderButtons,
      swapCameraUI,
      dynamicCaptureContainerStyles,
      dynamicFooterStyles,
      outermostContainerFlex,
      isLandscape,
      rotationDeg: this.getDegreesForOrientation(orientation),
    });
  };

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
  };

  render() {
    const constants = { ...this.props.cameraConstants };
    const { flashMode, cameraMode, resolutionMode } = this.props.cameraOpts;

    const { isLandscape, rotationDeg, swapCameraUI } = this.state;

    const TorchIsOn = flashMode === constants.FlashMode.torch;
    const PrimaryModeIsScan = cameraMode === constants.CameraMode.scanner;

    const invertedResolutionModes = invert(constants.ResolutionMode);
    const invertedCameraModes = invert(constants.CameraMode);

    const filteredCameraTrayData = this.props.cameraTrayData.filter(
      (data) => data.isDocument === PrimaryModeIsScan
    );

    let trayImageCount = '';
    let trayMostRecentImage = blankImage;
    if (filteredCameraTrayData.length > 0) {
      trayImageCount = filteredCameraTrayData.length;
      const recentURL = filteredCameraTrayData[0].url;
      trayMostRecentImage = { uri: recentURL };
    }

    return (
      <View
        style={[
          styles.cameraUIContainer,
          {
            flexDirection: this.state.outermostContainerFlex,
          },
        ]}
      >
        <View
          style={[
            styles.header,
            {
              flexDirection: isLandscape ? 'column' : 'row',
              transform: [{ rotate: swapCameraUI ? '180deg' : '0deg' }],
              height: isLandscape ? null : 56,
              width: isLandscape ? 56 : null,
            },
          ]}
        >
          <TouchableOpacity
            onPress={() => {
              this.props.onClose('', 'close');
            }}
            style={styles.uiButton}
          >
            <MaterialIcon name="close" size={24} color="white" />
          </TouchableOpacity>

          <TouchableOpacity onPress={() => {}} style={styles.headerTitleButton}>
            <View
              style={[
                styles.headerTitleWrapper,
                {
                  height: isLandscape ? null : 44,
                  width: isLandscape ? 44 : null,
                },
              ]}
            >
              <Text
                numberOfLines={1}
                style={[
                  styles.headerTitle,
                  {
                    transform: [{ rotate: isLandscape ? '90deg' : '0deg' }],
                    width: 400,
                  },
                ]}
              >
                {this.props.projectName || ''}
              </Text>
            </View>
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => this.setState({ showSettings: true })}
            style={styles.uiButton}
          >
            <MaterialIcon name="settings" size={24} color="white" />
          </TouchableOpacity>
        </View>

        {this.props.renderToast()}

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
          pointerEvents="box-none"
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

          <View
            style={[styles.captureContainer, this.state.dynamicCaptureContainerStyles]}
          >
            {/* Front/back camera button */}
            {!PrimaryModeIsScan && (
              <TouchableOpacity onPress={this.props.flipCamera}>
                <Animated.View
                  style={[
                    styles.uiButton,
                    {
                      transform: [
                        {
                          rotate: rotationDeg,
                        },
                      ],
                    },
                  ]}
                >
                  <FeatherIcon name="repeat" size={24} color="white" />
                </Animated.View>
              </TouchableOpacity>
            )}

            {/* Flash mode button */}
            {this.props.hasFlash ? (
              <TouchableOpacity onPress={this.props.toggleFlashMode}>
                <View
                  style={[
                    styles.uiButton,
                    {
                      transform: [
                        {
                          rotate: rotationDeg,
                        },
                      ],
                    },
                  ]}
                >
                  <MaterialCommunityIcon
                    name={TorchIsOn ? FLASH_ON_ICON : FLASH_OFF_ICON}
                    size={24}
                    color="white"
                  />
                </View>
              </TouchableOpacity>
            ) : (
              <View style={styles.uiButton} />
            )}

            {/* Capture button */}
            {!PrimaryModeIsScan && (
              <TouchableOpacity
                onPress={() => {
                  // this.doFlashAnimation();
                  this.props.captureButtonPress();
                }}
                style={styles.captureButton}
              />
            )}

            {/* Magic invisible view for when scanner mode is active */}
            {/* {
              PrimaryModeIsScan &&
                <View style={[styles.captureButton, { opacity: 0 }]} />
            } */}

            {/* show tray */}
            {!PrimaryModeIsScan ? (
              filteredCameraTrayData.length > 0 ? (
                <TouchableOpacity
                  onPress={() => {
                    this.props.setCameraTrayVisible(!this.props.cameraTrayVisible);
                  }}
                >
                  {this.props.cameraTrayVisible ? (
                    <View style={styles.uiButtonSmall}>{chevronDown}</View>
                  ) : (
                    <Animated.View
                      style={[
                        styles.uiButton,
                        {
                          transform: [
                            {
                              rotate: rotationDeg,
                            },
                          ],
                        },
                      ]}
                    >
                      <Image
                        style={styles.trayMostRecentImage}
                        source={trayMostRecentImage}
                      >
                        <View style={styles.trayMostRecentImageOveraly}>
                          <Text style={{ color: 'white' }}>{trayImageCount}</Text>
                        </View>
                      </Image>
                    </Animated.View>
                  )}
                </TouchableOpacity>
              ) : (
                <TouchableOpacity
                  onPress={() => {
                    this.props.setCameraTrayVisible(!this.props.cameraTrayVisible);
                  }}
                >
                  <View style={styles.uiButtonSmall}>
                    {this.props.cameraTrayVisible ? chevronDown : chevronUp}
                  </View>
                </TouchableOpacity>
              )
            ) : null}
          </View>

          {/* Photo mode buttons */}
          {this.props.cameraTrayVisible ? null : (
            <Animated.View
              style={[
                styles.modeContainer,
                {
                  transform: [
                    {
                      rotate: rotationDeg,
                    },
                  ],
                },
              ]}
            >
              {/* Photo mode button */}
              <TouchableOpacity
                onPress={() => this.props.setCameraMode(constants.CameraMode.photo)}
                style={styles.modeButton}
              >
                <ModeTitle isCurrentMode={!PrimaryModeIsScan}>PHOTO</ModeTitle>
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
                onPress={() => {
                  this.props.setCameraMode(constants.CameraMode.scanner);
                  this.props.setCameraTrayVisible(true);
                }}
                style={styles.modeButton}
              >
                <ModeTitle isCurrentMode={PrimaryModeIsScan}>SCAN</ModeTitle>
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
            </Animated.View>
          )}
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
          isLandscape={isLandscape}
          rotation={rotationDeg}
          swapCameraUI={swapCameraUI}
        />

        {this.state.showSettings && (
          <View style={styles.settingsOverlay}>
            <CameraSettings
              cameraModeString={invertedCameraModes[cameraMode]}
              resolutionModeString={invertedResolutionModes[resolutionMode]}
              setResolutionMode={this.props.setResolutionMode}
              closeSelf={() => this.setState({ showSettings: false })}
            />
          </View>
        )}
      </View>
    );
  }
}

CameraLayoutTablet.propTypes = {
  cameraConstants: PropTypes.object,

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
  captureButtonPress: PropTypes.func,

  setCameraMode: PropTypes.func,
  setResolutionMode: PropTypes.func,
  toggleFlashMode: PropTypes.func,

  renderToast: PropTypes.func,
};

CameraLayoutTablet.defaultProps = {
  cameraTrayData: [],
  cameraTrayVisible: false,
  onSelectTrayItem: () => {},
  setCameraTrayVisible: () => {},
  arModePress: () => {},
  baModePress: () => {},
};

export default CameraLayoutTablet;
