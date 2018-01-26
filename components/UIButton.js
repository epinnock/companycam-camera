import React from 'react';
import {
  TouchableHighlight, Image, StyleSheet, View,
} from 'react-native';

const BUTTON_SIZE = 44;

const styles = StyleSheet.create({
  uiButton: {
    justifyContent: 'center',
    alignItems: 'center',
    width: BUTTON_SIZE,
    height: BUTTON_SIZE,
    borderRadius: BUTTON_SIZE / 2,
    backgroundColor: 'transparent',
    margin: 16,
  },
});

const UIButton = (props) => {
  // empty is mostly used when we still want the button to take up 
  // the space but not actually function or show anything...
  if (props.empty) {
    return (
      <View style={styles.uiButton} />
    )
  }

  return (
    <TouchableHighlight
      hitSlop={{ top: 10, right: 10, bottom: 10, left: 10 }}
      onPress={props.onPress}
      style={[
        styles.uiButton,
        { backgroundColor: props.bgColor },
        props.style,
      ]}
      underlayColor={props.tapColor}
    >
      <View>
        {props.icon &&
          <Image source={props.icon} />
        }
        {props.children}
      </View>
    </TouchableHighlight>
  );
}

UIButton.defaultProps = {
  tapColor: 'rgba(0,0,0,0.2)',
}

UIButton.propTypes = {
  empty: React.PropTypes.bool,
  icon: React.PropTypes.number,
  style: React.PropTypes.any,
  bgColor: React.PropTypes.string,
  tapColor: React.PropTypes.string,
}

export default UIButton;