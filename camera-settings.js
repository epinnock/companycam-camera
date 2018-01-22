import React, { PureComponent, PropTypes } from 'react';

import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Platform,
  ScrollView,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';

// TODO remove what we dont use for icons...
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import FeatherIcon from 'react-native-vector-icons/Feather';

import CameraSetting from './components/CameraSetting';
import CameraSettingOption from './components/CameraSettingOption';

// uncomment for styling differently on iPhone X
// import DeviceInfo from 'react-native-device-info';
// const isiPhoneX = Platform.OS === 'ios' && DeviceInfo.getDeviceId() === 'iPhone10,3';

const RES_MODE_NORMAL = 'normal';
const RES_MODE_HIGH = 'high';
const RES_MODE_SUPER = 'super';

const CAMERA_MODE_FASTCAM = 'fastcam';
const CAMERA_MODE_REVIEW_MODE = 'photo';

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.8)',
    padding: 24,
  },
  container: {
    flex: 1,
    backgroundColor: '#F5F5F5',
    borderBottomLeftRadius: 8,
    borderBottomRightRadius: 8,
  },
  header: {
    width: '100%',
    height: 56,
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#0277BD',
    flexDirection: 'row',
    borderTopLeftRadius: 8,
    borderTopRightRadius: 8,
  },
  uiButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'transparent',
  },
  emptyUIbutton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'transparent',
  },
  sectionTitle: {
    fontSize: 20,
    color: 'black',
    fontWeight: 'bold',
    marginHorizontal: 24,
    marginBottom: 16,
    marginTop: 32,
  },
  optionRowContainer: {
    width: '85%',
  },
  optionRow: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    borderColor: '#F5F5F5',
    paddingVertical: 8,
    paddingHorizontal: 24,
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  optionText: {
    color: 'black',
    fontSize: 17,
  },
  optionDescription: {
    color: '#AAA',
    fontSize: 11,
  },
});

class CameraSettings extends PureComponent {
  constructor(props) {
    super(props);

    this.state={
      resolutionMode: 1,
      reviewMode: 1,
      openToCamera: 1,
    }
  }

