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
const retryIcon = (
  <MaterialIcon name="sync-problem" size={12} color="white" />
);
const pencilIcon = (
  <MaterialIcon name="mode-edit" size={12} style={{marginLeft: 8 }} color="white" />
);

const ImageTrayActionBar = styled.View`
  flex-direction: row;
  justify-content: space-between;
  padding: 0px 16px 8px 16px;
  ${''/* background-color: #37474F; */}
  ${''/* background-color: rgba(55,71,79, 0.5); */}
`;

const ImageTrayFileControl = styled.View`
  flex-direction: row;
  alignItems: center;
  justifyContent: space-between;
  padding: 8px 16px;
  background-color: rgba(38,50,56, 0.5);
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
  background-color: ${props => props.waiting ? 'rgba(255,193,7,0.8)' : 'rgba(0,0,0,0.5)'};
`;

const styles = StyleSheet.create({
  iconContainer: {
    flex: 1,
    justifyContent: 'space-between',
    padding: 4,
  },
  iconTray: {
    alignSelf: 'flex-end',
  },
  iconTrayPill: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    height: 18,
    borderTopLeftRadius: 9,
    borderBottomLeftRadius: 9,
    borderTopRightRadius: 9,
    borderBottomRightRadius: 9,
    paddingHorizontal: 4,
    paddingVertical: 2,
    backgroundColor: 'rgba(0,0,0,0.5)',
  },
  itemScroller: {
    paddingVertical: SCROLLPADDING,
    paddingRight: SCROLLPADDING,
    backgroundColor: 'rgba(38,50,56, 0.5)',
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
      <View style={styles.iconTray}>
        { trayItemIcons.length === 1 ?
          <IconContainerCircle>
            {trayItemIcons}
          </IconContainerCircle> :
          <View style={styles.iconTrayPill}>
            {trayItemIcons}
          </View>
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
          <View style={styles.iconContainer}>
            {/* retryIcon container */}
            <View>
              {
                !trayItem.uploaded ?
                <IconContainerCircle waiting>
                  {retryIcon}
                </IconContainerCircle> :
                null
              }
            </View>
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

        <ImageTrayActionBar>
          <TouchableOpacity
            onPress={this.props.onHideTray}
            style={{ flexDirection: 'row', alignItems: 'center' }}
          >
            {chevronDown}
            <Text style={{ color: 'white' }}>Hide</Text>
          </TouchableOpacity>

          {/* TODO will be used for scanner mode */}
          {/* <TouchableOpacity
            onPress={() => {}}
          >
            <Text style={{ color: 'white' }}>Done</Text>
          </TouchableOpacity> */}
        </ImageTrayActionBar>

        {/* TODO will be used for scanner mode */}
        {
          this.props.pdfTitleVisible ?
            <ImageTrayFileControl>
              <TouchableOpacity
                onPress={() => {}}
                style={{ flexDirection: 'row', alignItems: 'center' }}
              >
                <Text style={{ color: 'rgba(255,255,255,0.8)' }}>SomeFileName.pdf</Text>
                {pencilIcon}
              </TouchableOpacity>
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

          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.itemScroller}
          >
            {trayItems.map(this.renderTrayImageFromData)}
          </ScrollView>
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
