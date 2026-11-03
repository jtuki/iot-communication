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

package com.github.xingshuangs.iot.protocol.s7.serializer;


import com.github.xingshuangs.iot.common.buff.ByteReadBuff;
import com.github.xingshuangs.iot.common.buff.ByteWriteBuff;
import com.github.xingshuangs.iot.common.enums.EDataType;
import com.github.xingshuangs.iot.common.serializer.IPLCSerializable;
import com.github.xingshuangs.iot.exceptions.S7CommException;
import com.github.xingshuangs.iot.protocol.s7.enums.EPlcType;
import com.github.xingshuangs.iot.protocol.s7.model.DataItem;
import com.github.xingshuangs.iot.protocol.s7.model.RequestItem;
import com.github.xingshuangs.iot.protocol.s7.service.S7PLC;
import com.github.xingshuangs.iot.protocol.s7.utils.AddressUtil;
import lombok.Data;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * S7 serializer tool class.
 * (S7序列化工具)
 *
 * @author xingshuang
 */
@Data
public class S7Serializer implements IPLCSerializable {

    private final S7PLC s7PLC;

    /**
     * 字符集  (默认US_ASCII)
     */
    private Charset charsets;

    public S7Serializer(S7PLC s7PLC) {
        this.s7PLC = s7PLC;
        this.charsets = Charset.forName("GB2312");
    }

    /**
     * Create a new instance.
     * 静态方法实例对象
     *
     * @param s7PLC plc object
     * @return serializer object
     */
    public static S7Serializer newInstance(S7PLC s7PLC) {
        return new S7Serializer(s7PLC);
    }

    /**
     * Convert to parsed data according to the S7Variable annotation.
     * 将类根据S7Variable注解转换为解析数据
     *
     * @param targetClass target object class
     * @return result data list
     */
    private List<S7ParseData> parseBean(final Class<?> targetClass) {
        List<S7ParseData> s7ParseDataList = new ArrayList<>();

        for (final Field field : targetClass.getDeclaredFields()) {
            final S7Variable s7Variable = field.getAnnotation(S7Variable.class);

            if (s7Variable == null) {
                continue;
            }

            S7Parameter parameter = new S7Parameter(s7Variable.address(), s7Variable.type(), s7Variable.count());
            S7ParseData s7ParseData = this.createS7ParseData(parameter, field);

            s7ParseDataList.add(s7ParseData);
        }
        return s7ParseDataList;
    }

    /**
     * Create a list of S7ParseData objects from S7Parameter objects.
     * (解析参数)
     *
     * @param parameters parameter list
     * @return List<S7ParseData>
     */
    private List<S7ParseData> parseBean(final List<S7Parameter> parameters) {
        try {
            List<S7ParseData> s7ParseDataList = new ArrayList<>();
            for (final S7Parameter p : parameters) {
                if (p == null) {
                    // parameters列表中存在null
                    throw new S7CommException("parameters must not contain null elements");
                }
                S7ParseData s7ParseData = this.createS7ParseData(p, p.getClass().getDeclaredField("value"));
                s7ParseDataList.add(s7ParseData);
            }
            return s7ParseDataList;
        } catch (NoSuchFieldException e) {
            throw new S7CommException(e);
        }
    }

    /**
     * Check that the data of the S7Variable meets the rule requirements.
     * (校验S7Variable的数据是否满足规则要求)
     *
     * @param parameter parameter
     */
    private void checkS7Variable(final S7Parameter parameter) {
        if (parameter.getAddress().isEmpty()) {
            // S7参数中[address]不能为空
            throw new S7CommException("S7 parameter address must not be empty");
        }
        if (parameter.getCount() < 0) {
            // S7参数中[count]不能为负数
            throw new S7CommException("S7 parameter count must not be negative");
        }
        if (parameter.getDataType() == EDataType.STRING && parameter.getCount() > 254) {
            // S7参数中字符串类型类型数据的[count]不能大于254
            throw new S7CommException("S7 parameter count must not exceed 254 for STRING data");
        }
        if (parameter.getDataType() != EDataType.BYTE && parameter.getDataType() != EDataType.STRING && parameter.getCount() > 1) {
            // S7参数中只有[type]=字节和字符串类型数据的[count]才能大于1，其他必须等于1
            throw new S7CommException("S7 parameter count must not exceed 1 unless dataType is BYTE or STRING");
        }
    }

