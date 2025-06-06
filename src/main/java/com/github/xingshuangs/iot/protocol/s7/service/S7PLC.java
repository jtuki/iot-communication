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

package com.github.xingshuangs.iot.protocol.s7.service;


import com.github.xingshuangs.iot.common.buff.ByteReadBuff;
import com.github.xingshuangs.iot.common.buff.ByteWriteBuff;
import com.github.xingshuangs.iot.protocol.s7.enums.*;
import com.github.xingshuangs.iot.protocol.s7.model.DataItem;
import com.github.xingshuangs.iot.protocol.s7.model.RequestItem;
import com.github.xingshuangs.iot.protocol.s7.model.RequestNckItem;
import com.github.xingshuangs.iot.protocol.s7.model.S7Data;
import com.github.xingshuangs.iot.protocol.s7.utils.AddressUtil;
import com.github.xingshuangs.iot.utils.*;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.xingshuangs.iot.common.constant.GeneralConst.LOCALHOST;
import static com.github.xingshuangs.iot.common.constant.GeneralConst.S7_PORT;

/**
 * @author xingshuang
 */
public class S7PLC extends PLCNetwork {

    public S7PLC() {
        this(EPlcType.S1200, LOCALHOST, S7_PORT, EPlcType.S1200.getRack(), EPlcType.S1200.getSlot(), EPlcType.S1200.getPduLength());
    }

    public S7PLC(EPlcType plcType) {
        this(plcType, LOCALHOST, S7_PORT, plcType.getRack(), plcType.getSlot(), plcType.getPduLength());
    }

    public S7PLC(EPlcType plcType, String ip) {
        this(plcType, ip, S7_PORT, plcType.getRack(), plcType.getSlot(), plcType.getPduLength());
    }

    public S7PLC(EPlcType plcType, String ip, int port) {
        this(plcType, ip, port, plcType.getRack(), plcType.getSlot(), plcType.getPduLength());
    }

    public S7PLC(EPlcType plcType, String ip, int port, int rack, int slot) {
        this(plcType, ip, port, rack, slot, plcType.getPduLength());
    }

    public S7PLC(EPlcType plcType, String ip, int port, int rack, int slot, int pduLength) {
        super(ip, port);
        this.plcType = plcType;
        this.rack = rack;
        this.slot = slot;
        this.pduLength = pduLength;
    }

    //region 读取数据

    /**
     * The most primitive way to read raw data
     * (最原始的方式读取生数据)
     *
     * @param variableType variable type 参数类型
     * @param count        byte count 数据个数
     * @param area         data area 区域
     * @param dbNumber     DB number DB块编号
     * @param byteAddress  byte address 字节地址
     * @param bitAddress   bit address 位地址
     * @return byte array
     */
    public byte[] readRaw(EParamVariableType variableType, int count, EArea area, int dbNumber, int byteAddress, int bitAddress) {
        if (count <= 0) {
            throw new IllegalArgumentException("count<=0");
        }
        if (dbNumber < 0) {
            throw new IllegalArgumentException("dbNumber<0");
        }
        if (byteAddress < 0) {
            throw new IllegalArgumentException("byteAddress<0");
        }
        if (bitAddress < 0 || bitAddress > 7) {
            throw new IllegalArgumentException("bitAddress<0||bitAddress>7");
        }
        RequestItem requestItem = RequestItem.createByParams(variableType, count, area, dbNumber, byteAddress, bitAddress);
        DataItem dataItem = this.readS7Data(requestItem);
        return dataItem.getData();
    }

    /**
     * Multi-address reads byte data
     * (多地址读取字节数据)
     *
     * @param addressRead address wrapper list
     * @return byte array list
     */
    public List<byte[]> readMultiByte(MultiAddressRead addressRead) {
        List<DataItem> dataItems = this.readS7Data(addressRead.getRequestItems());
        return dataItems.stream().map(DataItem::getData).collect(Collectors.toList());
    }

    /**
     * Multi-address reads byte data. Unsafe mode, use with caution!!
     * (多地址读取字节数据) 非安全模式，如果某个地址读取不到，对应的列表索引返回是null
     *
     * @param addressRead address wrapper list
     * @return byte array list
     */
    public List<byte[]> readMultiByteUnsafe(MultiAddressRead addressRead) {
        List<DataItem> dataItems = this.readS7DataUnsafe(addressRead.getRequestItems());
        return dataItems.stream().map(DataItem::getData).collect(Collectors.toList());
    }

    /**
     * Read byte.
     * (单地址字节数据读取)
     *
     * @param address address string
     * @param count   byte count
     * @return byte array
     */
    public byte[] readByte(String address, int count) {
        DataItem dataItem = this.readS7Data(AddressUtil.parseByte(address, count));
        return dataItem.getData();
    }

    /**
     * Read byte.
     * (读取一个字节)
     *
     * @param address address string
     * @return byte
     */
    public byte readByte(String address) {
        return this.readByte(address, 1)[0];
    }

    /**
     * Read boolean.
     * (读取一个boolean)
     *
     * @param address address string
     * @return boolean
     */
    public boolean readBoolean(String address) {
        DataItem dataItem = this.readS7Data(AddressUtil.parseBit(address));
        return BooleanUtil.getValue(dataItem.getData()[0], 0);
    }

    /**
     * Read boolean.
     * (读取多个个boolean值)
     *
     * @param address address string
     * @return boolean list
     */
    public List<Boolean> readBoolean(String... address) {
        return this.readBoolean(Arrays.asList(address));
    }

    /**
     * Read boolean.
     * (读取boolean列表)
     *
     * @param addresses address strings
     * @return boolean list
     */
    public List<Boolean> readBoolean(List<String> addresses) {
        List<RequestItem> requestItems = addresses.stream().map(AddressUtil::parseBit).collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7Data(requestItems);
        return dataItems.stream().map(x -> BooleanUtil.getValue(x.getData()[0], 0)).collect(Collectors.toList());
    }

