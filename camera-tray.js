import React, { Component, PropTypes } from 'react';
import {
  Platform, ScrollView, StyleSheet, Text, TouchableOpacity, View,
} from 'react-native';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import styled from 'styled-components/native';

const SCROLLPADDING = 8;
const TRAYITEMHEIGHT = 80;

const hasCommentsIcon = (
  <MaterialIcon name="chat-bubble" size={11} style={{marginHorizontal: 2}} color="white" />
);
const hasTagsIcon = (
  <MaterialIcon name="local-offer" size={11} style={{marginHorizontal: 2}} color="white" />
);
const gestureIcon = (
  <MaterialIcon name="gesture" size={11} style={{marginHorizontal: 2}} color="white" />
);
const chevronDown = (
  <MaterialIcon name="keyboard-arrow-down" size={18} style={{marginRight: 4}} color="white" />
);
const pencilIcon = (
  <MaterialIcon name="mode-edit" size={14}color="white" />
);
const clearTray = (
  <MaterialIcon name="delete-sweep" size={22}color="white" />
);

const ImageTrayActionBar = styled.View`
  flex-direction: row;
  justify-content: space-between;
  padding: 0px 16px 8px 16px;
  ${''/* background-color: #37474F; */}
  ${''/* background-color: rgba(55,71,79, 0.5); */}
`;

const ImageTrayFileControl = styled.View`
  width: 100%;
  flex-direction: row;
  alignItems: center;
  padding: 16px;
  background-color: rgba(38,50,56, 0.5),
`;

const ImageTrayItem = styled.Image`
  justify-content: space-between;
  height: ${TRAYITEMHEIGHT}px;
  width: ${TRAYITEMHEIGHT}px;
  margin-left: 8px;
`;

const EmptyStateContent = styled.View`
  align-items: center;
  justify-content: center;
  background-color: rgba(38,50,56, 0.5),
  height: ${TRAYITEMHEIGHT + SCROLLPADDING * 2}px;
`;

const IconContainer = styled.View`
  align-items: center;
  justify-content: center;
`;

const IconContainerCircle = IconContainer.extend`
  borderRadius: 9px;
  width: 18px;
  height: 18px;
  background-color: rgba(0,0,0,0.5),
`;

const IconContainerPill = styled.View`
  flex-direction: row;
  align-items: center;
  justify-content: flex-end;
  height: 18px;
  border-top-left-radius: 9px;
  border-bottom-left-radius: 9px;
  border-top-right-radius: 9px;
  border-bottom-right-radius: 9px;
  padding: 2px 4px;
  background-color: rgba(0,0,0,0.5),
`;

const styles = StyleSheet.create({
  iconContainerOverlay: {
    flex: 1,
    justifyContent: 'flex-end',
    padding: 4,
    backgroundColor: 'transparent',
  },
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

  renderIconTray = (trayItem) => {
    const trayItemIcons = [];

    if (trayItem.edited) {
      trayItemIcons.push(gestureIcon);
    }

    if (trayItem.hasComments) {
      trayItemIcons.push(hasCommentsIcon);
    }

    if (trayItem.hasTags) {
      trayItemIcons.push(hasTagsIcon);
    }

    if (trayItemIcons.length === 0) {
      return null;
    }

    return (
      <View style={{ alignSelf: 'flex-end' }}>
        { trayItemIcons.length === 1 ?
          <IconContainerCircle>
            {trayItemIcons}
          </IconContainerCircle> :
          <IconContainerPill>
            {trayItemIcons}
          </IconContainerPill>
        }
      </View>
    );
  }


  renderTrayImageFromData = (trayItem) => {
    return (
      <TouchableOpacity
        onPress={() => { this.props.onSelectTrayItem(trayItem); }}
      >
        <ImageTrayItem
          failure={ !trayItem.uploaded }
          source={{ uri: trayItem.url }}
          style={{ borderRadius: Platform === 'ios' ? 4 : 0 }}
          resizeMode="cover"
        >
          <View style={styles.iconContainerOverlay}>
            {/* edits, comments, tags */}
            {this.renderIconTray(trayItem)}
          </View>
        </ImageTrayItem>
      </TouchableOpacity>
    );
  }

  render() {
    if (!this.props.visible) { return null; }

    const { trayItems, emptyText } = this.props;

    const trayIconsEmpty = !trayItems || (trayItems.length === 0);

    return (
      <View>

        {/* TODO most likely remove this section */}
        {/* <ImageTrayActionBar>
          <TouchableOpacity
            onPress={this.props.onHideTray}
            style={{ flexDirection: 'row', alignItems: 'center' }}
          >
            {chevronDown}
            <Text style={{ color: 'white' }}>Hide</Text>
          </TouchableOpacity>
        </ImageTrayActionBar> */}

        {/* TODO will be used for scanner mode */}
        {
          this.props.pdfTitleVisible ?
            <ImageTrayFileControl>
              <TouchableOpacity
                onPress={() => {}}
                style={{ flex: 1, flexDirection: 'row', alignItems: 'center' }}
              >
                {pencilIcon}
                <Text
                  style={{
                    color: 'rgba(255,255,255,0.8)',
                    marginLeft: 4,
                  }}
                  numberOfLines={2}
                >
                  New Document Name
                </Text>

              </TouchableOpacity>
              <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                <TouchableOpacity
                  style={{
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: 44,
                    height: 44,
                    marginHorizontal: 4,
                  }}
                >
                  {clearTray}
                </TouchableOpacity>
                <TouchableOpacity
                  style={{
                    backgroundColor: 'white',
                    paddingHorizontal: 16,
                    paddingVertical: 8,
                    borderRadius: 20,
                  }}
                >
                  <Text style={{ color: 'black', fontWeight: 'bold' }}>Finish</Text>
                </TouchableOpacity>
              </View>
            </ImageTrayFileControl>
            : null
        }

        {trayIconsEmpty ? (

          <EmptyStateContent>
            <Text style={styles.emptyStateText}>
              {emptyText}
            </Text>
          </EmptyStateContent>

        ) : (

          <View style={{ backgroundColor: 'rgba(38,50,56, 0.5)' }}>
            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={styles.itemScroller}
            >
              {trayItems.map(this.renderTrayImageFromData)}
            </ScrollView>
          </View>
        )}
      </View>
    );
  }
}

CameraTray.propTypes = {
  visible: PropTypes.bool,
  pdfTitleVisible: PropTypes.bool,
  trayItems: PropTypes.array,
  emptyText: PropTypes.string,
  onSelectTrayItem: PropTypes.func,
  onHideTray: PropTypes.func,
};

CameraTray.defaultProps = {
  visible: true,
  pdfTitleVisible: false,
  trayItems: [],
  emptyText: '',
  onSelectTrayItem: () => {},
  onHideTray: () => {},
};

export default CameraTray;