    /**
     * Read the data according to the condition.
     * (根据条件读取数据)
     *
     * @param s7ParseDataList condition
     */
    private void readDataByCondition(List<S7ParseData> s7ParseDataList) {
        if (s7ParseDataList.isEmpty()) {
            // 解析出的注解数据个数为空，无法读取数据
            throw new S7CommException("No annotated data fields were found");
        }

        // 读取PLC数据
        List<RequestItem> requestItems = s7ParseDataList.stream().map(S7ParseData::getRequestItem).collect(Collectors.toList());
        List<DataItem> dataItems = this.s7PLC.readS7Data(requestItems);

        if (s7ParseDataList.size() != dataItems.size()) {
            // 所需的字段解析项个数与返回的数据项数量不一致，错误
            throw new S7CommException("Parsed field count does not match the returned data item count");
        }

        // 提取数据
        for (int i = 0; i < dataItems.size(); i++) {
            s7ParseDataList.get(i).setDataItem(dataItems.get(i));
        }
    }

    /**
     * Build an S7ParseData object.
     * (构建S7ParseData数据)
     *
     * @param p     S7Parameter data
     * @param field target field
     * @return S7ParseData
     */
    private S7ParseData createS7ParseData(S7Parameter p, Field field) {
        this.checkS7Variable(p);
        // 组装S7解析数据
        S7ParseData s7ParseData = new S7ParseData();
        s7ParseData.setDataType(p.getDataType());
        s7ParseData.setCount(p.getCount());
        s7ParseData.setField(field);
        if (p.getDataType() == EDataType.BOOL) {
            s7ParseData.setRequestItem(AddressUtil.parseBit(p.getAddress()));
        } else if (p.getDataType() == EDataType.STRING) {
            RequestItem requestItem = AddressUtil.parseByte(p.getAddress(), 1 + p.getCount() * p.getDataType().getByteLength());
            // 为什么字节索引+1，为了避免修改PLC中string[60]类型的第一个字节数据，该数据为字符串的允许最大长度
            // S1200（非S200Smart）:数据类型为 string 的操作数可存储多个字符，最多可包括 254 个字符。字符串中的第一个字节为总长度，第二个字节为有效字符数量。
            // S200SMART:字符串由变量存储时，字符串长度为0至254个字符，最长为255个字节，其中第一个字符为长度字节
            int offset = this.s7PLC.getPlcType() == EPlcType.S200_SMART ? 0 : 1;
            requestItem.setByteAddress(requestItem.getByteAddress() + offset);
            s7ParseData.setRequestItem(requestItem);
        } else {
            s7ParseData.setRequestItem(AddressUtil.parseByte(p.getAddress(), p.getCount() * p.getDataType().getByteLength()));
        }
        return s7ParseData;
    }

    // region read

    @Override
    public <T> T read(Class<T> targetClass) {
        List<S7ParseData> s7ParseDataList = this.parseBean(targetClass);
        this.readDataByCondition(s7ParseDataList);
        return this.fillData(targetClass, s7ParseDataList);
    }

    /**
     * Read data, fill value.
     * (读取数据)
     *
     * @param parameters parameter list
     * @return S7Parameter list
     */
    public List<S7Parameter> read(List<S7Parameter> parameters) {
        List<S7ParseData> s7ParseDataList = this.parseBean(parameters);
        this.readDataByCondition(s7ParseDataList);
        this.fillData(parameters, s7ParseDataList);
        return parameters;
    }

    /**
     * Fill data from the parsed S7 values.
     * (提取数据)
     *
     * @param targetClass     target class
     * @param s7ParseDataList S7ParseData list
     * @param <T>             type
     * @return target object.
     */
    private <T> T fillData(Class<T> targetClass, List<S7ParseData> s7ParseDataList) {
        try {
            final T result = targetClass.newInstance();
            for (S7ParseData item : s7ParseDataList) {
                this.fillField(result, item);
            }
            return result;

        } catch (Exception e) {
            throw new S7CommException("Failed to read serialized data: " + e.getMessage(), e);
        }
    }

    /**
     * Fill data from the parsed S7 values.
     * (填充数据)
     *
     * @param parameters      parameter list
     * @param s7ParseDataList List<S7ParseData>
     */
    private void fillData(final List<S7Parameter> parameters, final List<S7ParseData> s7ParseDataList) {
        try {
            for (int i = 0; i < s7ParseDataList.size(); i++) {
                S7ParseData item = s7ParseDataList.get(i);
                S7Parameter parameter = parameters.get(i);
                this.fillField(parameter, item);
            }
        } catch (Exception e) {
            throw new S7CommException("Failed to read serialized data: " + e.getMessage(), e);
        }
    }

