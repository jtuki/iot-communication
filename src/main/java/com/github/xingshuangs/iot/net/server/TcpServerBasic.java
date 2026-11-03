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

package com.github.xingshuangs.iot.net.server;


import com.github.xingshuangs.iot.exceptions.SocketRuntimeException;
import com.github.xingshuangs.iot.net.SocketUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP socket服务端的基础类
 *
 * @author xingshuang
 */
@Data
@Slf4j
public class TcpServerBasic {

    /**
     * Server socket object.
     * (服务器对象)
     */
    private ServerSocket serverSocket;

    /**
     * Port Number.
     * (端口号)
     */
    protected int port = 8088;

    /**
     * Thread pool service.
     * (线程池)
     */
    protected ExecutorService executorService;

    public TcpServerBasic() {
        // NOOP
    }

    //region 服务端

    /**
     * start the server
     * (启动)
     *
     * @throws SocketRuntimeException Socket Runtime Exception
     */
    public void start() {
        this.start(this.port);
    }

    /**
     * start the server
     * (启动)
     *
     * @param port port number
     * @throws SocketRuntimeException Socket Runtime Exception
     */
    public void start(int port) {

        try {
            this.port = port;
            this.stop();
            this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            this.serverSocket = new ServerSocket(port);
            Thread thread = new Thread(this::waitForClients);
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            throw new SocketRuntimeException(e);
        }
    }

    /**
     * Stop the server.
     * (停止)
     *
     * @throws SocketRuntimeException Socket Runtime Exception
     */
    public void stop() {
        try {
            if (this.isAlive()) {
                this.serverSocket.close();
                this.executorService.shutdown();
                // 关闭服务端，端口号[{}]
                log.debug("Server closed, port [{}]", this.port);
            }
        } catch (IOException e) {
            throw new SocketRuntimeException(e);
        }
    }

    /**
     * Whether the server is running.
     * (是否活跃着)
     *
     * @return true if running; false otherwise
     */
    public boolean isAlive() {
        return this.serverSocket != null && !this.serverSocket.isClosed();
    }

