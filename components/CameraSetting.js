import React from 'react';
import {
  View, Text, Image, StyleSheet
} from 'react-native';
// import colors from '@companycam/companycam-colors';

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'red',
  },
});

const CameraSetting = (props) => (
  <View
    style={styles.container}
  >
    <Text>{props.title}</Text>
    { props.description &&
      <Text>{props.description}</Text>
    }
    {props.children}
  </View>
);

CameraSetting.defaultProps = {
   
}

CameraSetting.propTypes = {
  title: React.PropTypes.string,
  description: React.PropTypes.string,
}

export default CameraSetting;
