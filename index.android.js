import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Dimensions
} from 'react-native';

import CCCamera from './cccamera';

export default class newcam extends Component {

  render() {
    return (
      <CCCamera
        style={styles.fullscreen}
        storagePath={"/"}
        projectName={"Project name test"}
        projectAddress={"Project address test"}
        onClose={(error,button) => {
          console.log(`[CCC JS] Invoked callback: [${error}] [${button}]`);
        }}
        photoAccepted={(filename) => {
          console.log(`[CCC JS] Photo accepted: ${filename}`);
        }}
        photoTaken={(filename) => {
          console.log(`[CCC JS] Photo taken: ${filename}`);
        }}
      />
    );
  }
}

const styles = StyleSheet.create({
  fullscreen: {
    width: Dimensions.get('window').width,
    height: Dimensions.get('window').height,
  },
});

AppRegistry.registerComponent('newcam', () => newcam);
