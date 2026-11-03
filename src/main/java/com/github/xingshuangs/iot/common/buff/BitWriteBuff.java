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

package com.github.xingshuangs.iot.common.buff;


/**
 * 位写入
 * （bit writer）
 *
 * @author xings
 * @since 2026/3/20
 */
public class BitWriteBuff {
    private final byte[] buffer;
    private int bitPos = 0;

    public BitWriteBuff(int bitSize) {
        this.buffer = new byte[(bitSize + 7) / 8];
    }

    /**
     * 写bit
     *
     * @param data     字节数组
     * @param bitCount bit数量
     */
    public void writeBits(byte[] data, int bitCount) {
        for (int i = 0; i < bitCount; i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;

            int bit = (data[byteIndex] >> bitIndex) & 1;

            this.writeBit(bit);
        }
    }

    /**
     * 写单个bit（内部方法）
     * write bit
     *
     * @param bit bit
     */
    private void writeBit(int bit) {
        int byteIndex = this.bitPos / 8;
        int bitIndex = this.bitPos % 8;

        this.buffer[byteIndex] |= (byte) (bit << bitIndex);
        this.bitPos++;
    }

    /**
     * 写单个bit
     *
     * @param value    值
     * @param bitCount bit数量
     * @param lsbFirst 是否lsbFirst
     */
    public void writeByteBits(byte value, int bitCount, boolean lsbFirst) {
        if (bitCount < 0 || bitCount > 8) {
            throw new IllegalArgumentException("bitCount must be between 0 and 8");
        }

        int val = value & 0xFF;

        for (int i = 0; i < bitCount; i++) {
            int offset = lsbFirst ? i : 7 - i;
            this.writeBit((val >> offset) & 1);
        }
    }

    /**
     * 转为字节数组
     *
     * @return 字节数组
     */
    public byte[] toByteArray() {
        return buffer;
    }
}
