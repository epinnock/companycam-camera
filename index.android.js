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
        storagePath={"some path"}
        projectName={"Project name test"}
        projectAddress={"Project address test"}
        onClose={(error,button) => {
          console.log(`[CCC JS] Invoked callback: [${error}] [${button}]`);
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