    public List<Boolean> readBooleanUnsafe(String... address) {
        return this.readBooleanUnsafe(Arrays.asList(address));
    }

    public List<Boolean> readBooleanUnsafe(List<String> addresses) {
        List<RequestItem> requestItems = addresses.stream().map(AddressUtil::parseBit).collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7DataUnsafe(requestItems);
        return dataItems.stream().map(x -> !EReturnCode.SUCCESS.equals(x.getReturnCode()) ? null :
                BooleanUtil.getValue(x.getData()[0], 0)).collect(Collectors.toList());
    }

    /**
     * Read int16, 2-bytes.
     * (读取一个Int16 2字节数据)
     *
     * @param address address string
     * @return Int16
     */
    public short readInt16(String address) {
        DataItem dataItem = this.readS7Data(AddressUtil.parseByte(address, 2));
        return ShortUtil.toInt16(dataItem.getData());
    }

    /**
     * Read int16, 2-bytes.
     * (读取Int16 2字节数据列表)
     *
     * @param address address string
     * @return Int16 list
     */
    public List<Short> readInt16(String... address) {
        return this.readInt16(Arrays.asList(address));
    }

    /**
     * Read int16, 2-bytes.
     * (读取Int16 2字节数据列表)
     *
     * @param addresses address strings
     * @return Int16 list
     */
    public List<Short> readInt16(List<String> addresses) {
        List<RequestItem> requestItems = addresses.stream().map(x -> AddressUtil.parseByte(x, 2)).collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7Data(requestItems);
        return dataItems.stream().map(x -> ShortUtil.toInt16(x.getData())).collect(Collectors.toList());
    }

    /**
     * Read uint16, 2-bytes.
     * (读取一个UInt16 2字节数据)
     *
     * @param address address string
     * @return UInt16
     */
    public int readUInt16(String address) {
        DataItem dataItem = this.readS7Data(AddressUtil.parseByte(address, 2));
        return ShortUtil.toUInt16(dataItem.getData());
    }

    /**
     * Read uint16, 2-bytes.
     * (读取UInt16 2字节数据列表)
     *
     * @param address address string
     * @return UInt16 list
     */
    public List<Integer> readUInt16(String... address) {
        return this.readUInt16(Arrays.asList(address));
    }

    /**
     * Read uint16, 2-bytes.
     * (读取UInt16 2字节数据列表)
     *
     * @param addresses address strings
     * @return UInt16 list
     */
    public List<Integer> readUInt16(List<String> addresses) {
        List<RequestItem> requestItems = addresses.stream().map(x -> AddressUtil.parseByte(x, 2)).collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7Data(requestItems);
        return dataItems.stream().map(x -> ShortUtil.toUInt16(x.getData())).collect(Collectors.toList());
    }

    /**
     * Read int32, 4-bytes.
     * (读取一个Int32 4字节数据)
     *
     * @param address address string
     * @return UInt32
     */
    public int readInt32(String address) {
        DataItem dataItem = this.readS7Data(AddressUtil.parseByte(address, 4));
        return IntegerUtil.toInt32(dataItem.getData());
    }

    /**
     * Read int32, 4-bytes.
     * (读取UInt32 4字节数据列表)
     *
     * @param address address string
     * @return UInt32 list
     */
    public List<Integer> readInt32(String... address) {
        return this.readInt32(Arrays.asList(address));
    }

    /**
     * Read int32, 4-bytes.
     * (读取UInt32 4字节数据列表)
     *
     * @param addresses address strings
     * @return UInt32 list
     */
    public List<Integer> readInt32(List<String> addresses) {
        List<RequestItem> requestItems = addresses.stream().map(x -> AddressUtil.parseByte(x, 4)).collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7Data(requestItems);
        return dataItems.stream().map(x -> IntegerUtil.toInt32(x.getData())).collect(Collectors.toList());
    }

    /**
     * Read uint32, 4-bytes.
     * (读取一个UInt32 4字节数据)
     *
     * @param address address string
     * @return UInt32
     */
    public long readUInt32(String address) {
        DataItem dataItem = this.readS7Data(AddressUtil.parseByte(address, 4));
        return IntegerUtil.toUInt32(dataItem.getData());
    }

    /**
     * Read uint32, 4-bytes.
     * (读取UInt32 4字节数据列表)
     *
     * @param address address string
     * @return UInt32 list
     */
    public List<Long> readUInt32(String... address) {
        return this.readUInt32(Arrays.asList(address));
    }

    /**
     * Read uint32, 4-bytes.
     * (读取UInt32 4字节数据列表)
     *
     * @param addresses address strings
     * @return UInt32 list
     */
    public List<Long> readUInt32(List<String> addresses) {
        List<RequestItem> requestItems = addresses.stream().map(x -> AddressUtil.parseByte(x, 4)).collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7Data(requestItems);
        return dataItems.stream().map(x -> IntegerUtil.toUInt32(x.getData())).collect(Collectors.toList());
    }

    /**
     * Read int64, 8-bytes.
     * (读取一个Int64 8字节数据)
     *
     * @param address address string
     * @return Int64
     */
    public long readInt64(String address) {
        DataItem dataItem = this.readS7Data(AddressUtil.parseByte(address, 8));
        return LongUtil.toInt64(dataItem.getData());
    }

    /**
     * Read int64, 8-bytes.
     * (读取Int64 8字节数据列表)
     *
     * @param address address string
     * @return Int64 list
     */
    public List<Long> readInt64(String... address) {
        return this.readInt64(Arrays.asList(address));
    }

    /**
     * Read int64, 8-bytes.
     * (读取Int64 8字节数据列表)
     *
     * @param addresses address strings
     * @return Int64 list
     */
    public List<Long> readInt64(List<String> addresses) {
        List<RequestItem> requestItems = addresses.stream().map(x -> AddressUtil.parseByte(x, 8)).collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7Data(requestItems);
        return dataItems.stream().map(x -> LongUtil.toInt64(x.getData())).collect(Collectors.toList());
    }

