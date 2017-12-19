import React, { Component, PropTypes } from 'react';
import {
  Image, ScrollView, StyleSheet, Text, TouchableOpacity, View,
} from 'react-native';

import styled from 'styled-components/native';

const TRAYITEMHEIGHT = 80;

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
  height: ${TRAYITEMHEIGHT}px;
  width: ${TRAYITEMHEIGHT}px;
  margin-left: 8px;
  border-radius: 4px;
  border-width: ${props => props.active ? '2px' : '0'};
  border-color: ${props => props.active ? 'white' : 'transparent'};
`;

const EmptyStateContent = styled.View`
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

  static renderTrayIconFromData(data) {
    return (
      <TouchableOpacity
        onPress={() => {}}
      >
        {/*<Text>
          {data.uploaded ? 'DONE' : 'PENDING'}
        </Text>*/}
        <ImageTrayItem
          source={{ uri: data.url }}
          resizeMode="cover"
        />
      </TouchableOpacity>
    );
  }

  render() {
    const { imageData, emptyText } = this.props;

    const trayIconsEmpty = !imageData || (imageData.length === 0);

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

        {trayIconsEmpty ? (

          <EmptyStateContent>
            <Text style={{ color: 'white', fontSize: 17, backgroundColor: 'transparent' }}>
              {emptyText}
            </Text>
          </EmptyStateContent>

        ) : (

          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.itemScroller}
          >
            {imageData.map(CameraTray.renderTrayIconFromData)}
          </ScrollView>

        )}

      </ImageTray>
    );
  }
}

CameraTray.propTypes = {
  imageData: PropTypes.object,
  emptyText: PropTypes.string,
};

export default CameraTray;