    /**
     * Fill a field with the parsed S7 value.
     * 填充字段数据
     *
     * @param result target object
     * @param item   S7ParseData
     * @param <T>    type
     * @throws IllegalAccessException IllegalAccessException
     */
    private <T> void fillField(T result, S7ParseData item) throws IllegalAccessException {
        ByteReadBuff buff = new ByteReadBuff(item.getDataItem().getData());
        item.getField().setAccessible(true);
        switch (item.getDataType()) {
            case BOOL:
                item.getField().set(result, buff.getBoolean(0));
                break;
            case BYTE:
                item.getField().set(result, buff.getBytes(item.getCount()));
                break;
            case UINT16:
                item.getField().set(result, buff.getUInt16());
                break;
            case INT16:
                item.getField().set(result, buff.getInt16());
                break;
            case TIME:
            case UINT32:
                item.getField().set(result, buff.getUInt32());
                break;
            case INT32:
                item.getField().set(result, buff.getInt32());
                break;
            case INT64:
                item.getField().set(result, buff.getInt64());
                break;
            case FLOAT32:
                item.getField().set(result, buff.getFloat32());
                break;
            case FLOAT64:
                item.getField().set(result, buff.getFloat64());
                break;
            case STRING:
                int length = buff.getByteToInt(0);
                item.getField().set(result, buff.getString(1, Math.min(length, item.getCount()), this.charsets));
                break;
            case DATE:
                LocalDate date = this.s7PLC.byteArrayToDate(buff.getBytes(2));
                item.getField().set(result, date);
                break;
            case TIME_OF_DAY:
                LocalTime time = this.s7PLC.byteArrayToTimeOfDay(buff.getBytes(4));
                item.getField().set(result, time);
                break;
            case DTL:
                LocalDateTime dateTime = this.s7PLC.byteArrayToDTL(buff.getBytes(12));
                item.getField().set(result, dateTime);
                break;
            default:
                throw new S7CommException("Unrecognized data type");
        }
    }

    // endregion

    // region write
    @Override
    public <T> void write(T targetBean) {
        // 解析参数
        List<S7ParseData> s7ParseDataList = this.parseBean(targetBean.getClass());

        if (s7ParseDataList.isEmpty()) {
            // 解析出的注解数据个数为空，无法读取数据
            throw new S7CommException("No annotated data fields were found");
        }

        // 填充字节数据
        s7ParseDataList = this.extractData(targetBean, s7ParseDataList);

        // 写入PLC
        List<RequestItem> requestItems = s7ParseDataList.stream().map(S7ParseData::getRequestItem).collect(Collectors.toList());
        List<DataItem> dataItems = s7ParseDataList.stream().map(S7ParseData::getDataItem).collect(Collectors.toList());
        this.s7PLC.writeS7Data(requestItems, dataItems);
    }

    /**
     * Write data to plc by parameter list.
     * (写入数据)
     *
     * @param parameters parameter list
     */
    public void write(List<S7Parameter> parameters) {
        // 解析参数
        List<S7ParseData> s7ParseDataList = this.parseBean(parameters);

        if (s7ParseDataList.size() != parameters.size()) {
            // 解析出的数据个数与传入的数据个数不一致
            throw new S7CommException("Parsed data count does not match the supplied data count");
        }

        // 填充字节数据
        s7ParseDataList = this.extractData(parameters, s7ParseDataList);

        // 写入PLC
        List<RequestItem> requestItems = s7ParseDataList.stream().map(S7ParseData::getRequestItem).collect(Collectors.toList());
        List<DataItem> dataItems = s7ParseDataList.stream().map(S7ParseData::getDataItem).collect(Collectors.toList());
        this.s7PLC.writeS7Data(requestItems, dataItems);
    }

