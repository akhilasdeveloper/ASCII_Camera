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

extern "C"
JNIEXPORT void JNICALL
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_reducePixelsNative2(
        JNIEnv *env, jobject thiz,
        jint width,
        jint height,
        jint avg_bitmap_width,
        jint pixel_avg_size,
        jbyteArray int_array,
        jintArray ascii_color_array,
        jintArray ascii_index_array,
        jint density_length,
        jint color_type,
        jfloat ansi_ratio,
        jint fg_color) {

    jbyte *pixelsJ = env->GetByteArrayElements(int_array, nullptr);
    jint *ascii_color_arrayJ = env->GetIntArrayElements(ascii_color_array, nullptr);
    jint *ascii_index_arrayJ = env->GetIntArrayElements(ascii_index_array, nullptr);

    jint arraySize = pixel_avg_size * pixel_avg_size;
    jint rowArray[avg_bitmap_width * 4];

    for (jint index = 0; index < width*height; index ++) {
        jint y = index / width;
        jint x = index % width;
        jint col = x / pixel_avg_size;
        jint row = y / pixel_avg_size;
        jint rowArrayCol = col * 4;

        rowArray[rowArrayCol] += pixelsJ[index * 4] & 0xFF;
        rowArray[rowArrayCol + 1] += pixelsJ[index * 4 + 1] & 0xFF;
        rowArray[rowArrayCol + 2] += pixelsJ[index * 4 + 2] & 0xFF;
        rowArray[rowArrayCol + 3] += pixelsJ[index * 4 + 3] & 0xFF;

        if ((y + 1) % pixel_avg_size == 0 && (x + 1) % pixel_avg_size == 0) {
            jint r = rowArray[rowArrayCol] / arraySize;
            jint g = rowArray[rowArrayCol + 1] / arraySize;
            jint b = rowArray[rowArrayCol + 2] / arraySize;
            jint a = rowArray[rowArrayCol + 3] / arraySize;

            rowArray[rowArrayCol] = 0;
            rowArray[rowArrayCol + 1] = 0;
            rowArray[rowArrayCol + 2] = 0;
            rowArray[rowArrayCol + 3] = 0;

            jint result = (a << 24) | (r << 16) | (g << 8) | b;
            jint densityIndex = calculateDensityIndexNative(result, density_length);
            jint ind = col + avg_bitmap_width * row;
            ascii_index_arrayJ[ind] = densityIndex;

            if (color_type == -1) {
                ascii_color_arrayJ[ind] = fg_color;
                continue;
            }

            if (color_type == -3){
                ascii_color_arrayJ[ind] = result;
                continue;
            }

            if (color_type == -2){

                jint maxRG =  (r > g) ? r : g;
                jint maxColor = (b > maxRG) ? b : maxRG;
                    if (maxColor > 0) {
                        jint threshold = (jint) ((float) maxColor * ansi_ratio);
                        r =  (r >= threshold) ? r : 0;
                        g =  (g >= threshold) ? g : 0;
                        b =  (b >= threshold) ? b : 0;
                    }
                jint ansiResult = (a << 24) | (r << 16) | (g << 8) | b;
                ascii_color_arrayJ[ind] = ansiResult;
            }

        }
    }

    env->ReleaseByteArrayElements(int_array, pixelsJ, 0);
    env->ReleaseIntArrayElements(ascii_index_array, ascii_index_arrayJ, 0);
    env->ReleaseIntArrayElements(ascii_color_array, ascii_color_arrayJ, 0);
}

