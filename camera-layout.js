import React, { Component, PropTypes } from 'react';

import { View, Text, TouchableOpacity, StyleSheet, Platform, Animated, Image } from 'react-native';

import LinearGradient from 'react-native-linear-gradient';
import Orientation from 'react-native-orientation';
import DeviceInfo from 'react-native-device-info';
import styled from 'styled-components/native';
import CameraTray from './camera-tray';

// TODO remove what we dont use for icons...
import { blankImage } from './images';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import FeatherIcon from 'react-native-vector-icons/Feather';

import { PRIMARY_MODE_PHOTO, PRIMARY_MODE_SCAN } from './cccam-enums';

const chevronDown = (
  <FeatherIcon name="chevron-down" size={24} style={{ marginTop: 2 }} color="white" />
);
const chevronUp = (
  <FeatherIcon name="chevron-up" size={24} style={{ marginBottom: 2 }} color="white" />
);

const chevronLeft = <FeatherIcon name="chevron-left" size={32} color="white" />;
const closeIcon = <MaterialIcon name="close" size={24} color="white" />;

const isiPhoneX = Platform.OS === 'ios' && DeviceInfo.getDeviceId() === 'iPhone10,3';

const FLASH_ON_ICON = 'flashlight'; // MaterialCommunityIcon set
const FLASH_OFF_ICON = 'flashlight-off'; // MaterialCommunityIcon set

const ModeIndicator = styled.View`
  margin-top: 4;
  height: 4;
  width: ${props => (props.isCurrentMode ? '16' : '0')};
  border-radius: 2;
  background-color: #ffb300;
`;

