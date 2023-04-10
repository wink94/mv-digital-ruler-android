#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include "main.h"

#define TAG "NativeLib"

using namespace std;
using namespace cv;


extern "C" JNIEXPORT jstring  JNICALL
Java_com_windula_mv_1cpp_1android_1digital_1ruler_CalibrateActivity_00024NativeBridge_calibrationFromJNI(JNIEnv *env,
                                                                                  jobject instance,
                                                                                  jlong matAddr,jfloat width,jfloat height) {

    // get Mat from raw address
    Mat &mat = *(Mat *) matAddr;
    float objectWidth = (float) width;
    float objectHeight = (float) height;

    clock_t begin = clock();

    double refObjectPXPerCM = calibrationObject(mat,objectWidth,objectHeight);

    string value = to_string(refObjectPXPerCM);
    // log computation time to Android Logcat
    double totalTime = double(clock() - begin) / CLOCKS_PER_SEC;
    __android_log_print(ANDROID_LOG_INFO, TAG, "Calibration computation time = %f seconds\n",
                        totalTime);

    return env->NewStringUTF(value.c_str());
}