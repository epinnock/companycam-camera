import React from 'react';
import {
  Image, StyleSheet, TouchableOpacity, Text, View,
} from 'react-native';
// import colors from '@companycam/companycam-colors';

import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import FeatherIcon from 'react-native-vector-icons/Feather';

const checkIcon = (
  <MaterialCommunityIcon
    name="check"
    size={24}
    color="#0277BD"
  />
);

const styles = StyleSheet.create({
  checkBox: {
    width: 48,
    height: null,
    alignItems: 'center',
    justifyContent: 'center',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'stretch',
    backgroundColor: 'white',
  },
  rowDetails: {
    flex: 1,
    paddingHorizontal: 8,
    paddingVertical: 12,
    justifyContent: 'center',
    borderBottomWidth: 1,
    borderBottomColor: '#EEEEEE',
  },
  title: {
    color: '#212121',
    fontSize: 14,
    fontWeight: 'bold',
  },
  description: {
    color: '#616161',
    paddingTop: 4,
    fontSize: 12,
  },
});

const CameraSettingOption = (props) => (
  <TouchableOpacity
    onPress={props.onPress}
  >
    <View style={styles.row}>

      <View style={styles.checkBox}>
        { props.isSelected &&
          <Text>{checkIcon}</Text>
        }
      </View>

      <View style={styles.rowDetails}>
        <Text style={styles.title}>{props.title}</Text>
        { props.description &&
            <Text style={styles.description}>{props.description}</Text>
        }
      </View>

    </View>
  </TouchableOpacity>
);

CameraSettingOption.propTypes = {
  isSelected: React.PropTypes.bool,
  title: React.PropTypes.string,
  description: React.PropTypes.string,
}

export default CameraSettingOption;
