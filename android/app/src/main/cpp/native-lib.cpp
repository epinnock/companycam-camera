#include <jni.h>
#include <sstream>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "docscan.hpp"

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_newcam_imageprocessing_DocScanOpenCV_stringFromJNI(JNIEnv *env, jobject thiz) {

    int x = 9;

    cv::Mat mat;

    DocScanner docScan;

    std::ostringstream oss;
    oss << "Hello 2 " << x;

    return env->NewStringUTF(oss.str().c_str());
}

//TODO from OpenCV sample
//https://stackoverflow.com/questions/12695232/using-native-functions-in-android-with-opencv
//----------------------------------
JNIEXPORT jlong JNICALL
Java_com_newcam_imageprocessing_DocScanOpenCV_newScanner(JNIEnv *env, jobject thiz)
{
    DocScanner* docScanPtr = new DocScanner();
    return (jlong) docScanPtr;
}

JNIEXPORT void JNICALL
Java_com_newcam_imageprocessing_DocScanOpenCV_deleteScanner(JNIEnv *env, jobject thiz,
                                                            jlong ptr)
{
    DocScanner* docScanPtr = (DocScanner*)ptr;
    delete docScanPtr;
}

JNIEXPORT void JNICALL
Java_com_newcam_imageprocessing_DocScanOpenCV_nativeScan(JNIEnv *env, jobject thiz,
                                                         jlong ptr,
                                                         jint width,
                                                         jint height,
                                                         jbyteArray yuv,
                                                         jintArray bgra)
{
    if (!ptr) { return; }

    jbyte *_yuv = env->GetByteArrayElements(yuv, 0);
    jint *_bgra = env->GetIntArrayElements(bgra, 0);

    cv::Mat myuv(height + height / 2, width, CV_8UC1, (unsigned char *) _yuv);
    cv::Mat mbgra(height, width, CV_8UC4, (unsigned char *) _bgra);

    //Please make attention about BGRA byte order
    //ARGB stored in java as int array becomes BGRA at native level
    cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);

    DocScanner* docScanPtr = (DocScanner*)ptr;

    //geom::PerspectiveRect pRect = docScanPtr->scan(mbgra, false);
    docScanPtr->scan(mbgra, true);

    cv::Mat imageResult = docScanPtr->getOutputImage();
    if(imageResult.rows > 0 && imageResult.cols > 0) {
        int copyrows = imageResult.rows < mbgra.rows ? imageResult.rows : mbgra.rows;
        int copycols = imageResult.cols < mbgra.cols ? imageResult.cols : mbgra.cols;
        imageResult.copyTo(mbgra.rowRange(0, copyrows).colRange(0, copycols));
    }

    env->ReleaseIntArrayElements(bgra, _bgra, 0);
    env->ReleaseByteArrayElements(yuv, _yuv, 0);
}
//----------------------------------

}