import React, { Component, PropTypes } from 'react';
import {
  ActivityIndicator, Image, ScrollView, StyleSheet, Text, TouchableOpacity, View,
} from 'react-native';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import FeatherIcon from 'react-native-vector-icons/Feather';
import styled from 'styled-components/native';

const SCROLLPADDING = 8;
const TRAYITEMHEIGHT = 80;
const cloudIcon = (<MaterialIcon name="cloud" size={18} color="white" />)
const cloudOff = (<MaterialIcon name="cloud-off" size={18} color="rgba(255,255,255,0.8)" />)

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
  flex-direction: row;
  align-items: flex-start;
  justify-content: flex-end;
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
  height: ${TRAYITEMHEIGHT + SCROLLPADDING*2}px;
`;

const IconContainer = styled.View`
  align-items: center;
  justify-content: center;
  margin-top: 4px;
  margin-right: 4px;
  width: 32px;
  height: 32px;
  border-radius: 16px;
  background-color: 'rgba(0,0,0,0.3)';
`;

const styles = StyleSheet.create({
  itemScroller: {
    paddingVertical: SCROLLPADDING,
    paddingRight: SCROLLPADDING,
  },
});

class CameraTray extends Component {

  renderTrayIconFromData = (trayItem) => {
    return (
      <TouchableOpacity
        onPress={() => { this.props.onSelectTrayItem(trayItem); }}
      >
        <ImageTrayItem
          source={{ uri: trayItem.url }}
          resizeMode="cover"
        >
          <IconContainer>
            {
              trayItem.uploaded ?
                cloudIcon : cloudOff
            }
          </IconContainer>
        </ImageTrayItem>
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
