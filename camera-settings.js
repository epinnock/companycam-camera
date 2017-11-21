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
    margin: 16,
    backgroundColor: 'white',
    borderRadius: 8,
    overflow: 'hidden',
  },
  header: {
    width: '100%',
    height: 48,
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: '#CCC',
    flexDirection: 'row',
    paddingHorizontal: 24,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginHorizontal: 24,
    marginVertical: 16,
  },
  optionRow: {
    flexDirection: 'row',
    borderBottomWidth: 1,
    borderColor: '#CCC',
    paddingVertical: 8,
    paddingHorizontal: 24,
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  optionText: {
    fontSize: 18,
  },
  optionDescription: {
    color: '#AAA',
    fontSize: 14,
  },
});

class CameraSettings extends PureComponent {

  render() {
    const { resolutionModeString } = this.props;

    return (
      <View style={styles.container}>
        <View style={styles.header}>
          <FeatherIcon name="x" size={24} color="transparent" />
          <Text style={{ fontSize: 16 }}>Camera Settings</Text>
          <FeatherIcon name="x" size={24} color="black" />
        </View>

        {/* Quality Settings group */}
        <Text style={styles.sectionTitle}>Quality Setting</Text>

        <TouchableOpacity
          onPress={() => this.props.setResolutionMode(RES_MODE_NORMAL)}
          style={[styles.optionRow, { borderTopWidth: 1 }]}
        >
          <View style={{ width: '85%' }}>
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
          <View style={{ width: '85%' }}>
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
          <View style={{ width: '85%' }}>
            <Text style={styles.optionText}>Super Fine</Text>
            <Text style={styles.optionDescription}>
              Best for capturing great details. Largest file size. Uses the most data.
            </Text>
          </View>
          <FeatherIcon
            name="check"
            size={24}
            color={resolutionModeString === RES_MODE_SUPER ? 'black' : 'transparent'}
          />
        </TouchableOpacity>

      </View>
    );
  }
}

CameraSettings.propTypes = {
  resolutionModeString: PropTypes.string,
  setResolutionMode: PropTypes.func,
};

export default CameraSettings;
