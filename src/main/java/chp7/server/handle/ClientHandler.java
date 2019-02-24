package chp7.server.handle;

import chp7.utils.CloseUtils;
import sun.util.resources.cldr.ebu.CurrencyNames_ebu;

import javax.sound.midi.SoundbankResource;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
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

    private final SocketChannel socketChannel;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socketChannel = socketChannel;

        //设置非阻塞模式
        socketChannel.configureBlocking(false);

        //读取
        Selector readSelector = Selector.open();
        socketChannel.register(readSelector, SelectionKey.OP_READ);
        this.readHandler = new ClientReadHandler(readSelector);

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
        readHandler.exit();
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

    /**
     * 读取数据并打印,收发分离
     */
    public void readToPrint() {
        readHandler.start();
    }

    /**
     * 读取数据Handler
     */
    class ClientReadHandler extends Thread {

        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;

        ClientReadHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
        }

        @Override
        public void run() {
            super.run();

            try {
                do {
                    if(selector.select() == 0){
                        if(done){
                            break;
                        }
                        continue;
                    }

                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()){
                        if(done){
                            break;
                        }

                        SelectionKey key = iterator.next();
                        iterator.remove();

                        //如果当前可读
                        if(key.isReadable()){
                            //key.channel()拿到的是注册时的channel,这里是SocketChannel
                            SocketChannel client = (SocketChannel) key.channel();
                            byteBuffer.clear();
                            //读到ByteBuffer中
                            int readLen = client.read(byteBuffer);

                            if(readLen > 0){
                                //-1丢弃换行符
                                String str = new String(byteBuffer.array(), 0, byteBuffer.position() - 1);
                                //当收到消息时需要将自身以及消息通知回去给TcpServer
                                clientHandlerCallback.onNewMessageArrived(ClientHandler.this, str);
                            }else {
                                System.out.println("客户端已无法读取数据!");
                                //退出当前客户端
                                ClientHandler.this.exitBySelf();
                                break;
                            }
                        }
                    }
                }while (!done);
            } catch (IOException e) {
                if(!done){
                    System.out.println("连接异常断开");
                    //退出当前客户端
                    ClientHandler.this.exitBySelf();
                }

            } finally {
                CloseUtils.close(selector);
            }
        }

        void exit(){
            done = true;
            //退出时selector可能处于阻塞状态,唤醒一下
            selector.wakeup();
            CloseUtils.close(selector);
        }
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
                this.msg = msg;
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
