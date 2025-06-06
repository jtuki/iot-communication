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

package com.github.xingshuangs.iot.protocol.rtcp.service;


import com.github.xingshuangs.iot.protocol.rtcp.enums.ERtcpSdesItemType;
import com.github.xingshuangs.iot.protocol.rtcp.model.*;
import com.github.xingshuangs.iot.protocol.rtp.model.RtpPackage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

/**
 * RTCP data statistics.
 * RTCP接收数据统计
 *
 * @author xingshuang
 */
@Slf4j
@Data
public class RtcpDataStatistics {

    /**
     * Source id.
     * (同步源（SSRC of sender）：32比特，SR包发送者的同步源标识符。与对应RTP包中的SSRC一样。)
     */
    private long sourceId;

    /**
     * Highest sequence number
     * (接收的最大序号)
     */
    private int highestSequenceNumber = 0;

    /**
     * Packets received since last reset
     * (从上一次重置之后的包接收数量)
     */
    private int packetsReceivedSinceLastReset = 0;

    /**
     * Packets lost since last reset.
     * (从上一次重置之后的丢包数量)
     */
    private int packetsLostSinceLastReset = 0;

    /**
     * Cumulative packet lost.
     * (累计丢包数量)
     */
    private int cumulativePacketLost = 0;

    /**
     * Sequence number cycles.
     * (序列号循环被使用次数)
     */
    private int sequenceNumberCycles = 0;

    /**
     * Sample timestamp sum.
     * (采样总时间)
     */
    private long sampleTimestampSum = 0;

    /**
     * Last rtp timestamp.
     * （上一次的RTP时间戳）
     */
    private long lastRtpTimestamp = 0;

    /**
     * Last rtp ssrc.
     * （上一次RTP的SSRC）
     */
    private long lastRtpSsrc = 0;

    /**
     * Last ntp time sender report received.
     * （上一次RTCP发送SR的NTP时间）
     */
    private long lastNtpTimeSenderReportReceived = 0;

    /**
     * Last time rtcp report received.
     * （上一次接收到的SR的时间）
     */
    private long lastTimeRtcpReportReceived = 0;

    /**
     * Last local time receive rtp.
     * (上一次接收到RTP的时间，本地当前的时间戳）
     */
    private long lastLocalTimeReceiveRtp = 0;

    public RtcpDataStatistics() {
        this.sourceId = System.currentTimeMillis();
    }

    /**
     * Process rtp package.
     * (处理RTP数据包)
     *
     * @param rtp  rtp package
     * @param send send callback by bytes
     */
    public void processRtpPackage(RtpPackage rtp, Consumer<byte[]> send) {
        if (this.lastRtpSsrc > 0) {
            this.sampleTimestampSum += rtp.getHeader().getTimestamp() - this.lastRtpTimestamp;
            int delta = rtp.getHeader().getSequenceNumber() - this.highestSequenceNumber;
            if (delta > 1) {
                this.cumulativePacketLost += delta - 1;
                this.packetsLostSinceLastReset += delta - 1;
            }
            if (this.cumulativePacketLost > 0x7FFFFF) {
                this.cumulativePacketLost = 0x7FFFFF;
            }
            if (rtp.getHeader().getSequenceNumber() < this.highestSequenceNumber) {
                this.sequenceNumberCycles++;
            }
        }
        this.packetsReceivedSinceLastReset++;
        this.highestSequenceNumber = rtp.getHeader().getSequenceNumber();
        this.lastRtpSsrc = rtp.getHeader().getSsrc();
        this.lastRtpTimestamp = rtp.getHeader().getTimestamp();

        // 第一次接收rtp数据
        if (this.lastLocalTimeReceiveRtp == 0) {
            this.lastLocalTimeReceiveRtp = System.currentTimeMillis();
        }

        // 时间间隔超过5s的发一次RR数据，接收数据报告
        if (System.currentTimeMillis() - this.lastLocalTimeReceiveRtp > 5_000) {
            byte[] receiverAndByteContent = this.createReceiverAndSdesContent();
            send.accept(receiverAndByteContent);
            this.lastLocalTimeReceiveRtp = System.currentTimeMillis();
            int fractionLost = this.packetsLostSinceLastReset * 100 / this.packetsReceivedSinceLastReset;
            log.debug("Data Statistics: package lost number[{}], totoal number[{}], fraction lost[{}%]", this.cumulativePacketLost, this.packetsReceivedSinceLastReset, fractionLost);
        }
    }

    /**
     * Reset state.
     * (重置状态)
     */
    public void resetState() {
        this.packetsLostSinceLastReset = 0;
        this.packetsReceivedSinceLastReset = 0;
    }

