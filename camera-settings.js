import React, { PureComponent, PropTypes } from 'react';

import { View, Text, TouchableOpacity, StyleSheet, Platform } from 'react-native';
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

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'white',
    borderRadius: 8,
  },
  header: {
    width: '100%',
    height: 56,
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#0277BD',
    flexDirection: 'row',
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
    const { resolutionModeString } = this.props;

    return (
      <View style={styles.container}>
        <View style={styles.header}>
          <TouchableOpacity
            activeOpacity={1}
            style={styles.uiButton}
            onPress={() => this.props.closeSelf()}
          >
            <MaterialIcon name="close" size={24} color="white" />
          </TouchableOpacity>
          <Text style={{ fontSize: 17, color: 'white' }}>Camera Settings</Text>
          <View style={styles.emptyUIbutton} />
        </View>

        {/* Quality Settings group */}
        <Text style={styles.sectionTitle}>Image Quality</Text>

        <TouchableOpacity
          onPress={() => this.props.setResolutionMode(RES_MODE_NORMAL)}
          style={[
            styles.optionRow, {
              borderTopWidth: 1,
              borderTopColor: '#EEEEEE',
            }
          ]}
        >
          <View style={styles.optionRowContainer}>
            <Text style={styles.optionText}>Normal</Text>
            <Text style={styles.optionDescription}>
              Best for everyday use. Smallest file size. Uses the least data.
            </Text>
          </View>
          <FeatherIcon
            name="check"
            size={24}
            color={resolutionModeString === RES_MODE_NORMAL ? 'black' : 'transparent'}
          />
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
          <FeatherIcon
            name="check"
            size={24}
            color={resolutionModeString === RES_MODE_HIGH ? 'black' : 'transparent'}
          />
        </TouchableOpacity>

        <TouchableOpacity
          onPress={() => this.props.setResolutionMode(RES_MODE_SUPER)}
          style={styles.optionRow}
        >
          <View style={styles.optionRowContainer}>
            <Text style={styles.optionText}>Super Fine</Text>
            <Text style={styles.optionDescription}>
              Best for capturing details. Largest file size. Uses the most data.
            </Text>
          </View>
          <FeatherIcon
            name="check"
            size={24}
            color={resolutionModeString === RES_MODE_SUPER ? 'black' : 'transparent'}
          />
        </TouchableOpacity>

        <Text style={styles.sectionTitle}>Image Editor</Text>

        <TouchableOpacity
          onPress={() => {}}
          style={[
            styles.optionRow, {
              borderTopWidth: 1,
              borderTopColor: '#EEEEEE',
            }
          ]}
        >
          <View style={styles.optionRowContainer}>
            <Text style={styles.optionText}>Edit Mode On</Text>
            <Text style={styles.optionDescription}>
              Open the editor everytime a photo is taken.
            </Text>
          </View>
          <FeatherIcon
            name="check"
            size={24}
            color={resolutionModeString === RES_MODE_NORMAL ? 'black' : 'transparent'}
          />
        </TouchableOpacity>

        <TouchableOpacity
          onPress={() => {}}
          style={styles.optionRow}
        >
          <View style={styles.optionRowContainer}>
            <Text style={styles.optionText}>Edit Mode Off</Text>
            <Text style={styles.optionDescription}>
              Do not open the editor when taking photos.
            </Text>
          </View>
          <FeatherIcon
            name="check"
            size={24}
            color={resolutionModeString === RES_MODE_HIGH ? 'black' : 'transparent'}
          />
        </TouchableOpacity>

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
