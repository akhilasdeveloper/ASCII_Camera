#include "jni.h"
#include "string"
#include "iostream"

extern "C" JNIEXPORT jstring JNICALL
Java_com_akhilasdeveloper_asciicamera_ui_MainActivity_Test(JNIEnv *env, jobject) {
    std::string hello = "Hello test";
    return env->NewStringUTF(hello.c_str());
}


extern "C" void Java_com_akhilasdeveloper_asciicamera_util_AsciiGenerator_convertRgbaToRgb(JNIEnv *env, jobject, jbyteArray rgbaData, jintArray jcolorOutput, jint width, jint height) {

    // Get a pointer to the input byte array
    jbyte *data = env->GetByteArrayElements(rgbaData, nullptr);

    jint *rgbData = env->GetIntArrayElements(jcolorOutput, nullptr);


    // Loop through the input byte array, converting each RGBA pixel to an RGB pixel
    int pixelIndex = 0;
    for (int i = 0; i < width * height * 4; i += 4) {
        auto r = static_cast<uint8_t>(data[i]);
        auto g = static_cast<uint8_t>(data[i + 1]);
        auto b = static_cast<uint8_t>(data[i + 2]);
        auto a = static_cast<uint8_t>(data[i + 3]);
        // Ignore the alpha channel
        rgbData[pixelIndex++] = (a << 24) | (r << 16) | (g << 8) | b;
    }

    // Release the input and output arrays
    env->ReleaseByteArrayElements(rgbaData, data, 0);
    env->ReleaseIntArrayElements(jcolorOutput, rgbData, 0);

}
