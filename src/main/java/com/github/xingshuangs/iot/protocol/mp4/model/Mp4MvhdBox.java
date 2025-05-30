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

package com.github.xingshuangs.iot.protocol.mp4.model;


import com.github.xingshuangs.iot.common.buff.ByteWriteBuff;
import com.github.xingshuangs.iot.protocol.mp4.enums.EMp4Type;
import com.github.xingshuangs.iot.utils.TimesUtil;

import java.time.LocalDateTime;

/**
 * mvhd box
 *
 * @author xingshuang
 */
public class Mp4MvhdBox extends Mp4Box {

    /**
     * 1-bytes, version
     */
    private final int version;

    /**
     * 3-bytes flags
     */
    private final byte[] flags;

    /**
     * 4-bytes creation time.
     */
    private final LocalDateTime creationTime;

    /**
     * 4-bytes modification time
     */
    private final LocalDateTime modificationTime;

    /**
     * 4-bytes timescale
     */
    private final int timescale;

    /**
     * 4-bytes duration
     */
    private final int duration;

    /**
     * 4-bytes playback rate
     */
    private final int rate;

    /**
     * 2-bytes volume
     */
    private final int volume;

    /**
     * 10-bytes
     */
    private final byte[] reserved;

    /**
     * 36-bytes unity matrix
     */
    private final byte[] videoTransformationMatrix;

    /**
     * 24-bytes pre_defined
     */
    private final byte[] preDefined;

    /**
     * 4-bytes next_track_ID
     */
    private final int nextTrackId;

    public Mp4MvhdBox(Mp4TrackInfo trackInfo) {
        this.mp4Type = EMp4Type.MVHD;
        this.version = 0;
        this.flags = new byte[3];
        this.creationTime = TimesUtil.getUTCDateTime(1);
        this.modificationTime = TimesUtil.getUTCDateTime(2);
        this.timescale = trackInfo.getTimescale();
        this.duration = trackInfo.getDuration();
        this.rate = 1;
        this.volume = 1;
        this.reserved = new byte[10];
        this.videoTransformationMatrix = new byte[]{
                0x00, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x01, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                0x40, 0x00, 0x00, 0x00
        };
        this.preDefined = new byte[24];
        this.nextTrackId = 0xFFFFFFFF;
    }

    @Override
    public int byteArrayLength() {
        return 108;
    }

    @Override
    public byte[] toByteArray() {
        int size = this.byteArrayLength();
        return ByteWriteBuff.newInstance(size)
                .putInteger(size)
                .putBytes(this.mp4Type.getByteArray())
                .putByte(this.version)
                .putBytes(this.flags)
                .putInteger(TimesUtil.getUTCTotalSecond(this.creationTime))
                .putInteger(TimesUtil.getUTCTotalSecond(this.modificationTime))
                .putInteger(this.timescale)
                .putInteger(this.duration)
                .putInteger(this.rate << 16)
                .putShort(this.volume << 8)
                .putBytes(this.reserved)
                .putBytes(this.videoTransformationMatrix)
                .putBytes(this.preDefined)
                .putInteger(this.nextTrackId)
                .getData();
    }
}
