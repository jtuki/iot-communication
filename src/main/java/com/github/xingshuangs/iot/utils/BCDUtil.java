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
 * BCD code conversion tool.
 * (BCD码转换工具)
 *
 * @author xingshuang
 */
public class BCDUtil {

    private BCDUtil() {
        // NOOP
    }

    /**
     * Decode a BCD byte as an integer.
     * 转换为int
     *
     * @param data byte data
     * @return int result
     */
    public static int toInt(byte data) {
        return (((data >> 4) & 0x0F) * 10) + (data & 0x0F);
    }

    /**
     * Decode a BCD byte array as an integer.
     * 0x23 0x01 = 2301 <br>
     *
     * @param data byte array
     * @return int result
     */
    public static int toInt(byte[] data) {
        int result = 0;
        for (byte b : data) {
            result = result * 100 + toInt(b);
        }
        return result;
    }

    /**
     * Decode a BCD byte array as an integer using the specified maximum number of digits.<br>
     * 0x23 0x01 = 2301 <br>
     * maxDigits = 3 to 301
     *
     * @param data      byte array
     * @param maxDigits max digits
     * @return int result
     */
    public static int toInt(byte[] data, int maxDigits) {
        int value = toInt(data);
        int maxValue = (int) Math.pow(10, maxDigits);
        return value % maxValue;
    }

    /**
     * Encode an integer as a BCD byte.
     *
     * @param data int value
     * @return byte
     */
    public static byte toByte(int data) {
        if (data > 99 || data < 0) {
            throw new IllegalArgumentException("data must be between 0 and 99");
        }
        return (byte) (((data / 10) << 4) | (data % 10));
    }

    /**
     * Get the number of digits in an integer.
     * 获取整数的位数
     *
     * @param value int value
     * @return number of digits
     */
    public static int numDigits(int value) {
        if (value == 0) return 1;

        return (int) Math.floor(Math.log10(Math.abs(value))) + 1;
    }

    /**
     * Encode an integer as a BCD byte array.
     * 2301 to 0x23 0x01 <br>
     * 123 to 0x01 0x23 <br>
     * 5 to 0x05 <br>
     *
     * @param value int value
     * @param byteLength byte array length
     * @return byte array
     */
    public static byte[] toBytes(int value, int byteLength) {
        if (value < 0) {
            throw new IllegalArgumentException("BCD只支持非负整数");
        }
        byte[] bcd = new byte[byteLength];
        int temp = value;
        // 从低字节向高字节填充
        for (int i = byteLength - 1; i >= 0; i--) {
            int low = temp % 10;
            temp /= 10;
            int high = temp % 10;
            temp /= 10;
            bcd[i] = (byte) ((high << 4) | low);
        }

        return bcd;
    }

    /**
     * Encode an integer as a BCD byte array.
     * 根据整数的位数自动分配字节数组长度 <br>
     * 2301 to 0x23 0x01 <br>
     * 123 to 0x01 0x23 <br>
     * 5 to 0x05 <br>
     *
     * @param value int value
     * @return byte array
     */
    public static byte[] toBytes(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("BCD只支持非负整数");
        }

        // 计算需要多少个字节
        int number = numDigits(value);
        int byteLength = number % 2 == 0 ? number / 2 : number / 2 + 1;
        return toBytes(value, byteLength);
    }
}
