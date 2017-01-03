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
          console.log(`[CCC JS] Width = [${Dimensions.get('window').width}] Height = [${Dimensions.get('window').height}]`);
        }}
        photoAccepted={(filename, imgDims) => {
          console.log(`[CCC JS] Photo accepted: ${imgDims[0]}x${imgDims[1]} ${filename}`);
        }}
        photoTaken={(filename, imgDims) => {
          console.log(`[CCC JS] Photo taken: ${imgDims[0]}x${imgDims[1]} ${filename}`);
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
