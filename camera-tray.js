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

  renderTrayIconFromData = (trayItem) => {
    return (
      <TouchableOpacity
        onPress={() => { this.props.onSelectTrayItem(trayItem); }}
      >
        {/*<Text>
          {trayItem.uploaded ? 'DONE' : 'PENDING'}
        </Text>*/}
        <ImageTrayItem
          source={{ uri: trayItem.url }}
          resizeMode="cover"
        />
      </TouchableOpacity>
    );
  }

  render() {
    const { trayItems, emptyText } = this.props;

    const trayIconsEmpty = !trayItems || (trayItems.length === 0);

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
            {trayItems.map(this.renderTrayIconFromData)}
          </ScrollView>

        )}

      </ImageTray>
    );
  }
}

CameraTray.propTypes = {
  trayItems: PropTypes.object,
  emptyText: PropTypes.string,
  onSelectTrayItem: PropTypes.func,
};

CameraTray.defaultProps = {
  trayItems: [],
  emptyText: '',
  onSelectTrayItem: () => {},
};

export default CameraTray;
