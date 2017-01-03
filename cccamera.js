import React, { Component, PropTypes } from 'react';
import { 
  requireNativeComponent,
  View,
} from 'react-native';

class CCCamera extends React.Component {

  constructor(props){
    super(props);
    this._onClose = this._onClose.bind(this);
    this._photoAccepted = this._photoAccepted.bind(this);
    this._photoTaken = this._photoTaken.bind(this);
  }
  
  _onClose(event) {
    if(!this.props.onClose){ return; }
    
    const errmsg = event.nativeEvent.errmsg;
    const button = event.nativeEvent.button;
    this.props.onClose(errmsg, button);
  }
  
  _photoAccepted(event) {
    if(!this.props.photoAccepted){ return; }
    
    const { filename, imgWidth, imgHeight } = event.nativeEvent;
    this.props.photoAccepted(filename, [imgWidth, imgHeight]);
  }
  
  _photoTaken(event) {
    if(!this.props.photoTaken){ return; }
    
    const { filename, imgWidth, imgHeight } = event.nativeEvent;
    this.props.photoTaken(filename, [imgWidth, imgHeight]);
  }
  
  render() {
    return (
      <RNCCCamera 
        {...this.props} 
        onClose={this._onClose} 
        photoAccepted={this._photoAccepted}
        photoTaken={this._photoTaken}
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
  
  onClose: PropTypes.func,
  photoAccepted: PropTypes.func,
  photoTaken: PropTypes.func,
  ...View.propTypes
};

const RNCCCamera = requireNativeComponent('CompanyCamCamera', CCCamera);

export default CCCamera;
