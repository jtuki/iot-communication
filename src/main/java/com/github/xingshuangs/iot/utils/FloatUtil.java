/*
 * MIT License
 *
 * Copyright (c) 2021-2099 Oscura (xingshuang) <xingshuang_cool@163.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.xingshuangs.iot.utils;


/**
 * Float tool
 *
 * @author xingshuang
 */
public class FloatUtil {

    private FloatUtil() {
        // NOOP
    }

    /**
     * Convert double to 8-byte array, in big-endian mode by default.
     * (将double转换为字节数组，默认采用大端模式)
     *
     * @param data double data
     * @return byte array
     */
    public static byte[] toByteArray(double data) {
        return LongUtil.toByteArray(Double.doubleToLongBits(data), false);
    }

    /**
     * Convert double to 8-byte array.
     * (将double转换为字节数组)
     *
     * @param data         double data
     * @param littleEndian is little endian
     * @return byte array
     */
    public static byte[] toByteArray(double data, boolean littleEndian) {
        return LongUtil.toByteArray(Double.doubleToLongBits(data), littleEndian);
    }

    /**
     * Convert float to 4-byte array, in big-endian mode by default.
     * (将float转换为字节数组，默认采用大端模式)
     *
     * @param data float data.
     * @return byte array
     */
    public static byte[] toByteArray(float data) {
        return IntegerUtil.toByteArray(Float.floatToIntBits(data), false);
    }

    /**
     * Convert float to 4-byte array.
     * (将float转换为字节数组)
     *
     * @param data         float data
     * @param littleEndian true：little endian，false：big endian
     * @return byte array
     */
    public static byte[] toByteArray(float data, boolean littleEndian) {
        return IntegerUtil.toByteArray(Float.floatToIntBits(data), littleEndian);
    }

    /**
     * Converts a byte array to float32.
     * （将字节数组转换为float32）
     *
     * @param data byte array
     * @return float32 data
     */
    public static float toFloat32(byte[] data) {
        return toFloat32(data, 0, false);
    }

    /**
     * Converts a byte array to float32.
     * (将字节数组转换为float32)
     *
     * @param data   byte array
     * @param offset index offset
     * @return float32 data
     */
    public static float toFloat32(byte[] data, int offset) {
        return toFloat32(data, offset, false);
    }

    /**
     * Converts a byte array to float32.
     * (将字节数组转换为float32)
     *
     * @param data         byte array
     * @param offset       offset
     * @param littleEndian true：little endian，false：big endian
     * @return float32 data
     */
    public static float toFloat32(byte[] data, int offset, boolean littleEndian) {
        if (data.length < 4) {
            throw new IndexOutOfBoundsException("data length < 4");
        }
        if (offset + 4 > data.length) {
            throw new IndexOutOfBoundsException("offset + 4 > data length");
        }
        int b = littleEndian ? 3 : 0;
        int d = littleEndian ? 1 : -1;
        int l = (((data[offset + b - d * 0] & 0xFF) << 24)
                | ((data[offset + b - d * 1] & 0xFF) << 16)
                | ((data[offset + b - d * 2] & 0xFF) << 8)
                | ((data[offset + b - d * 3] & 0xFF) << 0));
        return Float.intBitsToFloat(l);
    }

    /**
     * Converts a byte array to float64.
     * (将字节数组转换为float64)
     *
     * @param data byte array
     * @return float64 data
     */
    public static double toFloat64(byte[] data) {
        return toFloat64(data, 0, false);
    }

    /**
     * Converts a byte array to float64.
     * (将字节数组转换为float64)
     *
     * @param data   byte array
     * @param offset index offset
     * @return float64 data
     */
    public static double toFloat64(byte[] data, int offset) {
        return toFloat64(data, offset, false);
    }

    /**
     * Converts a byte array to float64.
     * (将字节数组转换为float64)
     *
     * @param data         byte array
     * @param offset       offset
     * @param littleEndian true：little endian，false：big endian
     * @return float64 data
     */
    public static double toFloat64(byte[] data, int offset, boolean littleEndian) {
        if (data.length < 8) {
            throw new IndexOutOfBoundsException("data length < 8");
        }
        if (offset + 8 > data.length) {
            throw new IndexOutOfBoundsException("offset + 8 > data length");
        }
        int b = littleEndian ? 7 : 0;
        int d = littleEndian ? 1 : -1;
        long l = ((long) (data[offset + b - d * 0] & 0xFF) << 56)
                | ((long) (data[offset + b - d * 1] & 0xFF) << 48)
                | ((long) (data[offset + b - d * 2] & 0xFF) << 40)
                | ((long) (data[offset + b - d * 3] & 0xFF) << 32)
                | ((long) (data[offset + b - d * 4] & 0xFF) << 24)
                | ((long) (data[offset + b - d * 5] & 0xFF) << 16)
                | ((long) (data[offset + b - d * 6] & 0xFF) << 8)
                | (long) (data[offset + b - d * 7] & 0xFF);
        return Double.longBitsToDouble(l);
    }
}
