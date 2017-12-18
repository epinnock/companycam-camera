import React, { Component, PropTypes } from 'react';
import {
  Image, ScrollView, Text, TouchableOpacity, View,
} from 'react-native';

import styled from 'styled-components/native';

const ImageTray = styled.View`
  ${'' /* background-color: #212121; */}
  padding-top: 8px;
  padding-right: 8px;
  padding-bottom: 8px;
  padding-left: 0px;
`;

const ImageTrayItem = styled.Image`
  height: 80px;
  width: 80px;
  margin-left: 8px;
  border-radius: 4px;
  borderWidth: ${props => props.active ? '2' : '0'};
  borderColor: ${props => props.active ? 'white' : 'transparent'};
`;

class CameraTray extends Component {
  render() {
    return (
      <ImageTray>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={{ justifyContent: 'flex-start' }}
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