    /**
     * Read float32, 4-bytes.
     * (读取一个Float32的数据)
     *
     * @param address address string
     * @return Float32
     */
    public float readFloat32(String address) {
        DataItem dataItem = this.readS7Data(AddressUtil.parseByte(address, 4));
        return FloatUtil.toFloat32(dataItem.getData());
    }

    /**
     * Read float32, 4-bytes.
     * (读取多个Float32的数据)
     *
     * @param address address string
     * @return Float32 list
     */
    public List<Float> readFloat32(String... address) {
        return this.readFloat32(Arrays.asList(address));
    }

    /**
     * Read float32, 4-bytes.
     * (读取多个Float32的数据)
     *
     * @param addresses address strings
     * @return Float32 list
     */
    public List<Float> readFloat32(List<String> addresses) {
        List<RequestItem> requestItems = addresses.stream().map(x -> AddressUtil.parseByte(x, 4)).collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7Data(requestItems);
        return dataItems.stream().map(x -> FloatUtil.toFloat32(x.getData())).collect(Collectors.toList());
    }

    /**
     * Read float64, 8-bytes.
     * (读取一个Float64的数据)
     *
     * @param address address string
     * @return Float64
     */
    public double readFloat64(String address) {
        DataItem dataItem = this.readS7Data(AddressUtil.parseByte(address, 8));
        return FloatUtil.toFloat64(dataItem.getData());
    }

    /**
     * Read float64, 8-bytes.
     * (读取多个Float64的数据)
     *
     * @param address 多个地址
     * @return Float64 list
     */
    public List<Double> readFloat64(String... address) {
        return this.readFloat64(Arrays.asList(address));
    }

    /**
     * Read float64, 8-bytes.
     * (读取多个Float64的数据)
     *
     * @param addresses address strings
     * @return Float64 list
     */
    public List<Double> readFloat64(List<String> addresses) {
        List<RequestItem> requestItems = addresses.stream().map(x -> AddressUtil.parseByte(x, 8)).collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7Data(requestItems);
        return dataItems.stream().map(x -> FloatUtil.toFloat64(x.getData())).collect(Collectors.toList());
    }

    /**
     * Read string.
     * (读取字符串)
     * String（字符串）数据类型存储一串单字节字符，
     * S1200（非S200SMART）:String提供了最大256个字节，前两个字节分别表示字节中最大的字符数和当前的字符数，定义字符串的最大长度可以减少它的占用存储空间
     * S200SMART:字符串由变量存储时，字符串长度为0至254个字符，最长为255个字节，其中第一个字符为长度字节
     *
     * @param address address string
     * @return string
     */
    public String readString(String address) {
        int offset = this.plcType == EPlcType.S200_SMART ? 1 : 2;
        DataItem dataItem = this.readS7Data(AddressUtil.parseByte(address, offset));
        int length = ByteUtil.toUInt8(dataItem.getData(), offset - 1);
        dataItem = this.readS7Data(AddressUtil.parseByte(address, offset + length));
        return ByteUtil.toStr(dataItem.getData(), offset, length, Charset.forName("GB2312"));
    }

    /**
     * Read string.
     * (读取字符串)
     * S1200（非S200SMART）:数据类型为 string 的操作数可存储多个字符，最多可包括 254 个字符。字符串中的第一个字节为总长度，第二个字节为有效字符数量。
     * S200SMART:字符串由变量存储时，字符串长度为0至254个字符，最长为255个字节，其中第一个字符为长度字节
     *
     * @param address address string
     * @param length  string length
     * @return string
     */
    public String readString(String address, int length) {
        if (length <= 0 || length > 254) {
            throw new IllegalArgumentException("length <= 0 || length > 254");
        }
        int offset = this.plcType == EPlcType.S200_SMART ? 1 : 2;
        DataItem dataItem = this.readS7Data(AddressUtil.parseByte(address, offset + length));
        int actLength = ByteUtil.toUInt8(dataItem.getData(), offset - 1);
        return ByteUtil.toStr(dataItem.getData(), offset, Math.min(actLength, length), Charset.forName("GB2312"));
    }

//    /**
//     * 读取字符串
//     * Wsting数据类型与sting数据类型接近，支持单字值的较长字符串，
//     * 第一个字包含最大总字符数，下一个字包含的是当前的总字符数，接下来的字符串可含最多65534个字
//     *
//     * @param address address string
//     * @return 字符串
//     */
//    public String readWString(String address) {
//        DataItem dataItem = this.readS7Data(AddressUtil.parseByte(address, 4));
//        int type = ShortUtil.toUInt16(dataItem.getData());
//        if (type == 0 || type == 65535) {
//            throw new S7CommException("该地址的值不是字符串WString类型");
//        }
//        int length = ShortUtil.toUInt16(dataItem.getData(), 2);
//        dataItem = this.readS7Data(AddressUtil.parseByte(address, 4 + length * 2));
//        return ByteUtil.toStr(dataItem.getData(), 4);
//    }

    /**
     * Read time: milliseconds, ms, for example, 1000ms, 4-bytes.
     * (读取时间，时间为毫秒时间，ms，例如1000ms)
     *
     * @param address address string
     * @return time，ms
     */
    public long readTime(String address) {
        return this.readUInt32(address);
    }

    /**
     * Read date, for example, 2023-04-04, 2-bytes.
     * (读取日期，例如：2023-04-04)
     *
     * @param address address string
     * @return date
     */
    public LocalDate readDate(String address) {
        int offset = this.readUInt16(address);
        return LocalDate.of(1990, 1, 1).plusDays(offset);
    }

    /**
     * Read the time of day, for example: 23:56:31, 4-bytes.
     * (读取一天中的时间，例如：23:56:31)
     *
     * @param address address string
     * @return localTime
     */
    public LocalTime readTimeOfDay(String address) {
        long value = this.readUInt32(address);
        return LocalTime.ofSecondOfDay(value / 1000);
    }

