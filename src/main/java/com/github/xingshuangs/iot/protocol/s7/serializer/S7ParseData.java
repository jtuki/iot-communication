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


import com.github.xingshuangs.iot.common.enums.EDataType;
import com.github.xingshuangs.iot.protocol.s7.model.DataItem;
import com.github.xingshuangs.iot.protocol.s7.model.RequestItem;
import lombok.Data;

import java.lang.reflect.Field;

/**
 * S7 parse data
 * S7解析数据
 *
 * @author xingshuang
 */
@Data
public class S7ParseData {

    /**
     * Data type.
     * (数据类型)
     */
    private EDataType dataType;

    /**
     * Data count.
     * (数据个数)
     */
    private int count;

    /**
     * Field.
     * (字段参数)
     */
    private Field field;

    /**
     * Request item.
     * (请求项)
     */
    private RequestItem requestItem;

    /**
     * Data item.
     * (数据项)
     */
    private DataItem dataItem;
}