const ModeTitle = styled.Text`
  background-color: transparent;
  color: ${props => (props.isCurrentMode ? '#FFB300' : 'white')};
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
    overflow: 'hidden',
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
    backgroundColor: 'rgba(0,0,0,0.8)',
  },
  settingsWrapper: {
    flex: 1,
    margin: 16,
    borderRadius: 8,
    overflow: 'hidden',
  },
  settingsHeader: {
    width: '100%',
    height: 56,
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#0277BD',
    flexDirection: 'row',
    borderTopLeftRadius: 8,
    borderTopRightRadius: 8,
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

const TRAY_EMPTY_TEXT_SCANNER =
  'Hold camera steady over document.\nIt will automagically be scanned.';
const TRAY_EMPTY_TEXT_CAMERA =
  'Photos you take will show in this tray\nand will reset when you close your camera.';

class CameraLayout extends Component {
  constructor(props) {
    super(props);

    this.state = {
      author: 'Jared Goertzen',
      showSettings: false,
      rotationDeg: this.getDegreesForOrientation(props.orientation),
      swapHeaderButtons: false,
      isLandscape: false,
    };
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.orientation !== this.props.orientation) {
      const { orientation } = nextProps;
      const orientationEnum = Orientation.getOrientations();

      this.setState({
        swapHeaderButtons: orientation === orientationEnum.landscapeleft,
        rotationDeg: this.getDegreesForOrientation(orientation),
      });
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

  setToPhotoMode = () => {
    this.props.setPrimaryCameraMode(PRIMARY_MODE_PHOTO);
    this.props.setCameraTrayVisible(!this.props.cameraTrayVisible);
  };

  render() {
    const constants = { ...this.props.cameraConstants };
    const { flashMode, cameraMode } = this.props.cameraOpts;
    const { rotationDeg } = this.state;

    const TorchIsOn = flashMode === constants.FlashMode.torch;
    const PrimaryModeIsScan = cameraMode === constants.CameraMode.scanner;

    const filteredCameraTrayData = this.props.cameraTrayData.filter(
      data => data.isDocument === PrimaryModeIsScan
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
            isiPhoneX
              ? ['rgba(0,0,0,0.35)', 'transparent']
              : ['rgba(0,0,0,0.35)', 'rgba(0,0,0,0.05)', 'transparent']
          }
        >
          <View
            style={[
              styles.header,
              {
                flexDirection: this.state.swapHeaderButtons ? 'row-reverse' : 'row',
              },
            ]}
          >
            {!PrimaryModeIsScan ? (
              <TouchableOpacity
                hitSlop={{ top: 10, right: 10, bottom: 10, left: 10 }}
                onPress={() => {
                  this.props.onClose('', 'close');
                }}
                style={styles.uiButton}
              >
                {closeIcon}
              </TouchableOpacity>
            ) : (
                <TouchableOpacity
                  hitSlop={{ top: 10, right: 10, bottom: 10, left: 10 }}
                  onPress={this.setToPhotoMode}
                >
                  <View
                    style={[
                      styles.uiButton,
                      { transform: [{ rotate: rotationDeg }] },
                    ]}
                  >
                    {chevronLeft}
                  </View>
                </TouchableOpacity>
              )}

            <TouchableOpacity onPress={() => { }} style={styles.headerTitleButton}>
              <Text numberOfLines={1} style={styles.headerTitle}>
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

        {this.props.renderToast()}

        <LinearGradient
          colors={
            isiPhoneX
              ? ['transparent', 'rgba(0,0,0,0.35)']
              : ['transparent', 'rgba(0,0,0,0.05)', 'rgba(0,0,0,0.35)']
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
                { paddingBottom: this.props.cameraTrayVisible ? 8 : 24 },
              ]}
            >
              {/* show tray */}
              {!PrimaryModeIsScan ? (
                filteredCameraTrayData.length > 0 ? (
                  <TouchableOpacity
                    hitSlop={{ top: 10, right: 10, bottom: 10, left: 10 }}
                    onPress={() => {
                      this.props.setCameraTrayVisible(!this.props.cameraTrayVisible);
                    }}
                  >
                    {this.props.cameraTrayVisible ? (
                      <View style={[styles.uiButtonSmall, { backgroundColor: 'transparent' }]}>{chevronDown}</View>
                    ) : (
                        <View
                          style={[
                            styles.uiButton,
                            { transform: [{ rotate: rotationDeg }] },
                          ]}
                        >
                          <Image style={styles.trayMostRecentImage} source={trayMostRecentImage}>
                            <View style={styles.trayMostRecentImageOveraly}>
                              <Text style={{ color: 'white' }}>{trayImageCount}</Text>
                            </View>
                          </Image>
                        </View>
                      )}
                  </TouchableOpacity>
                ) : (
                    <TouchableOpacity
                      hitSlop={{ top: 10, right: 10, bottom: 10, left: 10 }}
                      onPress={() => {
                        this.props.setCameraTrayVisible(!this.props.cameraTrayVisible);
                      }}
                    >
                      <View style={[styles.uiButtonSmall, { backgroundColor: 'transparent' }]}>
                        {this.props.cameraTrayVisible ? chevronDown : chevronUp}
                      </View>
                    </TouchableOpacity>
                  )
              ) : null}

              {/* Flash mode button */}
              {this.props.hasFlash ? (
                <TouchableOpacity
                  hitSlop={{ top: 10, right: 10, bottom: 10, left: 10 }}
                  onPress={this.props.toggleFlashMode}
                >
                  <Animated.View
                    style={[
                      styles.uiButton,
                      { transform: [{ rotate: rotationDeg }] },
                    ]}
                  >
                    <MaterialCommunityIcon
                      name={TorchIsOn ? FLASH_ON_ICON : FLASH_OFF_ICON}
                      size={24}
                      color="white"
                    />
                  </Animated.View>
                </TouchableOpacity>
              ) : (
                  <View style={styles.uiButton} />
                )}

              {/* Capture button */}
              {!PrimaryModeIsScan ? (
                <TouchableOpacity
                  onPress={() => {
                    // this.doFlashAnimation();
                    this.props.captureButtonPress(this.state.isLandscape);
                  }}
                  style={styles.captureButton}
                />
              ) : (
                  <View style={styles.emptyCaptureButton} />
                )}

              {/* Front/back camera button */}
              {!PrimaryModeIsScan ? (
                <TouchableOpacity
                  hitSlop={{ top: 10, right: 10, bottom: 10, left: 10 }}
                  onPress={this.props.flipCamera}
                >
                  <View
                    style={[
                      styles.uiButton,
                      { transform: [{ rotate: rotationDeg }] },
                    ]}
                  >
                    <FeatherIcon name="repeat" size={24} color="white" />
                  </View>
                </TouchableOpacity>
              ) : (
                  <View style={styles.emptyUIbutton} />
                )}

              <TouchableOpacity
                hitSlop={{ top: 10, right: 10, bottom: 10, left: 10 }}
                onPress={() => this.setState({ showSettings: true })}
                style={styles.uiButton}
              >
                <MaterialIcon name="settings" size={24} color="white" />
              </TouchableOpacity>
            </View>

            {/* Photo mode buttons */}
            {this.props.cameraTrayVisible ? null : (
              <View style={styles.modeContainer}>
                {/* Photo mode button */}
                <TouchableOpacity
                  onPress={() => this.props.setPrimaryCameraMode(PRIMARY_MODE_PHOTO)}
                  style={styles.modeButton}
                >
                  <ModeTitle isCurrentMode={!PrimaryModeIsScan}>PHOTO</ModeTitle>
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
                    this.props.setPrimaryCameraMode(PRIMARY_MODE_SCAN);
                    this.props.setCameraTrayVisible(true);
                  }}
                  style={styles.modeButton}
                >
                  <ModeTitle isCurrentMode={PrimaryModeIsScan}>SCAN</ModeTitle>
                  <ModeIndicator isCurrentMode={PrimaryModeIsScan} />
                </TouchableOpacity>

                {/* AR mode button */}
                {this.props.deviceSupportsARCam && (
                  <TouchableOpacity
                    onPress={() => this.props.arModePress()}
                    style={styles.modeButton}
                  >
                    <ModeTitle>AR</ModeTitle>
                    <ModeIndicator />
                  </TouchableOpacity>
                )}

                {/* Before after mode button */}
                <TouchableOpacity
                  onPress={() => this.props.baModePress()}
                  style={styles.modeButton}
                >
                  <ModeTitle>B/A</ModeTitle>
                  <ModeIndicator />
                </TouchableOpacity>
              </View>
            )}
          </View>

          <CameraTray
            visible={this.props.cameraTrayVisible}
            // documentTrayHeaderVisible={PrimaryModeIsScan}
            documentTrayHeaderVisible={false} // TODO: when document scanner is ready to release, no longer force false
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

        {this.state.showSettings && (
          <View style={styles.settingsOverlay}>
            <View style={styles.settingsWrapper}>
              {/* settings header*/}
              <View style={styles.settingsHeader}>
                <TouchableOpacity
                  activeOpacity={1}
                  style={styles.uiButton}
                  onPress={() => this.setState({ showSettings: false })}
                >
                  <MaterialIcon name="close" size={24} color="white" />
                </TouchableOpacity>
                <Text style={{ fontSize: 17, color: 'white' }}>Camera Settings</Text>
                <View style={styles.emptyUIbutton} />
              </View>
              {/* settings components from this.props */}
              {this.props.settingsComponent}
            </View>
          </View>
        )}
      </View>
    );
  }
}

CameraLayout.propTypes = {
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
  hasFlash: PropTypes.bool,

  renderToast: PropTypes.func,
  settingsComponent: PropTypes.any,

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

CameraLayout.defaultProps = {
  cameraTrayData: [],
  cameraTrayVisible: false,
  onSelectTrayItem: () => { },
  setCameraTrayVisible: () => { },
  arModePress: () => { },
  baModePress: () => { },
  setPrimaryCameraMode: () => { },
};

export default CameraLayout;