    /**
     * Extract data.
     * (提取数据)
     *
     * @param targetBean      target object
     * @param s7ParseDataList List<S7ParseData>
     * @param <T>             type
     * @return List<S7ParseData>
     */
    private <T> List<S7ParseData> extractData(T targetBean, List<S7ParseData> s7ParseDataList) {
        try {
            for (S7ParseData item : s7ParseDataList) {
                item.getField().setAccessible(true);
                Object data = item.getField().get(targetBean);
                if (data == null) {
                    continue;
                }
                this.extractField(item, data);
            }
            return s7ParseDataList.stream().filter(x -> x.getDataItem() != null).collect(Collectors.toList());
        } catch (Exception e) {
            // 序列化填充字节数据错误
            throw new S7CommException("Failed to fill serialized byte data: " + e.getMessage(), e);
        }
    }

    /**
     * Extract data.
     * (提取数据)
     *
     * @param parameters      parameter list
     * @param s7ParseDataList List<S7ParseData>
     * @return List<S7ParseData>
     */
    private List<S7ParseData> extractData(List<S7Parameter> parameters, List<S7ParseData> s7ParseDataList) {
        try {
            for (int i = 0; i < s7ParseDataList.size(); i++) {
                S7ParseData item = s7ParseDataList.get(i);
                S7Parameter parameter = parameters.get(i);
                if (parameter.getValue() == null) {
                    throw new S7CommException("Parameter value must not be null");
                }
                this.extractField(item, parameter.getValue());
            }
            return s7ParseDataList.stream().filter(x -> x.getDataItem() != null).collect(Collectors.toList());
        } catch (Exception e) {
            // 序列化填充字节数据错误
            throw new S7CommException("Failed to fill serialized byte data: " + e.getMessage(), e);
        }
    }

    /**
     * Extract a field value into S7ParseData.
     * (提取字段数)
     *
     * @param item S7ParseData
     * @param data data source
     */
    private void extractField(S7ParseData item, Object data) {
        switch (item.getDataType()) {
            case BOOL:
                item.setDataItem(DataItem.createReqByBoolean((Boolean) data));
                break;
            case BYTE:
                item.setDataItem(DataItem.createReqByByte(ByteReadBuff.newInstance((byte[]) data)
                        .getBytes(item.getCount())));
                break;
            case UINT16:
                item.setDataItem(DataItem.createReqByByte(ByteWriteBuff.newInstance(2)
                        .putShort((Integer) data).getData()));
                break;
            case INT16:
                item.setDataItem(DataItem.createReqByByte(ByteWriteBuff.newInstance(2)
                        .putShort((Short) data).getData()));
                break;
            case TIME:
            case UINT32:
                item.setDataItem(DataItem.createReqByByte(ByteWriteBuff.newInstance(4)
                        .putInteger((Long) data).getData()));
                break;
            case INT32:
                item.setDataItem(DataItem.createReqByByte(ByteWriteBuff.newInstance(4)
                        .putInteger((Integer) data).getData()));
                break;
            case INT64:
                item.setDataItem(DataItem.createReqByByte(ByteWriteBuff.newInstance(8)
                        .putLong((Long) data).getData()));
                break;
            case FLOAT32:
                item.setDataItem(DataItem.createReqByByte(ByteWriteBuff.newInstance(4)
                        .putFloat((Float) data).getData()));
                break;
            case FLOAT64:
                item.setDataItem(DataItem.createReqByByte(ByteWriteBuff.newInstance(8)
                        .putDouble((Double) data).getData()));
                break;
            case STRING:
                byte[] bytes = ((String) data).getBytes(this.charsets);
                int actualLength = Math.min(bytes.length, item.getCount());
                byte[] targetBytes = new byte[1 + actualLength];
                targetBytes[0] = (byte) actualLength;
                System.arraycopy(bytes, 0, targetBytes, 1, actualLength);
                item.setDataItem(DataItem.createReqByByte(targetBytes));
                // 根据实际情况获取最小字符串长度+1，重新更新待写入的数据长度
                item.getRequestItem().setCount(targetBytes.length);
                break;
            case DATE:
                byte[] dateBytes = this.s7PLC.dateToByteArray((LocalDate) data);
                item.setDataItem(DataItem.createReqByByte(dateBytes));
                break;
            case TIME_OF_DAY:
                byte[] timeBytes = this.s7PLC.timeOfDayToByteArray((LocalTime) data);
                item.setDataItem(DataItem.createReqByByte(timeBytes));
                break;
            case DTL:
                byte[] dateTimeData = this.s7PLC.dtlToByteArray((LocalDateTime) data);
                item.setDataItem(DataItem.createReqByByte(dateTimeData));
                break;
            default:
                // 无法识别数据类型
                throw new S7CommException("Unrecognized data type");
        }
    }
    // endregion
}
