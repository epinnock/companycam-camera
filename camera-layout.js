import React, { Component, PropTypes } from 'react';

import { View, Text, TouchableOpacity, StyleSheet, Platform } from 'react-native';
import LinearGradient from 'react-native-linear-gradient';
import DeviceInfo from 'react-native-device-info';
import CCCamera from '@companycam/companycam-camera';
import styled from 'styled-components/native';

// TODO remove what we dont use for icons...
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import FeatherIcon from 'react-native-vector-icons/Feather';

const CAMERA_MODE_PHOTO = 'photo-mode';
const CAMERA_MODE_SCAN = 'scan-mode';

const isiPhoneX = Platform.OS === 'ios' && DeviceInfo.getDeviceId() === 'iPhone10,3';

const FASTCAM_ON_ICON = 'burst-mode'; // MaterialIcon set
const FASTCAM_OFF_ICON = 'photo'; // MaterialIcon set
const FLASH_ON_ICON = 'flashlight'; // MaterialCommunityIcon set
const FLASH_OFF_ICON = 'flashlight-off'; // MaterialCommunityIcon set

const ModeIndicator = styled.View`
  ${''/* This is if we want to animate line to width of text - remove width style also if used */}
  ${''/* align-self: ${props => props.isCurrentMode ? 'stretch' : 'center'}; */}
  margin-top: 4;
  height: 4;
  width: ${props => props.isCurrentMode ? '16' : '0'};
  border-radius: 2;
  ${''/* background-color: white; */}

  ${''/* with color.... */}
  background-color: #FFB300;
`;

const ModeTitle = styled.Text`
  ${''/* color: ${props => props.isCurrentMode ? 'white' : 'rgba(255,255,255,0.5)'}; */}
  ${''/* font-weight: bold; */}
  background-color: transparent;

  ${''/* with color.... */}
  color: ${props => props.isCurrentMode ? '#FFB300' : 'white'};
`;

const styles = StyleSheet.create({
  cameraUIContainer: {
    flex: 1,
    zIndex: 1,
    justifyContent: 'space-between',
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
  captureButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 72,
    height: 72,
    borderRadius: 36,
    borderWidth: 4,
    borderColor: 'white',
  },
  footer: {
    paddingBottom: isiPhoneX ? 34 : 4,
  },
  captureContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
    paddingBottom: 24,
    paddingHorizontal: 8,
  },
  modeContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
  },
  modeButton: {
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
    };
  }

  setCameraMode = (nextMode) => {
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

  toggleFlashMode = () => {
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

    this.props.setCameraState(nextState);
    this.forceUpdate(); // since the state is in the parent, we need this to rerender icons
  }

  render() {
    const constants = {...this.props.cameraConstants};
    console.log(constants);

    const { flashMode, cameraMode } = this.props.cameraState;

    const TorchIsOn = flashMode === constants.FlashMode.torch;
    const PrimaryModeIsScan = cameraMode === constants.CameraMode.scanner;

    return (
      <View style={styles.cameraUIContainer}>
        <LinearGradient
          colors={
            isiPhoneX ?
              ['rgba(0,0,0,0.25)', 'transparent'] :
              ['rgba(0,0,0,0.25)', 'rgba(0,0,0,0.05)', 'transparent']
          }
        >
          <View style={styles.header}>
            <TouchableOpacity
              onPress={() => this.props.onClose()}
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
                Project Name
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              onPress={() => {}}
              style={styles.uiButton}
            >
              <MaterialIcon name="settings" size={24} color="white" />
            </TouchableOpacity>
          </View>
        </LinearGradient>

        {
          this.state.showToast ?
            <View style={styles.toast}>
              <Text style={styles.toastTitle}>{this.state.toastTitleText}</Text>
              <Text style={styles.toastMessage}>{this.state.toastMessageText}</Text>
            </View> : null
        }

        <LinearGradient
          colors={
            isiPhoneX ?
              ['transparent', 'rgba(0,0,0,0.25)'] :
              ['transparent', 'rgba(0,0,0,0.05)', 'rgba(0,0,0,0.25)']
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

            <View style={styles.captureContainer}>

              {/* Preview tray */}
              {
                !PrimaryModeIsScan &&
                  <TouchableOpacity
                    onPress={() => {}}
                    style={styles.uiButton}
                  >
                    <MaterialIcon name="crop-portrait" size={32} color="white" />
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
                    style={styles.uiButton}
                  >
                    <MaterialIcon
                      name={cameraMode === constants.CameraMode.fastcam ? FASTCAM_ON_ICON : FASTCAM_OFF_ICON}
                      size={24}
                      color="white"
                    />
                  </TouchableOpacity>
              }

              {/* Capture button */}
              {
                !PrimaryModeIsScan &&
                  <TouchableOpacity
                    onPress={() => {}}
                    style={styles.captureButton}
                  />
              }

              {/* Front/back camera button */}
              <TouchableOpacity
                onPress={() => {}}
                style={styles.uiButton}
              >
                <FeatherIcon name="repeat" size={24} color="white" />
              </TouchableOpacity>

              {/* Flash mode button */}
              <TouchableOpacity
                onPress={() => {this.toggleFlashMode(); }}
                style={styles.uiButton}
              >
                <MaterialCommunityIcon
                  name={TorchIsOn ? FLASH_ON_ICON : FLASH_OFF_ICON}
                  size={24}
                  color="white"
                />
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
                <ModeIndicator isCurrentMode={!PrimaryModeIsScan}/>
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
                <ModeIndicator isCurrentMode={PrimaryModeIsScan}/>
              </TouchableOpacity>

              {/* AR mode button */}
              <TouchableOpacity
                onPress={() => {}}
                style={styles.modeButton}
              >
                <ModeTitle>AR</ModeTitle>
                <ModeIndicator />
              </TouchableOpacity>

              {/* Before after mode button */}
              <TouchableOpacity
                onPress={() => {}}
                style={styles.modeButton}
              >
                <ModeTitle>B/A</ModeTitle>
                <ModeIndicator />
              </TouchableOpacity>
            </View>

          </View>
        </LinearGradient>
      </View>
    );
  }
}

CameraLayout.propTypes = {
  cameraConstants: PropTypes.object,
  cameraState: PropTypes.object,
  setCameraState: PropTypes.func,

  onClose: PropTypes.func,
};

export default CameraLayout;