    /**
     * Read date and time, 12-bytes.
     * (读取日期和时间的数据类型)
     *
     * @param address address string
     * @return LocalDateTime
     */
    public LocalDateTime readDTL(String address) {
        byte[] bytes = this.readByte(address, 12);
        ByteReadBuff buff = ByteReadBuff.newInstance(bytes);
        int year = buff.getUInt16();
        int month = buff.getByteToInt();
        int dayOfMonth = buff.getByteToInt();
        int week = buff.getByteToInt();
        int hour = buff.getByteToInt();
        int minute = buff.getByteToInt();
        int second = buff.getByteToInt();
        long nanoOfSecond = buff.getUInt32();
        return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, (int) nanoOfSecond);
    }

    //endregion

    //region 写入数据

    /**
     * The most primitive way to write raw data
     * (最原始的方式写入生数据)
     *
     * @param variableType     variable type 参数类型
     * @param count            byte count 数据个数
     * @param area             data area区域
     * @param dbNumber         db number DB块编号
     * @param byteAddress      byte address 字节地址
     * @param bitAddress       bit address 位地址
     * @param dataVariableType data variable type 数据变量类型
     * @param data             byte array data 数据字节数组
     */
    public void writeRaw(EParamVariableType variableType, int count, EArea area, int dbNumber, int byteAddress,
                         int bitAddress, EDataVariableType dataVariableType, byte[] data) {
        if (count <= 0) {
            throw new IllegalArgumentException("count<=0");
        }
        if (dbNumber < 0) {
            throw new IllegalArgumentException("dbNumber<0");
        }
        if (byteAddress < 0) {
            throw new IllegalArgumentException("byteAddress<0");
        }
        if (bitAddress < 0 || bitAddress > 7) {
            throw new IllegalArgumentException("bitAddress<0||bitAddress>7");
        }
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("data");
        }
        RequestItem requestItem = RequestItem.createByParams(variableType, count, area, dbNumber, byteAddress, bitAddress);
        DataItem dataItem = DataItem.createReq(data, dataVariableType);
        this.writeS7Data(requestItem, dataItem);
    }

    /**
     * Write boolean.
     * (写入boolean数据)
     *
     * @param address address string
     * @param data    boolean
     */
    public void writeBoolean(String address, boolean data) {
        this.writeS7Data(AddressUtil.parseBit(address), DataItem.createReqByBoolean(data));
    }

    /**
     * Write byte.
     * (写入字节数据)
     *
     * @param address address string
     * @param data    byte
     */
    public void writeByte(String address, byte data) {
        this.writeS7Data(AddressUtil.parseByte(address, 1), DataItem.createReqByByte(data));
    }

    /**
     * Write byte.
     * (写入字节列表数据)
     *
     * @param address address string
     * @param data    byte array
     */
    public void writeByte(String address, byte[] data) {
        this.writeS7Data(AddressUtil.parseByte(address, data.length), DataItem.createReqByByte(data));
    }

    /**
     * Write uint16, 2-bytes.
     * (写入UInt16数据)
     *
     * @param address address string
     * @param data    UInt16
     */
    public void writeUInt16(String address, int data) {
        this.writeByte(address, ShortUtil.toByteArray(data));
    }

    /**
     * Write int16, 2-bytes.
     * (写入Int16数据)
     *
     * @param address address string
     * @param data    Int16
     */
    public void writeInt16(String address, short data) {
        this.writeByte(address, ShortUtil.toByteArray(data));
    }

    /**
     * Write uint32, 4-bytes.
     * (写入UInt32数据)
     *
     * @param address address string
     * @param data    UInt32
     */
    public void writeUInt32(String address, long data) {
        this.writeByte(address, IntegerUtil.toByteArray(data));
    }

    /**
     * Write int32, 4-bytes.
     * (写入Int32数据)
     *
     * @param address address string
     * @param data    Int32
     */
    public void writeInt32(String address, int data) {
        this.writeByte(address, IntegerUtil.toByteArray(data));
    }

    /**
     * Write int64, 8-bytes.
     * (写入Int64数据)
     *
     * @param address address string
     * @param data    Int64
     */
    public void writeInt64(String address, long data) {
        this.writeByte(address, LongUtil.toByteArray(data));
    }

    /**
     * Write float32, 4-bytes.
     * (写入Float32数据)
     *
     * @param address address string
     * @param data    Float32
     */
    public void writeFloat32(String address, float data) {
        this.writeByte(address, FloatUtil.toByteArray(data));
    }

    /**
     * Write float64, 8-bytes.
     * (写入Float64数据)
     *
     * @param address address string
     * @param data    Float64
     */
    public void writeFloat64(String address, double data) {
        this.writeByte(address, FloatUtil.toByteArray(data));
    }

    /**
     * Write data to multiple addresses
     * (多地址写入数据)
     *
     * @param addressWrite addresses for writing
     */
    public void writeMultiData(MultiAddressWrite addressWrite) {
        this.writeS7Data(addressWrite.getRequestItems(), addressWrite.getDataItems());
    }

    /**
     * Write string.
     * (写入字符串数据)
     * String（字符串）数据类型存储一串单字节字符，
     * String提供了最大256个字节，前两个字节分别表示字节中最大的字符数和当前的字符数，定义字符串的最大长度可以减少它的占用存储空间
     * S1200:数据类型为 string 的操作数可存储多个字符，最多可包括 254 个字符。字符串中的第一个字节为总长度，第二个字节为有效字符数量。
     * S200SMART:字符串由变量存储时，字符串长度为0至254个字符，最长为255个字节，其中第一个字符为长度字节
     *
     * @param address address string
     * @param data    string data
     */
    public void writeString(String address, String data) {
        if (data == null) {
            throw new IllegalArgumentException("data=null");
        }
        int offset = this.plcType == EPlcType.S200_SMART ? 0 : 1;
        // 填充字节长度数据
        byte[] dataBytes = data.length() == 0 ? new byte[0] : data.getBytes(Charset.forName("GB2312"));
        byte[] tmp = new byte[1 + dataBytes.length];
        tmp[0] = ByteUtil.toByte(dataBytes.length);
        System.arraycopy(dataBytes, 0, tmp, 1, dataBytes.length);
        // 字节索引+1
        RequestItem requestItem = AddressUtil.parseByte(address, tmp.length);
        requestItem.setByteAddress(requestItem.getByteAddress() + offset);
        // 通信交互
        this.writeS7Data(requestItem, DataItem.createReqByByte(tmp));
    }

