#include <jni.h>
#include <sstream>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "docscan.hpp"

extern "C"

JNIEXPORT jstring JNICALL
Java_com_newcam_imageprocessing_DocScanOpenCV_stringFromJNI(JNIEnv *env, jobject /* this */) {

    int x = 9;

    cv::Mat mat;

    DocScanner docScan;

    std::ostringstream oss;
    oss << "Hello 2 " << x;

    return env->NewStringUTF(oss.str().c_str());
}

//TODO from OpenCV sample
//https://stackoverflow.com/questions/12695232/using-native-functions-in-android-with-opencv
/*
JNIEXPORT void JNICALL
Java_org_opencv_samples_tutorial3_Sample3View_FindFeatures(JNIEnv* env, jobject thiz, jint width, jint height, jbyteArray yuv, jintArray bgra)
{
    jbyte* _yuv  = env->GetByteArrayElements(yuv, 0);
    jint*  _bgra = env->GetIntArrayElements(bgra, 0);

    Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_yuv);
    Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);
    Mat mgray(height, width, CV_8UC1, (unsigned char *)_yuv);

    //Please make attention about BGRA byte order
    //ARGB stored in java as int array becomes BGRA at native level
    cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);

    vector<KeyPoint> v;

    FastFeatureDetector detector(50);
    detector.detect(mgray, v);
    for( size_t i = 0; i < v.size(); i++ )
        circle(mbgra, Point(v[i].pt.x, v[i].pt.y), 10, Scalar(0,0,255,255));

    env->ReleaseIntArrayElements(bgra, _bgra, 0);
    env->ReleaseByteArrayElements(yuv, _yuv, 0);
}
*/