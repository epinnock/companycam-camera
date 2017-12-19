import React, { Component, PropTypes } from 'react';
import {
  Image, ScrollView, StyleSheet, Text, TouchableOpacity, View,
} from 'react-native';

import styled from 'styled-components/native';

// const TRAYITEMHEIGHT = 80;

const ImageTray = styled.View`
  background-color: #263238;
`;

const ImageTrayActionBar = styled.View`
  flex-direction: row;
  justify-content: space-between;
  padding-top: 8px;
  padding-right: 16px;
  padding-bottom: 8px;
  padding-left: 16px;
  background-color: #37474F;
`;

const ImageTrayItem = styled.Image`
  height: 80px;
  width: 80px;
  margin-left: 8px;
  border-radius: 4px;
  border-width: ${props => props.active ? '2px' : '0'};
  border-color: ${props => props.active ? 'white' : 'transparent'};
`;

const ScanTip = styled.Image`
  align-items: center;
  justify-content: center;
  height: 96px;
`;

const styles = StyleSheet.create({
  itemScroller: {
    paddingVertical: 8,
    paddingRight: 8,
  },
});

class CameraTray extends Component {
  render() {
    return (
      <ImageTray>

        <ImageTrayActionBar>
          <TouchableOpacity
            onPress={() => {}}
          >
            <Text style={{ color: 'white' }}>Cancel</Text>
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => {}}
          >
            <Text style={{ color: 'white' }}>Done</Text>
          </TouchableOpacity>
        </ImageTrayActionBar>

        {/* <ScanTip>
          <Text style={{ color: 'white', fontSize: 17, backgroundColor: 'transparent' }}>
            {`Fit document inside screen.\nPlace on contrasting background.`}
          </Text>
        </ScanTip> */}

        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.itemScroller}
        >
          <TouchableOpacity
            onPress={() => {}}
          >
            <ImageTrayItem
              source={{uri: 'https://picsum.photos/640/1136/?image=0'}}
              resizeMode='cover'
            />
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => {}}
          >
            <ImageTrayItem
              source={{uri: 'https://picsum.photos/640/1136/?image=20'}}
              resizeMode='cover'
            />
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => {}}
          >
            <ImageTrayItem
              source={{uri: 'https://picsum.photos/640/1136/?image=40'}}
              resizeMode='cover'
            />
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => {}}
          >
            <ImageTrayItem
              source={{uri: 'https://picsum.photos/640/1136/?image=60'}}
              resizeMode='cover'
            />
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => {}}
          >
            <ImageTrayItem
              source={{uri: 'https://picsum.photos/640/1136/?image=80'}}
              resizeMode='cover'
            />
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => {}}
          >
            <ImageTrayItem
              source={{uri: 'https://picsum.photos/640/1136/?image=100'}}
              resizeMode='cover'
            />
          </TouchableOpacity>
        </ScrollView>

      </ImageTray>
    );
  }
}

export default CameraTray;
