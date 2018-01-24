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
import { invert } from 'lodash';
import CameraSettings from './camera-settings';
import CameraTray from './camera-tray';
import UIButton from './components/UIButton';
import LinearGradient from 'react-native-linear-gradient';

// TODO remove what we dont use for icons...
import { blankImage } from './images';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import FeatherIcon from 'react-native-vector-icons/Feather';

import Orientation from 'react-native-orientation';

import { PRIMARY_MODE_PHOTO, PRIMARY_MODE_SCAN } from './cccam-enums';

const chevronDown = (
  <MaterialIcon name="keyboard-arrow-down" size={24} style={{ marginTop: 2 }} color="white" />
);
const chevronUp = <MaterialIcon name="expand-less" size={24} color="white" />;
// const chevronLeft = <MaterialIcon name="chevron-left" size={32} color="white" />;

const FLASH_ON_ICON = 'flashlight'; // MaterialCommunityIcon set
const FLASH_OFF_ICON = 'flashlight-off'; // MaterialCommunityIcon set

const TRAY_EMPTY_TEXT_SCANNER =
  'Hold camera steady over document.\nIt will automagically be scanned.';
const TRAY_EMPTY_TEXT_CAMERA =
  'Photos you take will show in this tray\nand will reset when you close your camera.';

const CameraMode = styled.View`
  padding: 4px 12px;
  border-radius: 8px;
  background-color: ${(props) => (props.isCurrentMode ? 'rgba(0,0,0,0.1)' : 'transparent')};
`;

