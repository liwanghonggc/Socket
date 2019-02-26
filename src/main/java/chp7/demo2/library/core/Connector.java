package chp7.demo2.library.core;

import chp7.demo2.library.impl.SocketChannelAdapter;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * 代表一个连接
 */
public class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {

    /**
     * key表示当前连接的唯一性
     */
    private UUID key = UUID.randomUUID();

    private SocketChannel channel;

    private Sender sender;

    private Receiver receiver;

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;

        IoContext context = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);

        this.sender = adapter;
        this.receiver = adapter;

        readNextMessage();
    }

    private void readNextMessage(){
        if(receiver != null){
            try {
                receiver.receiveAsync(echoReceiveListener);
            } catch (IOException e) {
                System.out.println("开始接收数据异常: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void onChannelClosed(SocketChannel channel) {

    }

    private IoArgs.IoArgsEventListener echoReceiveListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

        }

        @Override
        public void onCompleted(IoArgs args) {
            //打印
            onReceiveNewMessage(args.bufferString());
            //读取下一条数据
            readNextMessage();
        }
    };

    protected void onReceiveNewMessage(String str){
        System.out.println(key.toString() + ": " + str);
    }
}
