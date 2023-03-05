#include "jni.h"
#include "string"
#include "iostream"
#include <cmath>

jdouble calculateLuminanceNative(jint color);

jint mapNative(jfloat value,
               jfloat startValue,
               jfloat endValue,
               jint mapStartValue,
               jint mapEndValue);

jint calculateDensityIndexNative(jint pixel, jint length);

void addToResultArrayNative(jint x, jint y, jint width, jint anInt, jint color, jint color1,
                            const jbyte slice[], jint *pInt);

extern "C"
JNIEXPORT void JNICALL
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_reducePixelsNative(
        JNIEnv *env, jobject thiz, jint x, jint y, jint width, jint text_bitmap_width,
        jint text_size_int, jbyteArray int_array, jintArray ascii_color_array,
        jintArray ascii_index_array, jint density_length) {

    jbyte *pixelsJ = env->GetByteArrayElements(int_array, nullptr);
    jint *ascii_color_arrayJ = env->GetIntArrayElements(ascii_color_array, nullptr);
    jint *ascii_index_arrayJ = env->GetIntArrayElements(ascii_index_array, nullptr);

    jint arraySize = text_size_int * text_size_int;
    jint xStart = x * text_size_int;
    jint yStart = y * text_size_int;

    jint yEnd = yStart + text_size_int;
    jint xEnd = xStart + text_size_int;

    jint a = 0;
    jint r = 0;
    jint g = 0;
    jint b = 0;

    jint offset = 4;

    for (jint i = yStart; i < yEnd; i++) {
        for (jint j = i * width + xStart; j < i * width + xEnd; j++) {

            jint index = offset * j;

            r += static_cast<uint8_t>(pixelsJ[index]);
            g += static_cast<uint8_t>(pixelsJ[index + 1]);
            b += static_cast<uint8_t>(pixelsJ[index + 2]);
            a += static_cast<uint8_t>(pixelsJ[index + 3]);

        }
    }

    a /= arraySize;
    r /= arraySize;
    g /= arraySize;
    b /= arraySize;

    env->ReleaseByteArrayElements(int_array, pixelsJ, 0);

    jint result = (a << 24) | (r << 16) | (g << 8) | b;
    jint densityIndex = calculateDensityIndexNative(result, density_length);
    jint index = x + text_bitmap_width * y;

    ascii_color_arrayJ[index] = result;
    ascii_index_arrayJ[index] = densityIndex;

    env->ReleaseIntArrayElements(ascii_index_array, ascii_index_arrayJ, 0);
    env->ReleaseIntArrayElements(ascii_color_array, ascii_color_arrayJ, 0);
}