const ModeTitle = styled.Text`
  font-size: 16px;
  color: ${(props) => (props.isCurrentMode ? '#FFB300' : 'white')};
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
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerTitleTextWrapper: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'transparent',
  },
  headerTitleText: {
    color: 'white',
    backgroundColor: 'transparent',
    textAlign: 'center',
    alignSelf: 'center',
  },
  headerTitleButton: {
    flexDirection: 'row',
    paddingHorizontal: 12,
    paddingVertical: 4,
    backgroundColor: 'rgba(0,0,0,0.1)',
    borderRadius: 50,
  },
  uiButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 44,
    height: 44,
    margin: 16,
    borderRadius: 22,
  },
  uiButtonSmall: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 44,
    height: 44,
    borderRadius: 44 / 2,
    borderColor: 'white',
    borderWidth: 2,
    margin: 16,
  },
  // emptyUIbutton: {
  //   alignItems: 'center',
  //   justifyContent: 'center',
  //   width: 44,
  //   height: 32,
  //   borderRadius: 22,
  //   backgroundColor: 'transparent',
  // },
  // emptyCaptureButton: {
  //   alignItems: 'center',
  //   justifyContent: 'center',
  //   width: 72,
  //   height: 32,
  //   borderWidth: 4,
  //   borderColor: 'transparent',
  //   backgroundColor: 'transparent',
  //   margin: 16,
  // },
  captureButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 72,
    height: 72,
    borderRadius: 36,
    borderWidth: 4,
    borderColor: 'white',
    margin: 16,
  },
  footer: {
    flex: 1,
    // backgroundColor: 'blue',
  },
  captureContainer: {
    alignItems: 'center',
    justifyContent: 'space-around',
    // backgroundColor: 'yellow',
  },
  modeContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'red',
    width: 112,
    height: 112,
  },
  emptyModeContainer: {
    backgroundColor: 'red',
    width: 112,
    height: 112,
  },
  modeButton: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
  },
  trayMostRecentImage: {
    height: 44,
    width: 44,
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
        paddingVertical: 32,
        backgroundColor: 'papayawhip',
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
      justifyContent: 'space-around',
      paddingVertical: 32,
      backgroundColor: 'papayawhip',
    };

    switch (orientation) {
      case orientationEnum.portrait:
        dynamicCaptureContainerStyles = {
          paddingVertical: 32,
          backgroundColor: 'purple',
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
          paddingHorizontal: 32,
          backgroundColor: 'yellow',
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
          paddingHorizontal: 32,
          backgroundColor: 'green',
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
          paddingVertical: 32,
          backgroundColor: 'pink',
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

    // default bg gradient in portrait
    let gradientStartX = 1.0;
    let gradientStartY = 0.0;
    let gradientEndX = 0.0;
    let gradientEndY = 0.0;
    let gradientLocationStart = 0;
    let gradientLocationEnd = 0.18;

    if (isLandscape) {
      gradientStartX = 0.0;
      gradientStartY = 0.0;
      gradientEndX = 0.0;
      gradientEndY = 1.0;
      gradientLocationStart = 0;
      gradientLocationEnd = 0.15;
    }

    return (
      <LinearGradient
        colors={['rgba(0,0,0,0.4)', 'transparent']}
        locations={[gradientLocationStart, gradientLocationEnd]}
        start={{ x: gradientStartX, y: gradientStartY }}
        end={{ x: gradientEndX, y: gradientEndY }}
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
          <UIButton
            onPress={() => {
              this.props.onClose('', 'close');
            }}
            bgColor="rgba(0,0,0,0.1)"
          >
            <MaterialIcon name="close" size={24} color="white" />
          </UIButton>

          <View
            style={[
              styles.headerTitle,
              {
                height: isLandscape ? null : 44,
                width: isLandscape ? 44 : null,
              },
            ]}
          >
            <TouchableOpacity
              onPress={() => {}}
              style={[
                styles.headerTitleButton,
                { transform: [{ rotate: isLandscape ? '90deg' : '0deg' }] },
              ]}
            >
              <View style={styles.headerTitleTextWrapper}>
                <Text
                  numberOfLines={1}
                  style={[
                    styles.headerTitleText,
                    // TODO make this better, preferrably not set width for landscape
                    { width: isLandscape ? 200 : null },
                  ]}
                >
                  {this.props.projectName || ''}
                </Text>
              </View>
            </TouchableOpacity>
          </View>

          <UIButton onPress={() => this.setState({ showSettings: true })} bgColor="rgba(0,0,0,0.1)">
            <MaterialIcon name="settings" size={24} color="white" />
          </UIButton>
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

        <View style={[styles.footer, this.state.dynamicFooterStyles]} pointerEvents="box-none">
          <View
            style={[
              styles.captureContainer,
              this.state.dynamicCaptureContainerStyles,
              {
                height: isLandscape ? 112 : null,
                width: isLandscape ? null : 112,
              },
            ]}
          >
            {/* Front/back camera button */}
            {!PrimaryModeIsScan && (
              <UIButton onPress={this.props.flipCamera} bgColor="rgba(0,0,0,0.1)">
                <Animated.View style={{ transform: [{ rotate: rotationDeg }] }}>
                  <FeatherIcon name="repeat" size={24} color="white" />
                </Animated.View>
              </UIButton>
            )}

            {/* Flash mode button */}
            {this.props.hasFlash ? (
              <UIButton onPress={this.props.toggleFlashMode} bgColor="rgba(0,0,0,0.1)">
                <Animated.View style={{ transform: [{ rotate: rotationDeg }] }}>
                  <MaterialCommunityIcon
                    name={TorchIsOn ? FLASH_ON_ICON : FLASH_OFF_ICON}
                    size={24}
                    color="white"
                  />
                </Animated.View>
              </UIButton>
            ) : // <View style={styles.uiButton} />
            null}

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
                      <Image style={styles.trayMostRecentImage} source={trayMostRecentImage}>
                        <View style={styles.trayMostRecentImageOveraly}>
                          <Text style={{ color: 'white', fontSize: 17 }}>{trayImageCount}</Text>
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
                  <UIButton>{this.props.cameraTrayVisible ? chevronDown : chevronUp}</UIButton>
                </TouchableOpacity>
              )
            ) : null}
          </View>

          {/* Photo mode buttons */}
          {this.props.cameraTrayVisible ? (
            <View style={styles.emptyModeContainer} />
          ) : (
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
                onPress={() => this.props.setPrimaryCameraMode(PRIMARY_MODE_PHOTO)}
                style={styles.modeButton}
              >
                <CameraMode isCurrentMode={!PrimaryModeIsScan}>
                  <ModeTitle isCurrentMode={!PrimaryModeIsScan}>PHOTO</ModeTitle>
                </CameraMode>
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
                  this.props.setPrimaryCameraMode(PRIMARY_MODE_SCAN);
                  this.props.setCameraTrayVisible(true);
                }}
                style={styles.modeButton}
              >
                <CameraMode>
                  <ModeTitle isCurrentMode={PrimaryModeIsScan}>SCAN</ModeTitle>
                </CameraMode>
              </TouchableOpacity>

              {/* AR mode button */}
              <TouchableOpacity onPress={() => this.props.arModePress()} style={styles.modeButton}>
                <CameraMode>
                  <ModeTitle>AR</ModeTitle>
                </CameraMode>
              </TouchableOpacity>

              {/* Before after mode button */}
              <TouchableOpacity onPress={() => this.props.baModePress()} style={styles.modeButton}>
                <CameraMode>
                  <ModeTitle>B/A</ModeTitle>
                </CameraMode>
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
      </LinearGradient>
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

  setPrimaryCameraMode: PropTypes.func,
  setResolutionMode: PropTypes.func,
  toggleFlashMode: PropTypes.func,

  renderToast: PropTypes.func,

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
