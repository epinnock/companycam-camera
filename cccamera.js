import React, { Component, PropTypes } from 'react';
import { 
  requireNativeComponent,
  View,
} from 'react-native';

class CCCamera extends React.Component {

  constructor(props){
    super(props);
    this._onClose = this._onClose.bind(this);
  }
  
  _onClose(event) {
    if(!this.props.onClose){
      return;
    }
    
    const errmsg = event.nativeEvent.errmsg;
    const button = event.nativeEvent.button;
    this.props.onClose(errmsg, button);
  }
  
  render() {
    return <RNCCCamera {...this.props} onClose={this._onClose} />;
  }
}

CCCamera.propTypes = {
  projectName: PropTypes.string,
  projectAddress: PropTypes.string,
  onClose: PropTypes.func,
  ...View.propTypes
};

const RNCCCamera = requireNativeComponent('CompanyCamCamera', CCCamera);

export default CCCamera;
