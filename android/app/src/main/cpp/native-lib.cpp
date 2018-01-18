#include <jni.h>
#include <sstream>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "docscan.hpp"
#include "imageproc.hpp"

extern "C" {

JNIEXPORT void JNICALL
Java_com_newcam_jniexports_JNIExports_convertYUVtoBGRA(
    JNIEnv *env, jobject thiz,
    /* Input image */
    jint width, jint height, jbyteArray imageYUV,
    /* Output image */
    jintArray imageBGRA)
{
    // Get pointers to array data
    jbyte *_imageYUV = env->GetByteArrayElements(imageYUV, 0);
    jint *_imageBGRA = env->GetIntArrayElements(imageBGRA, 0);

    // Prepare mats which have given arrays as underlying data
    cv::Mat matYUV(height + height / 2, width, CV_8UC1, (unsigned char *) _imageYUV);
    cv::Mat matBGRA(height, width, CV_8UC4, (unsigned char *) _imageBGRA);

    // Convert YUV -> BGRA (to BGR, but with '4' as target num channels)
    cvtColor(matYUV, matBGRA, CV_YUV420sp2BGR, 4);

    // Release array data
    env->ReleaseByteArrayElements(imageYUV, _imageYUV, 0);
    env->ReleaseIntArrayElements(imageBGRA, _imageBGRA, 0);
}

JNIEXPORT void JNICALL
Java_com_newcam_jniexports_JNIExports_magicColor(
    JNIEnv *env, jobject thiz,
    /* Input image */
    jint width, jint height, jintArray imageInputBGRA,
    /* Output image */
    jintArray imageOutputBGRA)
{
    // Get pointers to array data
    jint *_imageInputBGRA = env->GetIntArrayElements(imageInputBGRA, 0);
    jint *_imageOutputBGRA = env->GetIntArrayElements(imageOutputBGRA, 0);

    // Convert input image to Mat
    cv::Mat matInputBGRA(height, width, CV_8UC4, (unsigned char *) _imageInputBGRA);

    // Main functionality
    //-----------------------------------------------
    cv::Mat matResult = imageproc::magicColor(matInputBGRA);

    cv::Mat matOutput(matResult.rows, matResult.cols, CV_8UC4, (unsigned char *) _imageOutputBGRA);
    matResult.copyTo(matOutput);
    //-----------------------------------------------

    // Release array data
    env->ReleaseIntArrayElements(imageInputBGRA, _imageInputBGRA, 0);
    env->ReleaseIntArrayElements(imageOutputBGRA, _imageOutputBGRA, 0);
}

JNIEXPORT jlong JNICALL
Java_com_newcam_jniexports_JNIExports_newScanner(JNIEnv *env, jobject thiz)
{
    DocScanner* docScanPtr = new DocScanner();
    return (jlong) docScanPtr;
}

JNIEXPORT void JNICALL
Java_com_newcam_jniexports_JNIExports_deleteScanner(JNIEnv *env, jobject thiz, jlong ptr)
{
    DocScanner* docScanPtr = (DocScanner*)ptr;
    delete docScanPtr;
}

JNIEXPORT void JNICALL
Java_com_newcam_jniexports_JNIExports_resetScanner(JNIEnv *env, jobject thiz, jlong ptr)
{
    DocScanner* docScanPtr = (DocScanner*)ptr;
    docScanPtr->reset();
}

//Basic method for converting YUV->RGB and returning image data:
//https://stackoverflow.com/questions/12695232/using-native-functions-in-android-with-opencv
JNIEXPORT void JNICALL
Java_com_newcam_jniexports_JNIExports_nativeScan(
    JNIEnv *env, jobject thiz,
    jlong ptr,
    /* Image to be scanned */
    jint width, jint height, jintArray imageInputBGRA,
    /* Image returned by the scanner, if any */
    jintArray dimsImageOutput, jint maxOutputPixels, jintArray imageOutputBGRA,
    /* Info about most recent scan */
    jintArray scanStatus, jfloatArray pRectRaw)
{
    if (!ptr) { return; }

    // Get pointers to array data
    jint *_imageInputBGRA = env->GetIntArrayElements(imageInputBGRA, 0);

    jint *_dimsImageOutput = env->GetIntArrayElements(dimsImageOutput, 0);
    jint *_imageOutputBGRA = env->GetIntArrayElements(imageOutputBGRA, 0);

    jint *_scanStatus = env->GetIntArrayElements(scanStatus, 0);
    jfloat *_pRectRaw = env->GetFloatArrayElements(pRectRaw, 0);

    // Convert input image to Mat
    cv::Mat matInputBGRA(height, width, CV_8UC4, (unsigned char *) _imageInputBGRA);

    // Main functionality
    //-----------------------------------------------
    DocScanner* docScanPtr = (DocScanner*)ptr;
    DocScanner::ScanStatus status = docScanPtr->smartScan(matInputBGRA);
    switch(status){
        case DocScanner::UNSTABLE:  _scanStatus[0] = 0; break;
        case DocScanner::STABLE:    _scanStatus[0] = 1; break;
        case DocScanner::DONE:      _scanStatus[0] = 2; break;
    }

    const geom::PerspectiveRect pRect = docScanPtr->getPerspectiveRect();
    _pRectRaw[0] = pRect.p00.x; _pRectRaw[1] = pRect.p00.y;
    _pRectRaw[2] = pRect.p10.x; _pRectRaw[3] = pRect.p10.y;
    _pRectRaw[4] = pRect.p11.x; _pRectRaw[5] = pRect.p11.y;
    _pRectRaw[6] = pRect.p01.x; _pRectRaw[7] = pRect.p01.y;

    if (status == DocScanner::DONE) {
        cv::Mat matScanned = docScanPtr->getOutputImage();
        _dimsImageOutput[0] = matScanned.cols;
        _dimsImageOutput[1] = matScanned.rows;

        if (matScanned.rows*matScanned.cols > maxOutputPixels) {
            //TODO: Image doesn't fit!
        } else {
            cv::Mat matOutput(matScanned.rows, matScanned.cols, CV_8UC4, (unsigned char *) _imageOutputBGRA);
            matScanned.copyTo(matOutput);
        }
    } else {
        _dimsImageOutput[0] = 0;
        _dimsImageOutput[1] = 0;
    }
    //-----------------------------------------------

    // Release array data
    env->ReleaseIntArrayElements(imageInputBGRA, _imageInputBGRA, 0);

    env->ReleaseIntArrayElements(dimsImageOutput, _dimsImageOutput, 0);
    env->ReleaseIntArrayElements(imageOutputBGRA, _imageOutputBGRA, 0);

    env->ReleaseIntArrayElements(scanStatus, _scanStatus, 0);
    env->ReleaseFloatArrayElements(pRectRaw, _pRectRaw, 0);
}

}