    /**
     * Wait for a client to connect.
     * (等待客户端连入)
     */
    protected void waitForClients() {
        // 开启等待客户端线程，端口号[{}]
        log.debug("Starting the accept thread and waiting for clients, port [{}]", this.port);
        while (this.isAlive()) {
            try {
                Socket client = this.serverSocket.accept();
                if (!this.checkClientValid(client)) {
                    SocketUtils.close(client);
                }
                this.executorService.execute(() -> this.doClientConnected(client));
            } catch (IOException e) {
                if (this.isAlive()) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    //endregion

    //region 客户端

    /**
     * Check whether the client connection can be accepted.
     * (校验客户端是否允许连入)
     *
     * @param client client socket object
     * @return true: valid success，false：valid fail
     */
    protected boolean checkClientValid(Socket client) {
        return true;
    }

    /**
     * Handle a newly connected client.
     * (客户端连入后要做的业务)
     *
     * @param client client socket object
     */
    protected void doClientConnected(Socket client) {
        // 有客户端[{}]连入
        log.debug("Client [{}] connected", client.getRemoteSocketAddress());
        this.clientConnected(client);
        try {
            if (this.checkHandshake(client) && this.isAlive()) {
                while (SocketUtils.isConnected(client)) {
                    this.doClientHandle(client);
                }
            }
        } catch (Exception e) {
            if (SocketUtils.isConnected(client)) {
                log.error(e.getMessage());
            }
        } finally {
            try {
                SocketUtils.close(client);
            } catch (Exception ex) {
                // NOOP
            }
        }

        this.clientDisconnected(client);
        // 有客户端[{}]断开
        log.debug("Client [{}] disconnected", client.getRemoteSocketAddress());
    }

    /**
     * Handle a client connection. Subclasses may override this method.
     * (客户端连入)
     *
     * @param socket client socket object
     */
    protected void clientConnected(Socket socket) {
        // NOOP
    }

    /**
     * Handle a client disconnection. Subclasses may override this method.
     * (客户端断开)
     *
     * @param socket client socket object
     */
    protected void clientDisconnected(Socket socket) {
        // NOOP
    }

    /**
     * Perform the handshake and check whether it succeeds.
     * (握手校验)
     *
     * @param socket client socket object
     * @return check result, true: success, false: fail
     */
    protected boolean checkHandshake(Socket socket) {
        return true;
    }

    /**
     * Handle a client message. Subclasses may override this method.
     * (执行客户端的业务，可重写)
     *
     * @param socket client socket object
     * @throws SocketRuntimeException Socket Runtime Exception
     */
    protected void doClientHandle(Socket socket) {
        byte[] data = this.readClientData(socket);
        log.debug(new String(data));
    }

    /**
     * Read client data. Subclasses may override this method.
     * (读取客户端数据)
     *
     * @param socket client socket object
     * @return the bytes array read into
     * @throws SocketRuntimeException Socket Runtime Exception
     */
    protected byte[] readClientData(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            int firstByte = in.read();
            if (firstByte == -1) {
                SocketUtils.close(socket);
                throw new SocketRuntimeException("Client closed the connection");
            }
            byte[] data = new byte[in.available() + 1];
            data[0] = (byte) firstByte;
            this.read(socket, data, 1, data.length - 1, 1024);
            return data;
        } catch (IOException e) {
            throw new SocketRuntimeException(e);
        }
    }

    /**
     * Write data from a byte array.
     * （写入数据）
     *
     * @param socket socket object
     * @param data   byte array
     * @throws SocketRuntimeException Socket Runtime Exception
     */
    protected void write(final Socket socket, final byte[] data) {
        try {
            SocketUtils.write(socket, data);
        } catch (IOException e) {
            throw new SocketRuntimeException(e);
        }
    }

    /**
     * Read data into the specified byte array.
     * （读取数据）
     *
     * @param socket socket object
     * @param data   byte array
     * @return the total number of bytes read into the data
     * @throws SocketRuntimeException Socket Runtime Exception
     */
    protected int read(final Socket socket, final byte[] data) {
        return this.read(socket, data, 0, data.length, 1024);
    }

    /**
     * Read data into the specified byte array.
     * （读取数据）
     *
     * @param socket socket object
     * @param data   byte array
     * @param offset the start offset in the data.
     * @param length the number of bytes to read.
     * @return the total number of bytes read into the data
     * @throws SocketRuntimeException Socket Runtime Exception
     */
    protected int read(final Socket socket, final byte[] data, final int offset, final int length) {
        return this.read(socket, data, offset, length, 1024);
    }

    /**
     * Read data into the specified byte array.
     * （读取数据）
     *
     * @param socket    socket object
     * @param data      byte array
     * @param offset    the start offset in the data.
     * @param length    the number of bytes to read.
     * @param maxLength maximum number of bytes per read or write; ignored if less than or equal to 0. (单次通信允许的对最大长度，若小于等于0则不考虑)
     * @return the total number of bytes read into the data
     * @throws SocketRuntimeException Socket Runtime Exception
     */
    protected int read(final Socket socket, final byte[] data, final int offset, final int length, final int maxLength) {
        try {
            return SocketUtils.read(socket, data, offset, length, maxLength);
        } catch (IOException e) {
            throw new SocketRuntimeException(e);
        }
    }

    /**
     * Read data into the specified byte array.
     * （读取数据）
     *
     * @param socket      socket object
     * @param data        byte array
     * @param offset      the start offset in the data.
     * @param length      the number of bytes to read.
     * @param maxLength   maximum number of bytes per read or write; ignored if less than or equal to 0. (单次通信允许的对最大长度，若小于等于0则不考虑)
     * @param timeout     timeout in milliseconds; 0 means no timeout
     * @param waitForMore whether to keep reading until the requested length is reached; useful for messages split across TCP reads(若数据不够，是否等待，等待更多数据，大部分都是不等待的，等待都适用于分包粘包的情况)
     * @return the total number of bytes read into the data
     * @throws SocketRuntimeException Socket Runtime Exception
     */
    protected int read(final Socket socket, final byte[] data, final int offset, final int length,
                       final int maxLength, final int timeout, final boolean waitForMore) {
        try {
            return SocketUtils.read(socket, data, offset, length, maxLength, timeout, waitForMore);
        } catch (IOException e) {
            throw new SocketRuntimeException(e);
        }
    }

    //endregion
}