    /**
     * Process rtcp package.
     * (处理RTCP数据包)
     *
     * @param basePackages rtcp数据包列表
     */
    public void processRtcpPackage(List<RtcpBasePackage> basePackages) {
        for (RtcpBasePackage rtcp : basePackages) {
            log.debug("RTCP receives [{}] data，{}", rtcp.getHeader().getPackageType(), rtcp);
            if (rtcp instanceof RtcpSenderReport) {
                this.lastNtpTimeSenderReportReceived = ((RtcpSenderReport) rtcp).getSenderInfo().getMswTimestamp();
                this.lastTimeRtcpReportReceived = System.currentTimeMillis();
            }
        }
    }

    /**
     * Create receiver report.
     * (创建接收报告)
     *
     * @return RtcpReceiverReport
     */
    public RtcpReceiverReport createReceiverReport() {
        // 丢包率（8bit）:丢包率需要转换为0-255的占比；如20%丢包=20%*255=51
        int fractionLost = 0;
        if (this.packetsReceivedSinceLastReset != 0) {
            fractionLost = this.packetsLostSinceLastReset * 256 / this.packetsReceivedSinceLastReset;
        }
        // 期望接收的最大序列号，低16位存储期望最大序列号；高16为翻转次数统计
        long extHighestSequenceNumberReceived = ((long) this.sequenceNumberCycles << 16) | this.highestSequenceNumber;
        // 最后一次接收SR到发送的时延（32bit）:DLSR最后一次收到SR包后到发送中间的时延；
        long delaySinceLastTimeSenderReportReceived = 0;
        if (this.lastTimeRtcpReportReceived > 0) {
            delaySinceLastTimeSenderReportReceived = (System.currentTimeMillis() - this.lastTimeRtcpReportReceived) / 1000 * 65536;
        }
        RtcpReceiverReport receiverReport = new RtcpReceiverReport(this.sourceId);
        RtcpReportBlock reportBlock = new RtcpReportBlock();
        reportBlock.setSourceId(this.lastRtpSsrc);
        reportBlock.setFractionLost(fractionLost);
        reportBlock.setCumulativePacketLost(this.cumulativePacketLost);
        reportBlock.setExtHighestSequenceNumberReceived(extHighestSequenceNumberReceived);
        reportBlock.setJitter(0);
        reportBlock.setLastNtpTimeSenderReportReceived(this.lastNtpTimeSenderReportReceived);
        reportBlock.setDelaySinceLastTimeSenderReportReceived(delaySinceLastTimeSenderReportReceived);
        receiverReport.addRtcpReportBlock(reportBlock);
        return receiverReport;
    }

    /**
     * Create sdes report.
     * (创建SDES报告)
     *
     * @return RtcpSdesReport
     */
    public RtcpSdesReport createSdesReport() {
        RtcpSdesReport sdesReport = new RtcpSdesReport();
        RtcpSdesChunk chunk = new RtcpSdesChunk(this.sourceId);
        RtcpSdesItem item = new RtcpSdesItem();
        item.setType(ERtcpSdesItemType.CNAME);
        item.setText("iot-communication");
        item.setLength(17);
        chunk.addRtcpSdesItem(item);
        sdesReport.addRtcpSdesChunk(chunk);
        return sdesReport;
    }

    /**
     * Create bye.
     * 创建Byte
     *
     * @return RtcpBye
     */
    public RtcpBye createByte() {
        return new RtcpBye(this.sourceId);
    }

    /**
     * Create receiver and sdes content.
     * (创建RR内容的报告字节数组)
     *
     * @return byte array
     */
    public byte[] createReceiverAndSdesContent() {
        RtcpReceiverReport receiverReport = this.createReceiverReport();
        RtcpSdesReport sdesReport = this.createSdesReport();
//        log.debug("RTCP发送[{}]，[{}]数据", receiverReport.getHeader().getPackageType(), sdesReport.getHeader().getPackageType());
        byte[] res = new byte[receiverReport.byteArrayLength() + sdesReport.byteArrayLength()];
        System.arraycopy(receiverReport.toByteArray(), 0, res, 0, receiverReport.byteArrayLength());
        System.arraycopy(sdesReport.toByteArray(), 0, res, receiverReport.byteArrayLength(), sdesReport.byteArrayLength());
        return res;
    }

    /**
     * Create receiver and byte content.
     * (创建byte的报告字节数组)
     *
     * @return byte array
     */
    public byte[] createReceiverAndByteContent() {
        RtcpReceiverReport receiverReport = this.createReceiverReport();
        RtcpBye aByte = this.createByte();
        log.debug("RTCP sends [{}], [{}] data", receiverReport.getHeader().getPackageType(), aByte.getHeader().getPackageType());
        byte[] res = new byte[receiverReport.byteArrayLength() + aByte.byteArrayLength()];
        System.arraycopy(receiverReport.toByteArray(), 0, res, 0, receiverReport.byteArrayLength());
        System.arraycopy(aByte.toByteArray(), 0, res, receiverReport.byteArrayLength(), aByte.byteArrayLength());
        return res;
    }
}
