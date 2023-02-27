#include "jni.h"
#include "string"
#include "iostream"

extern "C" JNIEXPORT jstring JNICALL
Java_com_akhilasdeveloper_asciicamera_ui_MainActivity_Test(JNIEnv *env, jobject){
    std::string hello = "Hello test";
    return env->NewStringUTF(hello.c_str());
}