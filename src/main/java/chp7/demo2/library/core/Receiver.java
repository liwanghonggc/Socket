package chp7.demo2.library.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {

    boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException;
}