  render() {
    const { cameraModeString, resolutionModeString } = this.props;

    return (
      <View style={styles.overlay}>
        <View style={styles.header}>
          <TouchableOpacity
            activeOpacity={1}
            style={styles.uiButton}
            onPress={() => this.props.closeSelf()}
          >
            <MaterialIcon name="close" size={24} color="white" />
          </TouchableOpacity>
            <Text style={{ fontSize: 17, color: 'white' }}>
              Camera Settings
            </Text>
          <View style={styles.emptyUIbutton} />
        </View>

        <ScrollView style={styles.container}>
          {/* Image Quality */}
          <CameraSetting title='Image Quality'>
            <CameraSettingOption
              onPress={() => { this.setState({ resolutionMode: 1 }) }}
              title="Normal"
              description='Best fore everyday use. Smallest file size. Uses the least data.'
              isSelected={this.state.resolutionMode === 1}
            />
            <CameraSettingOption
              onPress={() => { this.setState({ resolutionMode: 2 }) }}
              title="High"
              description='Best for balancing image quality and file size. Uses more data.'
              isSelected={this.state.resolutionMode === 2}
            />
            <CameraSettingOption
              onPress={() => { this.setState({ resolutionMode: 3 }) }}
              title="Super Fine"
              description='Best for capturing great details. Lartest file size. Uses the most data.'
              isSelected={this.state.resolutionMode === 3}
            />
          </CameraSetting>

          {/* Review Mode */}
          <CameraSetting
            title='Review Mode'
            description='Review photos before the upload.'
          >
            <CameraSettingOption
              onPress={() => { this.setState({ reviewMode: 1 }) }}
              title="Off"
              isSelected={this.state.reviewMode === 1}
            />
            <CameraSettingOption
              onPress={() => { this.setState({ reviewMode: 2 }) }}
              title="On"
              isSelected={this.state.reviewMode === 2}
            />
          </CameraSetting>

          {/* Open to Camera */}
          <CameraSetting
            title='Open to Camera'
            description='Open the camera when you load the app.b'
          >
            <CameraSettingOption
              onPress={() => { this.setState({ openToCamera: 1 }) }}
              title="Off"
              isSelected={this.state.openToCamera === 1}
            />
            <CameraSettingOption
              onPress={() => { this.setState({ openToCamera: 2 }) }}
              title="On"
              isSelected={this.state.openToCamera === 2}
            />
          </CameraSetting>

          {/* <CameraSetting title='Review Mode'>
            <CameraSettingOption description='option 1' />
            <CameraSettingOption description='option 2' />
          </CameraSetting>

          <CameraSetting title='Open to Camera'>
            <CameraSettingOption description='option 1' />
            <CameraSettingOption description='option 2' />
          </CameraSetting> */}

          {/* Quality Settings group */}
          {/* <Text style={styles.sectionTitle}>Image Quality</Text>

          <TouchableOpacity
            onPress={() => this.props.setResolutionMode(RES_MODE_NORMAL)}
            style={[
              styles.optionRow,
              {
                borderTopWidth: 1,
                borderTopColor: '#EEEEEE',
              },
            ]}
          >
            <View style={styles.optionRowContainer}>
              <Text style={styles.optionText}>Normal</Text>
              <Text style={styles.optionDescription}>
                Best for everyday use. Smallest file size. Uses the least data.
              </Text>
            </View>
            {resolutionModeString === RES_MODE_NORMAL && (
              <FeatherIcon name="check" size={24} color="black" />
            )}
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => this.props.setResolutionMode(RES_MODE_HIGH)}
            style={styles.optionRow}
          >
            <View style={styles.optionRowContainer}>
              <Text style={styles.optionText}>High</Text>
              <Text style={styles.optionDescription}>
                Best for balancing image quality and file size. Uses more data.
              </Text>
            </View>
            {resolutionModeString === RES_MODE_HIGH && (
              <FeatherIcon name="check" size={24} color="black" />
            )}
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => this.props.setResolutionMode(RES_MODE_SUPER)}
            style={styles.optionRow}
          >
            <View style={styles.optionRowContainer}>
              <Text style={styles.optionText}>Super Fine</Text>
              <Text style={styles.optionDescription}>
                Best for capturing details. Largest file size. Uses the most
                data.
              </Text>
            </View>
            {resolutionModeString === RES_MODE_SUPER && (
              <FeatherIcon name="check" size={24} color="black" />
            )}
          </TouchableOpacity> */}

          {/* <Text style={styles.sectionTitle}>Review Mode</Text>

          <TouchableOpacity
            onPress={() => {
              this.props.setCameraMode(CAMERA_MODE_REVIEW_MODE);
            }}
            style={[
              styles.optionRow,
              {
                borderTopWidth: 1,
                borderTopColor: '#EEEEEE',
              },
            ]}
          >
            <View style={styles.optionRowContainer}>
              <Text style={styles.optionText}>On</Text>
              <Text style={styles.optionDescription}>
                Every time a photo is captured, review and edit before uploading
              </Text>
            </View>
            {cameraModeString === CAMERA_MODE_REVIEW_MODE && (
              <FeatherIcon name="check" size={24} color="black" />
            )}
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => {
              this.props.setCameraMode(CAMERA_MODE_FASTCAM);
            }}
            style={styles.optionRow}
          >
            <View style={styles.optionRowContainer}>
              <Text style={styles.optionText}>Off</Text>
              <Text style={styles.optionDescription}>
                Captured photos will immediately upload to CompanyCam
              </Text>
            </View>
            {cameraModeString === CAMERA_MODE_FASTCAM && (
              <FeatherIcon name="check" size={24} color="black" />
            )}
          </TouchableOpacity> */}

        </ScrollView>
      </View>
    );
  }
}

CameraSettings.propTypes = {
  resolutionModeString: PropTypes.string,
  setResolutionMode: PropTypes.func,
  closeSelf: PropTypes.func,
};

export default CameraSettings;
