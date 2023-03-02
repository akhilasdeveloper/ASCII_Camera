#include "jni.h"
#include "string"
#include "iostream"

extern "C" JNIEXPORT jstring JNICALL
Java_com_akhilasdeveloper_asciicamera_ui_MainActivity_Test(JNIEnv *env, jobject) {
    std::string hello = "Hello test";
    return env->NewStringUTF(hello.c_str());
}

extern "C" jint
Java_com_akhilasdeveloper_asciicamera_util_AsciiGenerator_calculateAvgColorNative(JNIEnv *env, jobject,
                                                                            jintArray pixels,
                                                                            jint size) {
    jint *pixelsJ = env->GetIntArrayElements(pixels, nullptr);

    jint a = 0;
    jint r = 0;
    jint g = 0;
    jint b = 0;

    for (int i = 0; i < size; i++) {
        a += (pixelsJ[i] >> 24) & 0xff;
        r += (pixelsJ[i] >> 16) & 0xff;
        g += (pixelsJ[i] >> 8) & 0xff;
        b += pixelsJ[i] & 0xff;
    }

    a /= size;
    r /= size;
    g /= size;
    b /= size;

    env->ReleaseIntArrayElements(pixels, pixelsJ, 0);

    return (a << 24) | (r << 16) | (g << 8) | b;
}

extern "C" void
Java_com_akhilasdeveloper_asciicamera_util_AsciiGenerator_getSubPixelsNative(JNIEnv *env, jobject,
                                                                       jint width,
                                                                       jint xStart,
                                                                       jint yStart,
                                                                       jint destWidth,
                                                                       jint destHeight,
                                                                       jintArray array,
                                                                       jintArray resultArray) {

    // Get a pointer to the input byte array
    jint *arrayJ = env->GetIntArrayElements(array, nullptr);

    jint *resultArrayJ = env->GetIntArrayElements(resultArray, nullptr);

    jint yEnd = yStart + destHeight;
    jint xEnd = xStart + destWidth;

    jint index = 0;
    for (jint i = yStart; i < yEnd; i++) {
        for (jint j = i * width + xStart; j < i * width + xEnd; j++) {
            resultArrayJ[index] = arrayJ[j];
            index++;
        }
    }

    env->ReleaseIntArrayElements(array, arrayJ, 0);
    env->ReleaseIntArrayElements(resultArray, resultArrayJ, 0);

}



extern "C" void
Java_com_akhilasdeveloper_asciicamera_util_AsciiGenerator_convertByteArrayToRgbNative(JNIEnv *env, jobject,
                                                                           jbyteArray rgbaData,
                                                                           jintArray jcolorOutput,
                                                                           jint width,
                                                                           jint height) {

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

        rgbData[pixelIndex++] = (a << 24) | (r << 16) | (g << 8) | b;
    }

    // Release the input and output arrays
    env->ReleaseByteArrayElements(rgbaData, data, 0);
    env->ReleaseIntArrayElements(jcolorOutput, rgbData, 0);

}
