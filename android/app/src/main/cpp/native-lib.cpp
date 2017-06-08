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

    DocScanner *docScan = new DocScanner();
    docScan->test(x);

    std::ostringstream oss;
    oss << "Hello " << x;

    return env->NewStringUTF(oss.str().c_str());
}
