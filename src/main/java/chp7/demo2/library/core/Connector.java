package chp7.demo2.library.core;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * 代表一个连接
 */
public class Connector {

    /**
     * key表示当前连接的唯一性
     */
    private UUID key = UUID.randomUUID();

    private SocketChannel channel;

    private Sender sender;

    private Receiver receiver;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;
    }


}
