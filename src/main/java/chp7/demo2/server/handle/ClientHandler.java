package chp7.demo2.server.handle;

import chp7.demo2.library.core.Connector;
import chp7.demo2.library.utils.CloseUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lwh
 * @date 2019-02-23
 * @desp
 */
public class ClientHandler {

    private Connector connector;
    private final SocketChannel socketChannel;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socketChannel = socketChannel;

        Connector connector = new Connector(){
            @Override
            public void onChannelClosed(SocketChannel channel) {
                super.onChannelClosed(channel);
                exitBySelf();
            }

            @Override
            protected void onReceiveNewMessage(String str) {
                super.onReceiveNewMessage(str);

                clientHandlerCallback.onNewMessageArrived(ClientHandler.this, str);
            }
        };

        connector.setup(socketChannel);

        //写入
        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        this.writeHandler = new ClientWriteHandler(writeSelector);

        this.clientHandlerCallback = clientHandlerCallback;
        this.clientInfo = "Address[" + socketChannel.getRemoteAddress().toString() + "]";
        System.out.println("新客户端连接: " + clientInfo);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    /**
     * 发送
     * @param str
     */
    public void send(String str) {
        writeHandler.send(str);
    }

    /**
     * 退出
     */
    public void exit() {
        CloseUtils.close(connector);
        writeHandler.exit();
        CloseUtils.close(socketChannel);
        System.out.println("客户端已退出: " + clientInfo);
    }

    private void exitBySelf(){
        exit();
        clientHandlerCallback.onSelfClosed(this);
    }

    public interface ClientHandlerCallback {
        //自身关闭
        void onSelfClosed(ClientHandler handler);

        //收到消息时通知
        void onNewMessageArrived(ClientHandler handler, String msg);
    }


    class ClientWriteHandler{
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final ExecutorService executorService;

        ClientWriteHandler(Selector selector){
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        void send(String str) {
            if(done){
                return;
            }
            executorService.execute(new WriteRunnable(str));
        }

        void exit(){
            done = true;
            CloseUtils.close(selector);
            executorService.shutdownNow();
        }

        class WriteRunnable implements Runnable {
            private final String msg;

            WriteRunnable(String msg){
                this.msg = msg + '\n';
            }

            @Override
            public void run() {

                if(ClientWriteHandler.this.done){
                    return;
                }

                byteBuffer.clear();
                byteBuffer.put(msg.getBytes());

                //将byteBuffer反转一下,重点,指针回到初始位置
                byteBuffer.flip();

                while (!done && byteBuffer.hasRemaining()){
                    try {
                        int len = socketChannel.write(byteBuffer);
                        //len < 0说明出异常了, = 0 是因为这里并没有进行select操作,直接调用线程池发送数据,而buffer中可能并没有数据要发送
                        if(len < 0){
                            System.out.println("客户端已无法发送数据");
                            ClientHandler.this.exitBySelf();
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