jint calculateDensityIndexNative(jint pixel,
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

extern "C"
JNIEXPORT void JNICALL
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_generateResultNative2(
        JNIEnv *env,
        jobject thiz,
        jintArray ascii_index_array,
        jintArray ascii_color_array,
        jint avg_bitmap_width,
        jint avg_bitmap_height,
        jint text_size_int,
        jbyteArray density_int_array,
        jintArray result_array,
        jint result_width,
        jint bg_color) {

    jbyte *density_int_arrayJ = env->GetByteArrayElements(density_int_array, nullptr);
    jint *ascii_index_arrayJ = env->GetIntArrayElements(ascii_index_array, nullptr);
    jint *ascii_color_arrayJ = env->GetIntArrayElements(ascii_color_array, nullptr);
    jint *result_arrayJ = env->GetIntArrayElements(result_array, nullptr);

    for (jint index = 0; index < (text_size_int * text_size_int * avg_bitmap_width *
            avg_bitmap_height); index++) {
        jint x = index % result_width;
        jint y = index / result_width;

        jint asciiIndexX = x / text_size_int;
        jint asciiIndexY = y / text_size_int;
        jint asciiIndexIndex = asciiIndexX + avg_bitmap_width * asciiIndexY;
        jint asciiIndex = ascii_index_arrayJ[asciiIndexIndex];

        jint asciiArrayIndexX = x % text_size_int;
        jint asciiArrayIndexY = y % text_size_int;
        jint asciiArrayIndex = asciiArrayIndexX + text_size_int * asciiArrayIndexY;
        jbyte ascii = density_int_arrayJ[asciiArrayIndex +
                                         (asciiIndex * text_size_int * text_size_int)];

        result_arrayJ[index] = (ascii) ? ascii_color_arrayJ[asciiIndexIndex] : bg_color;
    }

    env->ReleaseByteArrayElements(density_int_array, density_int_arrayJ, 0);
    env->ReleaseIntArrayElements(ascii_index_array, ascii_index_arrayJ, 0);
    env->ReleaseIntArrayElements(ascii_color_array, ascii_color_arrayJ, 0);
    env->ReleaseIntArrayElements(result_array, result_arrayJ, 0);

}


extern "C"
JNIEXPORT void JNICALL
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_cropArrayNative(
        JNIEnv *env, jobject thiz, jbyteArray out_put_array, jint out_put_array_size, jint width,
        jbyteArray cropped_array, jint width1, jint height1) {

    jbyte *out_put_arrayJ = env->GetByteArrayElements(out_put_array, nullptr);
    jbyte *cropped_arrayJ = env->GetByteArrayElements(cropped_array, nullptr);

    for (jint index = 0; index < out_put_array_size; index++) {
        jbyte byte = out_put_arrayJ[index];
        jint x = index % (width * 4);
        jint y = index / (width * 4);
        jint ind = x + width1 * 4 * y;
        if (x >= width1 * 4 || y >= height1)
            continue;

        cropped_arrayJ[ind] = byte;
    }

    env->ReleaseByteArrayElements(cropped_array, cropped_arrayJ, 0);
    env->ReleaseByteArrayElements(out_put_array, out_put_arrayJ, 0);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_rotateByteArray90Native(
        JNIEnv *env, jobject thiz, jbyteArray array, jbyteArray rotated_array, jint height, jint width) {

    jbyte *arrayJ = env->GetByteArrayElements(array, nullptr);
    jbyte *rotated_arrayJ = env->GetByteArrayElements(rotated_array, nullptr);

    for (jint index = 0; index < width * height; index++) {

        jint i = index / width;
        jint j = index % width;

        jint rotatedIndex = (j * height + (height - i - 1)) * 4;
        jint copyIndex = (i * width + j) * 4;

        rotated_arrayJ[rotatedIndex] = arrayJ[copyIndex];
        rotated_arrayJ[rotatedIndex + 1] = arrayJ[copyIndex + 1];
        rotated_arrayJ[rotatedIndex + 2] = arrayJ[copyIndex + 2];
        rotated_arrayJ[rotatedIndex + 3] = arrayJ[copyIndex + 3];
    }

    env->ReleaseByteArrayElements(array, arrayJ, 0);
    env->ReleaseByteArrayElements(rotated_array, rotated_arrayJ, 0);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_akhilasdeveloper_asciicamera_util_asciigenerator_AsciiGenerator_mirrorByteArrayHorizontalNative(
        JNIEnv *env, jobject thiz, jbyteArray array, jbyteArray mirrored_array, jint height, jint width) {

    jbyte *arrayJ = env->GetByteArrayElements(array, nullptr);
    jbyte *mirrored_arrayJ = env->GetByteArrayElements(mirrored_array, nullptr);

    for (jint index = 0; index < width * height; index++) {

        jint i = index / width;
        jint j = index % width;

        jint mirroredIndex = (i * width + (width - j - 1)) * 4;
        jint copyIndex = (i * width + j) * 4;

        mirrored_arrayJ[mirroredIndex] = arrayJ[copyIndex];
        mirrored_arrayJ[mirroredIndex + 1] = arrayJ[copyIndex + 1];
        mirrored_arrayJ[mirroredIndex + 2] = arrayJ[copyIndex + 2];
        mirrored_arrayJ[mirroredIndex + 3] = arrayJ[copyIndex + 3];
    }

    env->ReleaseByteArrayElements(array, arrayJ, 0);
    env->ReleaseByteArrayElements(mirrored_array, mirrored_arrayJ, 0);

}