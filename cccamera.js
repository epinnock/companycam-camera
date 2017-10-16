import React, { Component, PropTypes } from 'react';
import {
  requireNativeComponent,
  View,
} from 'react-native';

class CCCamera extends React.Component {

  constructor(props){
    super(props);
  }

  _onClose(event) {
    console.log("_onClose called in cccamera.js");
    if(!this.props.onClose){ return; }

    const errmsg = event.nativeEvent.errmsg;
    const button = event.nativeEvent.button;
    this.props.onClose(errmsg, button);
  }

  _onPhotoAccepted(event) {
    if(!this.props.onPhotoAccepted){ return; }

    const { filename, imgWidth, imgHeight } = event.nativeEvent;
    this.props.onPhotoAccepted(filename, [imgWidth, imgHeight]);
  }

  _onPhotoTaken(event) {
    console.log("_onPhotoTaken called in cccamera.js");
    if(!this.props.onPhotoTaken){ return; }

    const { filename, imgWidth, imgHeight } = event.nativeEvent;
    this.props.onPhotoTaken(filename, [imgWidth, imgHeight]);
  }

  _onAuxModeClicked(event) {
    console.log("_onAuxModeClicked called in cccamera.js");
    if(!this.props.onAuxModeClicked){ return; }

    this.props.onAuxModeClicked();
  }

  render() {
    return (
      <RNCCCamera
        {...this.props}
        onClose={this._onClose.bind(this)}
        onPhotoAccepted={this._onPhotoAccepted.bind(this)}
        onPhotoTaken={this._onPhotoTaken.bind(this)}
        onAuxModeClicked={this._onAuxModeClicked.bind(this)}
      />
    );
  }
}

CCCamera.propTypes = {
  storagePath: PropTypes.string,
  projectName: PropTypes.string,
  projectAddress: PropTypes.string,

  exifLat: PropTypes.number,
  exifLon: PropTypes.number,
  exifLocTimestamp: PropTypes.number,

  auxModeCaption: PropTypes.string,
  onAuxModeClicked: PropTypes.func,

  hideNativeUI: PropTypes.bool,

  onClose: PropTypes.func,
  onPhotoAccepted: PropTypes.func,
  onPhotoTaken: PropTypes.func,
  ...View.propTypes
};

CCCamera.defaultProps = {
  projectName: '',
  projectAddress: '',

  exifLat: 0,
  exifLon: 0,
  exifLocTimestamp: 0,

  auxModeCaption: '',
  onAuxModeClicked: () => {},

  hideNativeUI: false,
};

const RNCCCamera = requireNativeComponent('CompanyCamCamera', CCCamera);

export default CCCamera;
