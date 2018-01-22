import React from 'react';
import {
  View, Text, Image, StyleSheet
} from 'react-native';
// import colors from '@companycam/companycam-colors';

const styles = StyleSheet.create({
  header: {
    backgroundColor: '#F5F5F5',
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#E0E0E0',
  },
  title: {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#212121',
  },
  description: {
    color: '#757575',
    fontSize: 14,
  },
});

const CameraSetting = (props) => (
  <View>
    <View style={styles.header}>
      <Text style={styles.title}>{props.title}</Text>
      { props.description &&
        <Text style={styles.description}>{props.description}</Text>
      }
    </View>
    {props.children}
  </View>
);

CameraSetting.propTypes = {
  title: React.PropTypes.string,
  description: React.PropTypes.string,
}

export default CameraSetting;
