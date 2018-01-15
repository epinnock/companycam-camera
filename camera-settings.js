import React, { PureComponent, PropTypes } from 'react';

import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Platform,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';

// TODO remove what we dont use for icons...
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import FeatherIcon from 'react-native-vector-icons/Feather';

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
  },
  container: {
    flex: 1,
    backgroundColor: 'white',
    borderRadius: 8,
    margin: 24,
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
    fontSize: 13,
  },
});

class CameraSettings extends PureComponent {
  render() {
    const { cameraModeString, resolutionModeString } = this.props;

    return (
      <View style={styles.overlay}>
        <View style={styles.container}>
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

          {/* Quality Settings group */}
          <Text style={styles.sectionTitle}>Image Quality</Text>

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
          </TouchableOpacity>

          <Text style={styles.sectionTitle}>Review Mode</Text>

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
          </TouchableOpacity>
        </View>
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
