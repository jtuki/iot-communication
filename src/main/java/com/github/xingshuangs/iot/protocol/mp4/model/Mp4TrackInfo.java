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
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 轨道信息
 *
 * @author xingshuang
 */
@Data
public class Mp4TrackInfo {

    private int id;

    private String type = "video";

    private String codec = "avc1.64002a";

    // region video
    private int timescale;

    private int duration;

    private int width;

    private int height;

    private byte[] sps;

    private byte[] pps;
    // endregion

    // region audio

    private int volume;

    private int audioSampleRate;

    private int channelCount;

    private byte[] config;
    // endregion

    private List<Mp4SampleData> sampleData = new ArrayList<>();

    public byte[] totalSampleData() {
        int sum = this.sampleData.stream().mapToInt(Mp4SampleData::getSize).sum();
        ByteWriteBuff buff = new ByteWriteBuff(sum);
        sampleData.forEach(x -> buff.putBytes(x.getData()));
        return buff.getData();
    }
}