extern "C" jint
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_calculateAvgColorNative(
        JNIEnv *env, jobject,
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
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_getSubPixelsNative(
        JNIEnv *env, jobject,
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
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_getSubPixelsBytesNative(
        JNIEnv *env, jobject,
        jint width,
        jint xStart,
        jint yStart,
        jint destWidth,
        jint destHeight,
        jbyteArray array,
        jbyteArray resultArray) {

    // Get a pointer to the input byte array
    jbyte *arrayJ = env->GetByteArrayElements(array, nullptr);

    jbyte *resultArrayJ = env->GetByteArrayElements(resultArray, nullptr);

    jint yEnd = yStart + destHeight;
    jint xEnd = xStart + destWidth;

    jint index = 0;
    for (jint i = yStart; i < yEnd; i++) {
        for (jint j = i * width + xStart; j < i * width + xEnd; j++) {
            resultArrayJ[index] = arrayJ[j];
            index++;
        }
    }

    env->ReleaseByteArrayElements(array, arrayJ, 0);
    env->ReleaseByteArrayElements(resultArray, resultArrayJ, 0);

}

extern "C" void
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_addToResultArrayNative(
        JNIEnv *env, jobject,
        jint x,
        jint y,
        jint width,
        jint textSizeInt,
        jint bgColor,
        jint fgColor,
        jbyteArray array,
        jintArray resultArray) {

    // Get a pointer to the input byte array
    jbyte *arrayJ = env->GetByteArrayElements(array, nullptr);
    jint *resultArrayJ = env->GetIntArrayElements(resultArray, nullptr);

    for (int index = 0; index < textSizeInt * textSizeInt; ++index) {
        jbyte i = arrayJ[index];
        jint xx = index % textSizeInt;
        jint yy = index / textSizeInt;
        jint mainIndex = ((x * textSizeInt) + xx) + width * ((y * textSizeInt) + yy);
        resultArrayJ[mainIndex] = (i) ? fgColor : bgColor;
    }

    env->ReleaseByteArrayElements(array, arrayJ, 0);
    env->ReleaseIntArrayElements(resultArray, resultArrayJ, 0);

}



jint calculateDensityIndexNative(jint pixel,
                                 jint densityLength) {

    jdouble brightness = calculateLuminanceNative(pixel);
    jint charIndex = mapNative((float) brightness, 0, 1, 0, densityLength);
    return densityLength - charIndex - 1;
}

extern "C" jint
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_calculateDensityIndexNative(
        JNIEnv *env, jobject,
        jint pixel,
        jint densityLength) {

    jdouble brightness = calculateLuminanceNative(pixel);
    jint charIndex = mapNative((float) brightness, 0, 1, 0, densityLength);
    return densityLength - charIndex - 1;
}


jint mapNative(jfloat value,
               jfloat startValue,
               jfloat endValue,
               jint mapStartValue,
               jint mapEndValue) {

    jfloat n = endValue - startValue;
    jint mapN = mapEndValue - mapStartValue;
    jfloat factor = ((float) mapN) / n;
    jint result = ((jint) ((startValue + value) * factor));
    if (result < 0)
        result = 0;
    if (result > mapN - 1)
        result = mapN - 1;
    return result;
}

jdouble calculateLuminanceNative(jint color) {

    jint r = (color >> 16) & 0xff;
    jint g = (color >> 8) & 0xff;
    jint b = color & 0xff;

    double sr = r / 255.0;
    sr = sr < 0.04045 ? sr / 12.92 : pow((sr + 0.055) / 1.055, 2.4);
    double sg = g / 255.0;
    sg = sg < 0.04045 ? sg / 12.92 : pow((sg + 0.055) / 1.055, 2.4);
    double sb = b / 255.0;
    sb = sb < 0.04045 ? sb / 12.92 : pow((sb + 0.055) / 1.055, 2.4);
    return (sr * 0.2126 + sg * 0.7152 + sb * 0.0722);
}

extern "C" void
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_convertByteArrayToRgbNative(
        JNIEnv *env, jobject,
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

extern "C"
JNIEXPORT void JNICALL
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_generateResultNative(
        JNIEnv *env,
        jobject thiz,
        jintArray ascii_index_array,
        jint ascii_index_array_size,
        jint text_bitmap_width,
        jint text_size_int,
        jbyteArray density_int_array,
        jintArray result_array,
        jint result_width,
        jint fg_color,
        jint bg_color) {

    jbyte *density_int_arrayJ = env->GetByteArrayElements(density_int_array, nullptr);
    jint *ascii_index_arrayJ = env->GetIntArrayElements(ascii_index_array, nullptr);
    jint *result_arrayJ = env->GetIntArrayElements(result_array, nullptr);
    jbyte slice[text_size_int * text_size_int];

    for (jint index = 0; index < ascii_index_array_size; index++) {
        jint i = ascii_index_arrayJ[index];
        jint x = index % text_bitmap_width;
        jint y = index / text_bitmap_width;
        jint sliceStart = i * text_size_int * text_size_int;
        jint sliceEnd = sliceStart + (text_size_int * text_size_int);

        for (jint index1 = sliceStart; index1 < sliceEnd; ++index1) {
            slice[index1 - sliceStart] = density_int_arrayJ[index1];
        }

        addToResultArrayNative(x, y, result_width, text_size_int, bg_color, fg_color, slice, result_arrayJ);

    }

    env->ReleaseByteArrayElements(density_int_array, density_int_arrayJ, 0);
    env->ReleaseIntArrayElements(ascii_index_array, ascii_index_arrayJ, 0);
    env->ReleaseIntArrayElements(result_array, result_arrayJ, 0);

}



void
addToResultArrayNative(
        jint x,
        jint y,
        jint width,
        jint textSizeInt,
        jint bgColor,
        jint fgColor,
        const jbyte arrayJ[],
        jint* resultArrayJ) {

    for (int ind = 0; ind < textSizeInt * textSizeInt; ++ind) {
        jbyte b = arrayJ[ind];
        jint xx = ind % textSizeInt;
        jint yy = ind / textSizeInt;
        jint mainIndex = ((x * textSizeInt) + xx) + width * ((y * textSizeInt) + yy);
        resultArrayJ[mainIndex] = (b) ? fgColor : bgColor;
    }

}
