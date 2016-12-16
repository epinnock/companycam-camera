import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  View
} from 'react-native';

import CCCamera from './cccamera';

export default class newcam extends Component {
  
  render() {
    return (
      <View style={styles.container}>
        <CCCamera
          projectName={"Project name test"}
          projectAddress={"Project address test"}
          onClose={(error,button) => {
            console.log(`[CCC JS] Invoked callback: [${error}] [${button}]`);
          }}
        />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});

AppRegistry.registerComponent('newcam', () => newcam);
