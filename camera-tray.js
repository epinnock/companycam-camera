import React, { Component, PropTypes } from 'react';
import {
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  Dimensions,
} from 'react-native';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import styled from 'styled-components/native';

const SCROLLPADDING = 8;
const TRAYITEMHEIGHT = 80;

const hasCommentsIcon = (
  <MaterialIcon
    name="chat-bubble"
    size={11}
    style={{ marginHorizontal: 2 }}
    color="white"
  />
);
const hasTagsIcon = (
  <MaterialIcon
    name="local-offer"
    size={11}
    style={{ marginHorizontal: 2 }}
    color="white"
  />
);
const gestureIcon = (
  <MaterialIcon
    name="gesture"
    size={11}
    style={{ marginHorizontal: 2 }}
    color="white"
  />
);
const chevronDown = (
  <MaterialIcon
    name="keyboard-arrow-down"
    size={18}
    style={{ marginRight: 4 }}
    color="white"
  />
);
const pencilIcon = <MaterialIcon name="mode-edit" size={14} color="white" />;
const clearTray = <MaterialIcon name="delete-sweep" size={22} color="white" />;

const ImageTrayActionBar = styled.View`
  flex-direction: row;
  justify-content: space-between;
  padding: 0px 16px 8px 16px;
  ${'' /* background-color: #37474F; */} ${'' /* background-color: rgba(55,71,79, 0.5); */};
`;

const ImageTrayFileControl = styled.View`
  width: 100%;
  flex-direction: row;
  alignitems: center;
  padding: 16px;
  background-color: rgba(38, 50, 56, 0.5);
`;

const ImageTrayItem = styled.Image`
  justify-content: space-between;
  height: ${TRAYITEMHEIGHT}px;
  width: ${TRAYITEMHEIGHT}px;
  margin: 8px;
`;

const EmptyStateContent = styled.View`
  align-items: center;
  justify-content: center;
  background-color: rgba(38, 50, 56, 0.5);
`;

const IconContainer = styled.View`
  align-items: center;
  justify-content: center;
`;

const IconContainerCircle = IconContainer.extend`
  border-radius: 9px;
  width: 18px;
  height: 18px;
  background-color: rgba(0, 0, 0, 0.5);
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
  background-color: rgba(0, 0, 0, 0.5);
`;

const styles = StyleSheet.create({
  uiButton: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'transparent',
  },
  iconContainerOverlay: {
    flex: 1,
    justifyContent: 'flex-end',
    padding: 4,
    backgroundColor: 'transparent',
  },
  itemScroller: {
    padding: SCROLLPADDING,
  },
  emptyStateText: {
    textAlign: 'center',
    color: 'white',
    fontSize: 17,
    backgroundColor: 'transparent',
  },
  documentNameContainer: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  documentName: {
    color: 'rgba(255,255,255,0.8)',
    marginLeft: 4,
  },
  documentTrayHeaderRightContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  trayAction: {
    backgroundColor: 'white',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    marginLeft: 4,
  },
  trayActionText: {
    color: 'black',
    fontWeight: 'bold',
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
        {trayItemIcons.length === 1 ? (
          <IconContainerCircle>{trayItemIcons}</IconContainerCircle>
        ) : (
          <IconContainerPill>{trayItemIcons}</IconContainerPill>
        )}
      </View>
    );
  };

  renderTrayImageFromData = (trayItem) => {
    return (
      <TouchableOpacity
        style={{
          transform: [
            {
              rotate: this.props.isLandscape ? '90deg' : '0deg',
            },
          ],
        }}
        onPress={() => {
          this.props.onSelectTrayItem(trayItem);
        }}
      >
        <ImageTrayItem
          failure={!trayItem.uploaded}
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
  };

  renderDocumentTrayHeader = () => {
    if (!this.props.documentTrayHeaderVisible) {
      return null;
    }

    const { trayItems, emptyText, setToPhotoMode } = this.props;
    const trayEmpty = !trayItems || trayItems.length === 0;

    return (
      <ImageTrayFileControl>
        <TouchableOpacity
          onPress={() => {}}
          style={styles.documentNameContainer}
        >
          {pencilIcon}
          <Text style={styles.documentName} numberOfLines={2}>
            New Document Name
          </Text>
        </TouchableOpacity>

        <View style={styles.documentTrayHeaderRightContainer}>
          {!trayEmpty && (
            <TouchableOpacity style={styles.uiButton}>
              {clearTray}
            </TouchableOpacity>
          )}
          <TouchableOpacity
            onPress={() => {
              if (trayEmpty) {
                setToPhotoMode();
                return;
              }
            }}
            style={styles.trayAction}
          >
            <Text style={styles.trayActionText}>
              {trayEmpty ? 'Cancel' : 'Finish'}
            </Text>
          </TouchableOpacity>
        </View>
      </ImageTrayFileControl>
    );
  };

  render() {
    if (!this.props.visible) {
      return null;
    }

    const { trayItems, emptyText, isLandscape } = this.props;
    const trayEmpty = !trayItems || trayItems.length === 0;

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

        {this.renderDocumentTrayHeader()}

        {trayEmpty ? (
          <EmptyStateContent
            style={{
              height: isLandscape ? null : TRAYITEMHEIGHT + SCROLLPADDING * 2,
              width: isLandscape ? TRAYITEMHEIGHT + SCROLLPADDING * 2 : null,
              flexGrow: 1,
            }}
          >
            <Text
              style={[
                styles.emptyStateText,
                {
                  transform: [{ rotate: this.props.rotation }],
                  width: Math.min(400, Dimensions.get('window').width),
                },
              ]}
            >
              {emptyText}
            </Text>
          </EmptyStateContent>
        ) : (
          <View
            style={{
              backgroundColor: 'rgba(38,50,56, 0.5)',
              transform: [
                { rotate: this.props.swapCameraUI ? '180deg' : '0deg' },
              ],
              flexGrow: 1,
            }}
          >
            <ScrollView
              horizontal={!isLandscape}
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
  documentTrayHeaderVisible: PropTypes.bool,
  primaryModeIsScan: PropTypes.bool,
  trayItems: PropTypes.array,
  emptyText: PropTypes.string,
  onSelectTrayItem: PropTypes.func,
  onHideTray: PropTypes.func,
  isLandscape: PropTypes.bool,
  rotation: PropTypes.string,
  swapCameraUI: PropTypes.bool,
};

CameraTray.defaultProps = {
  visible: true,
  pdfTitleVisible: false,
  trayItems: [],
  emptyText: '',
  onSelectTrayItem: () => {},
  onHideTray: () => {},
  setToPhotoMode: () => {},
  isLandscape: false,
  rotation: '0deg',
  swapCameraUI: false,
};

export default CameraTray;
