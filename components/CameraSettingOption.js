import React from 'react';
import {
  Image, StyleSheet, TouchableOpacity, Text, View,
} from 'react-native';
// import colors from '@companycam/companycam-colors';

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'blue',
  },
  checkBox: {
    width: 48,
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'yellow',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'stretch',
    backgroundColor: 'red',
  },
  rowDetails: {
    flex: 1,
    backgroundColor: 'green', 
    justifyContent: 'center',
  },
});

const CameraSettingOption = (props) => (
  <TouchableOpacity
    onPress={props.onPress}
    style={styles.container}
  >
    <View style={styles.row}>

      <View style={styles.checkBox}>
        { props.isSelected &&
          <Text>X</Text>
        }
      </View>

      <View style={styles.rowDetails}>
        <Text>{props.title}</Text>
        { props.description &&
            <Text>{props.description}</Text>
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
