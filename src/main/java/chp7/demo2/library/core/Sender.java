package chp7.demo2.library.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {

    /**
     * 异步发送
     * @param args 发送的数据
     * @param listener 发送的状态通过该listener回调
     */
    boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException;
}
