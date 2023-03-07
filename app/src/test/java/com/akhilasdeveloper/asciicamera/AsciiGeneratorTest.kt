package com.akhilasdeveloper.asciicamera

import com.akhilasdeveloper.asciicamera.util.generateResult
import com.akhilasdeveloper.asciicamera.util.getSubPixels
import com.akhilasdeveloper.asciicamera.util.rotateByteArrayImage
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import timber.log.Timber

@RunWith(JUnit4::class)
class AsciiGeneratorTest {

    @Test
    fun testGettingSubPixels() {

        val testArray = intArrayOf(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
            31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
            41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
            51, 52, 53, 54, 55, 56, 57, 58, 59, 60
        )

        val subArray = testArray.getSubPixels(10, 4, 0, 2, 3)
        assertThat(subArray).isEqualTo(intArrayOf(5, 6, 15, 16, 25, 26))
    }

    @Test
    fun testRotateBytes90() {

        val testArray = intArrayOf(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
            11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
            21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
            31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
            41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
            51, 52, 53, 54, 55, 56, 57, 58, 59, 60
        )

        val rotated = IntArray(testArray.size)

        rotateByteArrayImage(
            testArray,
            rotated,
            5,
            6
        )

        val answerArray = intArrayOf(
            51, 52, 41, 42, 31, 32, 21, 22, 11, 12, 1, 2,
            53, 54, 43, 44, 33, 34, 23, 24, 13, 14, 3, 4,
            55, 56, 45, 46, 35, 36, 25, 26, 15, 16, 5, 6,
            57, 58, 47, 48, 37, 38, 27, 28, 17, 18, 7, 8,
            59, 60, 49, 50, 39, 40, 29, 30, 19, 20, 9, 10

        )
        assertThat(rotated).isEqualTo(answerArray)
    }

    @Test
    fun testPopulateResult() {

        val resultArray = IntArray(60)
        val densityArray = intArrayOf(
            0, 0,
            0, 0,
            1, 0,
            0, 1,
            2, 0,
            0, 2,
            0, 3,
            3, 0
        )
        val densityIndexArray = intArrayOf(
            1, 2, 3, 2, 1,
            3, 2, 1, 2, 3,
            1, 2, 3, 2, 1
        )

        val answerArray = intArrayOf(
            1, 0, 2, 0, 0, 3, 2, 0, 1, 0,
            0, 1, 0, 2, 3, 0, 0, 2, 0, 1,
            0, 3, 2, 0, 1, 0, 2, 0, 0, 3,
            3, 0, 0, 2, 0, 1, 0, 2, 3, 0,
            1, 0, 2, 0, 0, 3, 2, 0, 1, 0,
            0, 1, 0, 2, 3, 0, 0, 2, 0, 1
        )

        generateResult(
            ascii_index_array = densityIndexArray,
            text_bitmap_width = 5,
            text_size_int = 2,
            density_byte_array = densityArray,
            result_array = resultArray,
            result_size = 60,
            result_width = 10
        )

        assertThat(resultArray).isEqualTo(answerArray)
    }

}