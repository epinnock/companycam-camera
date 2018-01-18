import React, { Component, PropTypes } from 'react';
import {
	NativeModules,
	requireNativeComponent,
	View,
	AsyncStorage,
	Dimensions,
} from 'react-native';
import CameraLayout from './camera-layout';
import CameraLayoutTablet from './camera-layout-tablet';
import { isTablet, deviceSupportsARCam } from './device-info-helper';

const CameraModule =
	NativeModules.CCCameraModuleIOS || NativeModules.CCCameraModule;

const normalizePhotoOrigin = photoOrigin => {
	const validPhotoOrigin =
		photoOrigin === 'STANDARD_CAMERA' ||
		photoOrigin === 'STANDARD_CAMERA_FASTCAM' ||
		photoOrigin === 'STANDARD_CAMERA_DOCSCAN';
	return validPhotoOrigin ? photoOrigin : 'STANDARD_CAMERA';
};

class CCCamera extends React.Component {
	static constants = {
		FlashMode: CameraModule.FlashMode, // off, on, auto, torch
		CameraMode: CameraModule.CameraMode, // fastcam, photo, scanner
		ResolutionMode: CameraModule.ResolutionMode, // normal, high, super
	};

	constructor(props) {
		super(props);

		this.state = {
			hasFlash: false,
		};
	}

	componentDidMount() {
		console.disableYellowBox = true;
	}

	_onFlashAvailabilityChange = event => {
		const { hasFlash } = event.nativeEvent;
		console.log(
			`_onFlashAvailabilityChange called in cccamera.js (${hasFlash})`
		);

		this.setState({ hasFlash });
	};

	_onClose = event => {
		console.log('_onClose called in cccamera.js');
		if (!this.props.onClose) {
			return;
		}

		const errmsg = event.nativeEvent.errmsg;
		const button = event.nativeEvent.button;
		this.props.onClose(errmsg, button);
	};

	_onPhotoAccepted = event => {
		if (!this.props.onPhotoAccepted) {
			return;
		}

		const { filename, imgWidth, imgHeight, photoOrigin } = event.nativeEvent;
		const origin = normalizePhotoOrigin(photoOrigin);

		console.log(
			`_onPhotoAccepted called in cccamera.js (dims: ${imgWidth}x${imgHeight}, origin: ${origin})`
		);
		this.props.onPhotoAccepted(filename, [imgWidth, imgHeight], origin);
	};

	_onPhotoTaken = event => {
		if (!this.props.onPhotoTaken) {
			return;
		}

		const { filename, imgWidth, imgHeight, photoOrigin } = event.nativeEvent;
		const origin = normalizePhotoOrigin(photoOrigin);

		console.log(
			`_onPhotoTaken called in cccamera.js (dims: ${imgWidth}x${imgHeight}, origin: ${origin})`
		);
		this.props.onPhotoTaken(filename, [imgWidth, imgHeight], origin);
	};

	_onAuxModeClicked = () => {
		console.log('_onAuxModeClicked called in cccamera.js');
		if (!this.props.onAuxModeClicked) {
			return;
		}

		this.props.onAuxModeClicked();
	};

	render() {
		const { cameraOpts } = this.props;
		const CCCameraLayout = isTablet ? CameraLayoutTablet : CameraLayout;

		console.log(cameraOpts);

		return (
			<RNCCCamera
				{...this.props}
				hideNativeUI
				onClose={this._onClose}
				onPhotoAccepted={this._onPhotoAccepted}
				onPhotoTaken={this._onPhotoTaken}
				onFlashAvailabilityChange={this._onFlashAvailabilityChange}
				flashMode={cameraOpts.flashMode}
				cameraMode={cameraOpts.cameraMode}
				resolutionMode={cameraOpts.resolutionMode}
			>
				{!this.props.hideCameraLayout && (
					<CCCameraLayout
						cameraOpts={cameraOpts}
						cameraConstants={constants}
						updateCameraOpts={this.props.updateCameraOpts}
						onClose={(errmsg, button) => {
							if (!this.props.onClose) {
								return;
							}
							this.props.onClose(errmsg, button);
						}}
						flipCamera={() => CameraModule.flipCamera()}
						hasFlash={this.state.hasFlash}
						projectName={cameraOpts.projectName}
						orientation={cameraOpts.orientation}
						deviceSupportsARCam={deviceSupportsARCam()}
						cameraTrayData={this.props.cameraTrayData}
						cameraTrayVisible={this.props.cameraTrayVisible}
						onSelectTrayItem={this.props.onSelectTrayItem}
						setCameraTrayVisible={this.props.setCameraTrayVisible}
						arModePress={this.props.arModePress}
						baModePress={this.props.baModePress}
						captureButtonPress={() => {
							CameraModule.capture();
							this.props.captureButtonPress();
						}}
					/>
				)}
				{this.props.children}
			</RNCCCamera>
		);
	}
}

CCCamera.propTypes = {
	storagePath: PropTypes.string,
	hideNativeUI: PropTypes.bool,
	hideCameraLayout: PropTypes.bool,

	cameraOpts: PropTypes.shape({
		projectName: PropTypes.string,
		projectAddress: PropTypes.string,
		exifLat: PropTypes.number,
		exifLon: PropTypes.number,
		exifLocTimestamp: PropTypes.number,
		hideCameraLayout: PropTypes.bool,
		orientation: PropTypes.number,
		flashMode: PropTypes.number,
		cameraMode: PropTypes.number,
		resolutionMode: PropTypes.number,
	}),

	onClose: PropTypes.func,
	onPhotoAccepted: PropTypes.func,
	onPhotoTaken: PropTypes.func,
	onFlashAvailabilityChange: PropTypes.func,
	...View.propTypes,

	arModePress: PropTypes.func,
	baModePress: PropTypes.func,
	captureButtonPress: PropTypes.func,

	cameraTrayData: PropTypes.array,
	cameraTrayVisible: PropTypes.bool,
	onSelectTrayItem: PropTypes.func,
	setCameraTrayVisible: PropTypes.func,

	// duplicated from above to make RN happy. not happy myself
	projectName: PropTypes.string,
	projectAddress: PropTypes.string,
	exifLat: PropTypes.number,
	exifLon: PropTypes.number,
	exifLocTimestamp: PropTypes.number,
	orientation: PropTypes.number,
	flashMode: PropTypes.number,
	cameraMode: PropTypes.number,
	resolutionMode: PropTypes.number,
};

CCCamera.defaultProps = {
	hideNativeUI: true,

	cameraOpts: {
		projectName: '',
		projectAddress: '',
		exifLat: 0,
		exifLon: 0,
		exifLocTimestamp: 0,
		hideCameraLayout: false,
		orientation: 0,
		flashMode: CameraModule.FlashMode.off,
		cameraMode: CameraModule.CameraMode.fastcam,
		resolutionMode: CameraModule.ResolutionMode.normal,
	},

	arModePress: () => {},
	baModePress: () => {},
	captureButtonPress: () => {},

	cameraTrayData: [],
	cameraTrayVisible: false,
	onSelectTrayItem: () => {},
	setCameraTrayVisible: () => {},
};

export const constants = CCCamera.constants;

const RNCCCamera = requireNativeComponent('CompanyCamCamera', CCCamera);

export default CCCamera;
