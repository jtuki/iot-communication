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

package com.github.xingshuangs.iot.protocol.modbus.enums;


import java.util.HashMap;
import java.util.Map;

/**
 * Modbus function codes, including exception response codes.
 * (功能码枚举)
 * <p>
 * Constant names retain legacy terminology; comments use Modbus function names.
 *
 * @author xingshuang
 */
public enum EMbFunctionCode {

    /**
     * Read Coils.
     * 读线圈
     */
    READ_COIL((byte) 0x01, "read coil"),

    /**
     * Read Discrete Inputs.
     * 读离散量输入
     */
    READ_DISCRETE_INPUT((byte) 0x02, "read discrete input"),

    /**
     * Read Holding Registers (HOLD in the constant name means "holding").
     * 读保持寄存器
     */
    READ_HOLD_REGISTER((byte) 0x03, "read hold register"),

    /**
     * Read Input Registers.
     * 读输入寄存器
     */
    READ_INPUT_REGISTER((byte) 0x04, "read input register"),

    /**
     * Write Single Coil.
     * 写单个线圈
     */
    WRITE_SINGLE_COIL((byte) 0x05, "write single coil"),

    /**
     * Write Single Register.
     * 写单个寄存器
     */
    WRITE_SINGLE_REGISTER((byte) 0x06, "write single register"),

    /**
     * Write Multiple Coils.
     * 写多个线圈
     */
    WRITE_MULTIPLE_COIL((byte) 0x0F, "write multiple coil"),

    /**
     * Write Multiple Registers.
     * 写多个寄存器
     */
    WRITE_MULTIPLE_REGISTER((byte) 0x10, "write multiple register"),

    /**
     * Read File Record (DOCUMENT_RECORD is the legacy name for a file record).
     * 读文件记录
     */
    READ_DOCUMENT_RECORD((byte) 0x14, "read document record"),

    /**
     * Mask Write Register (SHIELD is the legacy name for the mask operation).
     * 屏蔽写寄存器
     */
    SHIELD_WRITE_REGISTER((byte) 0x16, "shield write register"),

    /**
     * Read/Write Multiple Registers.
     * 读/写多个寄存器
     */
    READ_WRITE_MULTIPLE_REGISTER((byte) 0x17, "read write multiple register"),

    /**
     * Read device identification code.
     * 读设备识别码
     */
    READ_DEVICE_IDENTIFICATION_CODE((byte) 0x2B, "read device identification code"),

    /**
     * Exception response for {@link #READ_COIL}.
     * 读线圈错误
     */
    ERROR_READ_COIL((byte) 0x81, "error read coil"),

    /**
     * Exception response for {@link #READ_DISCRETE_INPUT}.
     * 读离散量输入错误
     */
    ERROR_READ_DISCRETE_INPUT((byte) 0x82, "error read discrete input"),

    /**
     * Exception response for {@link #READ_HOLD_REGISTER}.
     * 读保持寄存器错误
     */
    ERROR_READ_HOLD_REGISTER((byte) 0x83, "error read hold register"),

    /**
     * Exception response for {@link #READ_INPUT_REGISTER}.
     * 读输入寄存器错误
     */
    ERROR_READ_INPUT_REGISTER((byte) 0x84, "error read input register"),

    /**
     * Exception response for {@link #WRITE_SINGLE_COIL}.
     * 写单个线圈错误
     */
    ERROR_WRITE_SINGLE_COIL((byte) 0x85, "error write single coil"),

    /**
     * Exception response for {@link #WRITE_SINGLE_REGISTER}.
     * 写单个寄存器错误
     */
    ERROR_WRITE_SINGLE_REGISTER((byte) 0x86, "error write single register"),

    /**
     * Exception response for {@link #WRITE_MULTIPLE_COIL}.
     * 写多个线圈错误
     */
    ERROR_WRITE_MULTIPLE_COIL((byte) 0x8F, "error write multiple coil"),

    /**
     * Exception response for {@link #WRITE_MULTIPLE_REGISTER}.
     * 写多个寄存器错误
     */
    ERROR_WRITE_MULTIPLE_REGISTER((byte) 0x90, "error write multiple register"),

    /**
     * Exception response for {@link #READ_DOCUMENT_RECORD}.
     * 读文件记录错误
     */
    ERROR_READ_DOCUMENT_RECORD((byte) 0x94, "error read document record"),

    /**
     * Exception response for {@link #SHIELD_WRITE_REGISTER}.
     * 屏蔽写寄存器错误
     */
    ERROR_SHIELD_WRITE_REGISTER((byte) 0x96, "error shield write register"),

    /**
     * Exception response for {@link #READ_WRITE_MULTIPLE_REGISTER}.
     * 读/写多个寄存器错误
     */
    ERROR_READ_WRITE_MULTIPLE_REGISTER((byte) 0x97, "error read write multiple register"),
    ;

    // 静态内部类（static 内部类）实现懒加载
    private static class Holder {
        private static final Map<Byte, EMbFunctionCode> INSTANCE = createMap();

        private static Map<Byte, EMbFunctionCode> createMap() {
            Map<Byte, EMbFunctionCode> map = new HashMap<>();
            for (EMbFunctionCode item : EMbFunctionCode.values()) {
                map.put(item.code, item);
            }
            return map;
        }
    }

    public static EMbFunctionCode from(byte data) {
        return Holder.INSTANCE.get(data);
    }

    private final byte code;

    private final String description;

    EMbFunctionCode(byte code, String description) {
        this.code = code;
        this.description = description;
    }

    public byte getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