//    /**
//     * 写入字符串数据
//     * Wsting数据类型与sting数据类型接近，支持单字值的较长字符串，
//     * 第一个字包含最大总字符数，下一个字包含的是当前的总字符数，接下来的字符串可含最多65534个字
//     *
//     * @param address address string
//     * @param data    字符串数据
//     */
//    public void writeWString(String address, String data) {
//        if (data.length() > (65534*2-4)) {
//            throw new IllegalArgumentException("data字符串参数过长");
//        }
//        byte[] dataBytes = data.getBytes(StandardCharsets.US_ASCII);
//        byte[] tmp = new byte[2 + dataBytes.length];
//        byte[] lengthBytes = ShortUtil.toByteArray(dataBytes.length / 2);
//        tmp[0] = (byte) 0xFF;
//        tmp[1] = (byte) 0xFE;
//        tmp[2] = lengthBytes[0];
//        tmp[3] = lengthBytes[1];
//        System.arraycopy(dataBytes, 0, tmp, 4, dataBytes.length);
//        this.writeByte(address, tmp);
//    }

    /**
     * Write time, the time is milliseconds, ms, 4-bytes.
     * (写入时间，时间为毫秒时间，ms)
     *
     * @param address address string
     * @param time    time，ms
     */
    public void writeTime(String address, long time) {
        this.writeUInt32(address, time);
    }

    /**
     * Write date, 2-bytes.
     * (写入日期)
     *
     * @param address address string
     * @param date    date
     */
    public void writeDate(String address, LocalDate date) {
        LocalDate start = LocalDate.of(1990, 1, 1);
        long value = date.toEpochDay() - start.toEpochDay();
        this.writeUInt16(address, (int) value);
    }

    /**
     * Write the time of day, 4-bytes.
     * (写入一天中的时间)
     *
     * @param address address string
     * @param time    time of day
     */
    public void writeTimeOfDay(String address, LocalTime time) {
        int value = time.toSecondOfDay();
        this.writeUInt32(address, (long) value * 1000);
    }

    /**
     * Write date and time, 12-bytes.
     * (写入具体的时间)
     *
     * @param address  address string
     * @param dateTime LocalDateTime
     */
    public void writeDTL(String address, LocalDateTime dateTime) {
        byte[] data = ByteWriteBuff.newInstance(12)
                .putShort(dateTime.getYear())
                .putByte(dateTime.getMonthValue())
                .putByte(dateTime.getDayOfMonth())
                .putByte(dateTime.getDayOfWeek().getValue())
                .putByte(dateTime.getHour())
                .putByte(dateTime.getMinute())
                .putByte(dateTime.getSecond())
                .putInteger(dateTime.getNano())
                .getData();
        this.writeByte(address, data);
    }

    //endregion

    //region 控制部分

    /**
     * Hot restart.
     * (热重启)
     */
    public void hotRestart() {
        this.readFromServerByPersistence(S7Data.createHotRestart());
    }

    /**
     * Cold restart.
     * (冷重启)
     */
    public void coldRestart() {
        this.readFromServerByPersistence(S7Data.createColdRestart());
    }

    /**
     * Plc stop.
     * (PLC停止)
     */
    public void plcStop() {
        this.readFromServerByPersistence(S7Data.createPlcStop());
    }

    /**
     * Copy ram to rom.
     * (将ram复制到rom)
     */
    public void copyRamToRom() {
        this.readFromServerByPersistence(S7Data.createCopyRamToRom());
    }

    /**
     * Compress.
     * (压缩)
     */
    public void compress() {
        this.readFromServerByPersistence(S7Data.createCompress());
    }

    /**
     * Insert file command.
     * (创建插入文件指令)
     *
     * @param blockType   block type 块类型
     * @param blockNumber block number 块编号
     */
    public void insert(EFileBlockType blockType, int blockNumber) {
        this.readFromServerByPersistence(S7Data.createInsert(blockType, blockNumber, EDestinationFileSystem.P));
    }

    //endregion

    //region NCK

    //    04.08.07.00.020
    //    828D_04.08
    //    16/11/21 20:34:20
    //    828D-ME42
    //    SOC2
    //    machineTool

    /**
     * Read cnc id.
     * CNC的ID<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 01 46 6E 00 01 1A 01 <br>
     * 接收[57]：03 00 00 39 02 F0 80 32 03 00 00 00 00 00 02 00 24 00 00 04 01 FF 09 00 20 30 30 30 30 36 30 31 39 33 30 38 38 46 43 30 30 30 30 37 35 00 00 00 00 00 00 00 00 00 00 00 00
     *
     * @return data
     */
    public String readCncId() {
        // 12 08 82 01 46 6E 00 01 1A 01
        RequestNckItem requestNckItem = new RequestNckItem(ENckArea.N_NCK, 1, 18030, 1, ENckModule.M, 1);
        DataItem dataItem = this.readS7NckData(requestNckItem);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getString(dataItem.getCount()).trim();
    }

    /**
     * Read cnc version.
     * CNC的Version<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 01 46 78 00 01 1A 01 <br>
     * 接收[45]：03 00 00 2D 02 F0 80 32 03 00 00 00 00 00 02 00 18 00 00 04 01 FF 09 00 14 30 34 2E 30 38 2E 30 37 2E 30 30 2E 30 32 30 20 20 20 20 00
     *
     * @return data
     */
    public String readCncVersion() {
        // 12 08 82 01 46 78 00 01 1A 01
        RequestNckItem requestNckItem = new RequestNckItem(ENckArea.N_NCK, 1, 18040, 1, ENckModule.M, 1);
        DataItem dataItem = this.readS7NckData(requestNckItem);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getString(dataItem.getCount()).trim();
    }

    /**
     * Read cnc type1.
     * 类型<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 01 46 78 00 02 1A 01<br>
     * 接收[45]：03 00 00 2D 02 F0 80 32 03 00 00 00 00 00 02 00 18 00 00 04 01 FF 09 00 14 38 32 38 44 5F 30 34 2E 30 38 20 20 20 20 20 00 00 00 00 00
     *
     * @return data
     */
    public String readCncType1() {
        // 12 08 82 01 46 78 00 01 1A 01
        RequestNckItem requestNckItem = new RequestNckItem(ENckArea.N_NCK, 1, 18040, 2, ENckModule.M, 1);
        DataItem dataItem = this.readS7NckData(requestNckItem);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getString(dataItem.getCount()).trim();
    }

    /**
     * Read cnc manufacture date.
     * CNC的生产日期<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 01 46 78 00 01 1A 01<br>
     * 接收[45]：03 00 00 2D 02 F0 80 32 03 00 00 00 00 00 02 00 18 00 00 04 01 FF 09 00 14 30 34 2E 30 38 2E 30 37 2E 30 30 2E 30 32 30 20 20 20 20 00
     *
     * @return data
     */
    public String readCncManufactureDate() {
        // 12 08 82 01 46 78 00 03 1A 01
        RequestNckItem requestNckItem = new RequestNckItem(ENckArea.N_NCK, 1, 18040, 3, ENckModule.M, 1);
        DataItem dataItem = this.readS7NckData(requestNckItem);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getString(dataItem.getCount()).trim();
    }

    /**
     * Read cnc type.
     * CNC的Type<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 01 46 78 00 04 1A 01<br>
     * 接收[45]：03 00 00 2D 02 F0 80 32 03 00 00 00 00 00 02 00 18 00 00 04 01 FF 09 00 14 38 32 38 44 2D 4D 45 34 32 00 00 00 00 00 00 00 00 00 00 00
     *
     * @return data
     */
    public String readCncType() {
        // 12 08 82 01 46 78 00 04 1A 01
        RequestNckItem requestNckItem = new RequestNckItem(ENckArea.N_NCK, 1, 18040, 4, ENckModule.M, 1);
        DataItem dataItem = this.readS7NckData(requestNckItem);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getString(dataItem.getCount()).trim();
    }

    /**
     * Read machine position.
     * 获取机械坐标系<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 13 00 0C 00 00 04 01 12 08 82 41 00 02 00 01 74 01<br>
     * 接收[33]：03 00 00 21 02 F0 80 32 03 00 00 00 13 00 02 00 0C 00 00 04 01 FF 09 00 08 CD CC CC CC CC 6C 61 40<br>
     * <p>
     * 另一种方式也可以一个request，lineCount=3，结果有3个数据<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 02 00 0C 00 00 04 01 12 08 82 41 00 02 00 01 74 03<br>
     * 接收[49]：03 00 00 31 02 F0 80 32 03 00 00 00 02 00 02 00 1C 00 00 04 01 FF 09 00 18 D8 B6 28 B3 41 26 69 3F 2D 43 1C EB E2 36 3A BF E7 52 5C 55 F6 5D 41 3F
     *
     * @return data
     */
    public List<Double> readMachinePosition() {
        // 12 08 82 41 00 02 00 01 74 01
        // 12 08 82 41 00 02 00 02 74 01
        // 12 08 82 41 00 02 00 03 74 01
        List<RequestNckItem> requestNckItems = IntStream.of(1, 2, 3, 4)
                .mapToObj(x -> new RequestNckItem(ENckArea.C_CHANNEL, 1, 2, x, ENckModule.SMA, 1))
                .collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7NckData(requestNckItems);
        return dataItems.stream().map(x -> ByteReadBuff.newInstance(x.getData(), true).getFloat64())
                .collect(Collectors.toList());
    }

    /**
     * Read relative position.
     * 获取相对坐标系<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 13 00 0C 00 00 04 01 12 08 82 41 00 19 00 01 70 01<br>
     * 接收[33]：03 00 00 21 02 F0 80 32 03 00 00 00 13 00 02 00 0C 00 00 04 01 FF 09 00 08 5B B6 D6 17 89 2D C8 40
     *
     * @return data
     */
    public List<Double> readRelativePosition() {
        // 12 08 82 41 00 19 00 01 70 01
        // 12 08 82 41 00 19 00 02 70 01
        // 12 08 82 41 00 19 00 03 70 01
        List<RequestNckItem> requestNckItems = IntStream.of(1, 2, 3, 4)
                .mapToObj(x -> new RequestNckItem(ENckArea.C_CHANNEL, 1, 25, x, ENckModule.SEGA, 1))
                .collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7NckData(requestNckItems);
        return dataItems.stream().map(x -> ByteReadBuff.newInstance(x.getData(), true).getFloat64())
                .collect(Collectors.toList());
    }

    /**
     * Read remain position.
     * 获取剩余坐标系<br>
     * 发送[59]：03 00 00 3B 02 F0 80 32 01 00 00 00 00 00 2A 00 00 04 04 12 08 82 41 00 03 00 01 74 01 12 08 82 41 00 03 00 02 74 01 12 08 82 41 00 03 00 03 74 01 12 08 82 41 00 03 00 04 74 01<br>
     * 接收[69]：03 00 00 45 02 F0 80 32 03 00 00 00 00 00 02 00 30 00 00 04 04 FF 09 00 08 00 00 00 00 00 00 00 00 FF 09 00 08 00 00 00 00 00 00 00 00 FF 09 00 08 00 00 00 00 00 00 00 00 FF 09 00 08 00 00 00 00 00 00 00 00
     *
     * @return data
     */
    public List<Double> readRemainPosition() {
        // 12 08 82 41 00 03 00 01 74 01
        // 12 08 82 41 00 03 00 02 74 01
        // 12 08 82 41 00 03 00 03 74 01
        List<RequestNckItem> requestNckItems = IntStream.of(1, 2, 3, 4)
                .mapToObj(x -> new RequestNckItem(ENckArea.C_CHANNEL, 1, 3, x, ENckModule.SMA, 1))
                .collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7NckData(requestNckItems);
        return dataItems.stream().map(x -> ByteReadBuff.newInstance(x.getData(), true).getFloat64())
                .collect(Collectors.toList());
    }

    /**
     * Read T work piece position.
     * T工件坐标<br>
     * 发送[49]：03 00 00 31 02 F0 80 32 01 00 00 00 00 00 20 00 00 04 03 12 08 82 41 00 01 00 04 12 01 12 08 82 41 00 01 00 05 12 01 12 08 82 41 00 01 00 06 12 01<br>
     * 接收[57]：03 00 00 39 02 F0 80 32 03 00 00 00 00 00 02 00 24 00 00 04 03 FF 09 00 08 00 00 00 00 00 00 00 80 FF 09 00 08 00 00 00 00 00 00 00 80 FF 09 00 08 00 00 00 00 00 00 00 80
     *
     * @return data
     */
    public List<Double> readTWorkPiecePosition() {
        // 12 08 82 41 00 01 00 04 12 01
        // 12 08 82 41 00 01 00 05 12 01
        // 12 08 82 41 00 01 00 06 12 01
        List<RequestNckItem> requestNckItems = IntStream.of(4, 5, 6)
                .mapToObj(x -> new RequestNckItem(ENckArea.C_CHANNEL, 1, 1, x, ENckModule.FU, 1))
                .collect(Collectors.toList());
        List<DataItem> dataItems = this.readS7NckData(requestNckItems);
        return dataItems.stream().map(x -> ByteReadBuff.newInstance(x.getData(), true).getFloat64())
                .collect(Collectors.toList());
    }

    /**
     * Read tool radius compensation number.
     * 刀具半径补偿编号<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 13 00 0C 00 00 04 01 12 08 82 41 00 23 00 01 7F 01<br>
     * 接收[57]：03 00 00 39 02 F0 80 32 03 00 00 00 13 00 02 00 24 00 00 04 01 FF 09 00 20 32 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
     *
     * @return data
     */
    public int readToolRadiusCompensationNumber() {
        // 12 08 82 41 00 23 00 01 7F 01
        RequestNckItem requestNckItem = new RequestNckItem(ENckArea.C_CHANNEL, 1, 35, 1, ENckModule.S, 1);
        DataItem dataItem = this.readS7NckData(requestNckItem);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getUInt16();
    }

    /**
     * Read tool number.
     * 刀具编号<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 13 00 0C 00 00 04 01 12 08 82 41 00 17 00 01 7F 01<br>
     * 接收[27]：03 00 00 1B 02 F0 80 32 03 00 00 00 13 00 02 00 06 00 00 04 01 FF 09 00 02 01 00
     *
     * @return data
     */
    public int readToolNumber() {
        // 12 08 82 41 00 17 00 01 7F 01
        RequestNckItem requestNckItem = new RequestNckItem(ENckArea.C_CHANNEL, 1, 23, 1, ENckModule.S, 1);
        DataItem dataItem = this.readS7NckData(requestNckItem);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getUInt16();
    }

    /**
     * Read act spindle speed.
     * 实际主轴转速<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 13 00 0C 00 00 04 01 12 08 82 41 00 02 00 01 72 01<br>
     * 接收[33]：03 00 00 21 02 F0 80 32 03 00 00 00 13 00 02 00 0C 00 00 04 01 FF 09 00 08 00 00 00 00 00 00 00 00
     *
     * @return data
     */
    public double readActSpindleSpeed() {
        // 12 08 82 41 00 02 00 01 72 01
        RequestNckItem item = new RequestNckItem(ENckArea.C_CHANNEL, 1, 2, 1, ENckModule.SSP, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getFloat64();
    }

    /**
     * Read set spindle speed.
     * 设定主轴转速<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 41 00 04 00 01 72 01<br>
     * 接收[33]：03 00 00 21 02 F0 80 32 03 00 00 00 00 00 02 00 0C 00 00 04 01 FF 09 00 08 00 00 00 00 00 00 59 40
     *
     * @return data
     */
    public double readSetSpindleSpeed() {
        // 12 08 82 01 00 03 00 04 72 01
        RequestNckItem item = new RequestNckItem(ENckArea.N_NCK, 1, 3, 4, ENckModule.SSP, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getFloat64();
    }

    /**
     * Read spindle rate.
     * 主轴速率<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 41 00 04 00 01 72 01<br>
     * 接收[33]：03 00 00 21 02 F0 80 32 03 00 00 00 00 00 02 00 0C 00 00 04 01 FF 09 00 08 00 00 00 00 00 00 59 40
     *
     * @return data
     */
    public double readSpindleRate() {
        // 12 08 82 41 00 04 00 01 72 01
        RequestNckItem item = new RequestNckItem(ENckArea.C_CHANNEL, 1, 4, 1, ENckModule.SSP, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getFloat64();
    }

    /**
     * Read feed rate.
     * 进给速率<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 13 00 0C 00 00 04 01 12 08 82 41 00 03 00 01 7F 01<br>
     * 接收[33]：03 00 00 21 02 F0 80 32 03 00 00 00 13 00 02 00 0C 00 00 04 01 FF 09 00 08 00 00 00 00 00 00 00 00
     *
     * @return data
     */
    public double readFeedRate() {
        // 12 08 82 41 00 03 00 01 7F 01
        RequestNckItem item = new RequestNckItem(ENckArea.C_CHANNEL, 1, 3, 1, ENckModule.S, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getFloat64();
    }

    /**
     * Read set feed rate.
     * 获取设定进给速率<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 13 00 0C 00 00 04 01 12 08 82 41 00 02 00 01 7F 01<br>
     * 接收[33]：03 00 00 21 02 F0 80 32 03 00 00 00 13 00 02 00 0C 00 00 04 01 FF 09 00 08 00 00 00 00 00 00 00 00
     *
     * @return data
     */
    public double readSetFeedRate() {
        // 12 08 82 41 00 02 00 01 7F 01
        RequestNckItem item = new RequestNckItem(ENckArea.C_CHANNEL, 1, 2, 1, ENckModule.S, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getFloat64();
    }

    /**
     * Read act feed rate.
     * 获取实际进给速率<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 13 00 0C 00 00 04 01 12 08 82 41 00 01 00 01 7F 01<br>
     * 接收[33]：03 00 00 21 02 F0 80 32 03 00 00 00 13 00 02 00 0C 00 00 04 01 FF 09 00 08 00 00 00 00 00 00 00 00
     *
     * @return data
     */
    public double readActFeedRate() {
        // 12 08 82 41 00 01 00 01 7F 01
        RequestNckItem item = new RequestNckItem(ENckArea.C_CHANNEL, 1, 1, 1, ENckModule.S, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getFloat64();
    }

    /**
     * Read work mode.
     * 工作模式的请求，0:JOG, 1:MDA, 2:AUTO, 其他<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 21 00 03 00 01 7F 01<br>
     * 接收[27]：03 00 00 1B 02 F0 80 32 03 00 00 00 00 00 02 00 06 00 00 04 01 FF 09 00 02 00 00
     *
     * @return data
     */
    public int readWorkMode() {
        // 12 08 82 21 00 03 00 01 7F 01
        RequestNckItem item = new RequestNckItem(ENckArea.B_MODE_GROUP, 1, 3, 1, ENckModule.S, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getUInt16();
    }

    /**
     * Read status.
     * 状态，2:stop, 1:start, 0:reset<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 41 00 0B 00 01 7F 01<br>
     * 接收[27]：03 00 00 1B 02 F0 80 32 03 00 00 00 00 00 02 00 06 00 00 04 01 FF 09 00 02 02 00
     *
     * @return data
     */
    public int readStatus() {
        // 12 08 82 41 00 0b 00 01 7F 01
        RequestNckItem item = new RequestNckItem(ENckArea.C_CHANNEL, 1, 11, 1, ENckModule.S, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getUInt16();
    }

    /**
     * Read run time.
     * 运行时间<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 41 01 29 00 01 7F 01<br>
     * 接收[33]：03 00 00 21 02 F0 80 32 03 00 00 00 00 00 02 00 0C 00 00 04 01 FF 09 00 08 00 00 00 00 00 00 00 00
     *
     * @return data
     */
    public double readRunTime() {
        // 12 08 82 41 01 29 00 01 7F 01
        RequestNckItem item = new RequestNckItem(ENckArea.C_CHANNEL, 1, 297, 1, ENckModule.S, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getFloat64();
    }

    /**
     * Read remain time.
     * 剩余时间<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 41 01 2A 00 01 7F 01<br>
     * 接收[33]：03 00 00 21 02 F0 80 32 03 00 00 00 00 00 02 00 0C 00 00 04 01 FF 09 00 08 00 00 00 00 00 00 00 00
     *
     * @return data
     */
    public double readRemainTime() {
        // 12 08 82 41 01 2A 00 01 7F 01
        RequestNckItem item = new RequestNckItem(ENckArea.C_CHANNEL, 1, 298, 1, ENckModule.S, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getFloat64();
    }

    /**
     * Read program name.
     * 程序名<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 41 00 0C 00 01 7A 01<br>
     * 接收[185]：03 00 00 B9 02 F0 80 32 03 00 00 00 00 00 02 00 A4 00 00 04 01 FF 09 00 A0 2F 5F 4E 5F 4D 50 46 30 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
     *
     * @return data
     */
    public String readProgramName() {
        // 12 08 82 41 00 0C 00 01 7A 01
        RequestNckItem item = new RequestNckItem(ENckArea.C_CHANNEL, 1, 12, 1, ENckModule.SPARPP, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getString(dataItem.getCount()).trim();
    }

    /**
     * Read alarm number.
     * 报警数量<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 01 00 07 00 01 7F 01<br>
     * 接收[27]：03 00 00 1B 02 F0 80 32 03 00 00 00 00 00 02 00 06 00 00 04 01 FF 09 00 02 05 00
     *
     * @return data
     */
    public int readAlarmNumber() {
        // 12 08 82 01 00 07 00 01 7F 01
        RequestNckItem item = new RequestNckItem(ENckArea.N_NCK, 1, 7, 1, ENckModule.S, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getUInt16();
    }

    /**
     * Read alarm info.
     * 报警信息<br>
     * 发送[29]：03 00 00 1D 02 F0 80 32 01 00 00 00 00 00 0C 00 00 04 01 12 08 82 01 00 07 00 01 7F 01<br>
     * 接收[27]：03 00 00 1B 02 F0 80 32 03 00 00 00 00 00 02 00 06 00 00 04 01 FF 09 00 02 05 00
     *
     * @return data
     */
    public long readAlarmInfo() {
        // 12 08 82 01 00 01 00 01 77 01
        // 12 08 82 01 00 04 00 01 77 01
        RequestNckItem item = new RequestNckItem(ENckArea.N_NCK, 1, 1, 1, ENckModule.SALA, 1);
        DataItem dataItem = this.readS7NckData(item);
        return ByteReadBuff.newInstance(dataItem.getData(), true).getUInt32();
    }

    //endregion
}
