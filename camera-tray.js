import React, { Component, PropTypes } from 'react';
import {
  ScrollView, StyleSheet, Text, TouchableOpacity, Platform,
} from 'react-native';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import styled from 'styled-components/native';

const SCROLLPADDING = 8;
const TRAYITEMHEIGHT = 80;
const cloudIcon = (
  <MaterialIcon name="cloud-queue" size={18} color="white" />
);
const cloudOff = (
  <MaterialIcon name="cloud-off" size={18} color="rgba(255,255,255,0.8)" />
);

const ImageTray = styled.View`
  ${''/* background-color: #263238; */}
  background-color: rgba(38,50,56, 0.5);
`;

const ImageTrayActionBar = styled.View`
  flex-direction: row;
  justify-content: space-between;
  padding-top: 8px;
  padding-right: 16px;
  padding-bottom: 8px;
  padding-left: 16px;
  ${''/* background-color: #37474F; */}
  background-color: rgba(55,71,79, 0.5);
`;

const ImageTrayItem = styled.Image`
  flex-direction: row;
  align-items: flex-start;
  justify-content: flex-end;
  height: ${TRAYITEMHEIGHT}px;
  width: ${TRAYITEMHEIGHT}px;
  margin-left: 8px;
  border-width: ${props => props.active ? '2px' : '0'};
  border-color: ${props => props.active ? 'white' : 'transparent'};
`;

const EmptyStateContent = styled.View`
  align-items: center;
  justify-content: center;
  height: ${TRAYITEMHEIGHT + SCROLLPADDING * 2}px;
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
  emptyStateText: {
    textAlign: 'center',
    color: 'white',
    fontSize: 17,
    backgroundColor: 'transparent',
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
          style={{ borderRadius: Platform === 'ios' ? 4 : 0 }}
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
    if (!this.props.visible) { return null; }

    const { trayItems, emptyText } = this.props;

    const trayIconsEmpty = !trayItems || (trayItems.length === 0);

    return (
      <ImageTray>

        <ImageTrayActionBar>
          <TouchableOpacity
            onPress={this.props.onHideTray}
          >
            <Text style={{ color: 'white' }}>Hide Tray</Text>
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => {}}
          >
            <Text style={{ color: 'white' }}>Done</Text>
          </TouchableOpacity>
        </ImageTrayActionBar>

        {trayIconsEmpty ? (

          <EmptyStateContent>
            <Text style={styles.emptyStateText}>
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
  visible: PropTypes.bool,
  doneButtonVisible: PropTypes.bool,
  trayItems: PropTypes.object,
  emptyText: PropTypes.string,
  onSelectTrayItem: PropTypes.func,
  onHideTray: PropTypes.func,
};

CameraTray.defaultProps = {
  visible: true,
  doneButtonVisible: false,
  trayItems: [],
  emptyText: '',
  onSelectTrayItem: () => {},
  onHideTray: () => {},
};

export default CameraTray;
