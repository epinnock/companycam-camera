#include <jni.h>
#include <sstream>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "docscan.hpp"

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_newcam_imageprocessing_DocScanOpenCV_newScanner(JNIEnv *env, jobject thiz)
{
    DocScanner* docScanPtr = new DocScanner();
    return (jlong) docScanPtr;
}

JNIEXPORT void JNICALL
Java_com_newcam_imageprocessing_DocScanOpenCV_deleteScanner(JNIEnv *env, jobject thiz, jlong ptr)
{
    DocScanner* docScanPtr = (DocScanner*)ptr;
    delete docScanPtr;
}

JNIEXPORT void JNICALL
Java_com_newcam_imageprocessing_DocScanOpenCV_resetScanner(JNIEnv *env, jobject thiz, jlong ptr)
{
    DocScanner* docScanPtr = (DocScanner*)ptr;
    docScanPtr->reset();
}

//Basic method for converting YUV->RGB and returning image data:
//https://stackoverflow.com/questions/12695232/using-native-functions-in-android-with-opencv
JNIEXPORT void JNICALL
Java_com_newcam_imageprocessing_DocScanOpenCV_nativeScan(
    JNIEnv *env, jobject thiz,
    jlong ptr,
    /* Image to be scanned */
    jint width, jint height, jbyteArray imageYUV, jintArray imageBGRA,
    /* Image returned by the scanner, if any */
    jbooleanArray didGenerateOutput, jintArray dimsImageOutput, jint maxWidth, jint maxHeight, jintArray imageOutput,
    /* Info about most recent scan */
    jintArray scanStatus, jfloatArray pRectRaw)
{
    if (!ptr) { return; }

    jbyte *_imageYUV = env->GetByteArrayElements(imageYUV, 0);
    jint *_imageBGRA = env->GetIntArrayElements(imageBGRA, 0);

    jboolean *_didGenerateOutput = env->GetBooleanArrayElements(didGenerateOutput, 0);
    jint *_dimsImageOutput = env->GetIntArrayElements(dimsImageOutput, 0);
    jint *_imageOutput = env->GetIntArrayElements(imageOutput, 0);

    jint *_scanStatus = env->GetIntArrayElements(scanStatus, 0);
    jfloat *_pRectRaw = env->GetFloatArrayElements(pRectRaw, 0);

    // Convert input image to Mat
    cv::Mat matYUV(height + height / 2, width, CV_8UC1, (unsigned char *) _imageYUV);
    cv::Mat matBGRA(height, width, CV_8UC4, (unsigned char *) _imageBGRA);

    //Please make attention about BGRA byte order
    //ARGB stored in java as int array becomes BGRA at native level
    cvtColor(matYUV, matBGRA, CV_YUV420sp2BGR, 4);

    DocScanner* docScanPtr = (DocScanner*)ptr;
    docScanPtr->scan(matBGRA, false);

    const geom::PerspectiveRect pRect = docScanPtr->getPerspectiveRect();
    _pRectRaw[0] = pRect.p00.x; _pRectRaw[1] = pRect.p00.y;
    _pRectRaw[2] = pRect.p10.x; _pRectRaw[3] = pRect.p10.y;
    _pRectRaw[4] = pRect.p11.x; _pRectRaw[5] = pRect.p11.y;
    _pRectRaw[6] = pRect.p01.x; _pRectRaw[7] = pRect.p01.y;

    /*cv::Mat imageResult = docScanPtr->getOutputImage();
    if(imageResult.rows > 0 && imageResult.cols > 0) {
        int copyrows = imageResult.rows < matBGRA.rows ? imageResult.rows : matBGRA.rows;
        int copycols = imageResult.cols < matBGRA.cols ? imageResult.cols : matBGRA.cols;
        imageResult.copyTo(matBGRA.rowRange(0, copyrows).colRange(0, copycols));
    }*/

    env->ReleaseByteArrayElements(imageYUV, _imageYUV, 0);
    env->ReleaseIntArrayElements(imageBGRA, _imageBGRA, 0);

    env->ReleaseBooleanArrayElements(didGenerateOutput, _didGenerateOutput, 0);
    env->ReleaseIntArrayElements(dimsImageOutput, _dimsImageOutput, 0);
    env->ReleaseIntArrayElements(imageOutput, _imageOutput, 0);

    env->ReleaseIntArrayElements(scanStatus, _scanStatus, 0);
    env->ReleaseFloatArrayElements(pRectRaw, _pRectRaw, 0);
}

}