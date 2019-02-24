package chp7.demo2.server;

import chp7.demo2.server.handle.ClientHandler;
import chp7.demo2.library.utils.CloseUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lwh
 * @date 2019-02-21
 * @desp 监听连接,交给ClientHandler来处理
 */
public class TCPServer implements ClientHandler.ClientHandlerCallback{

    private final int port;
    private ClientListener listener;
    //要发送到所有客户端,这里要维护一个客户端列表
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardThreadPoolExecutor;
    private Selector selector;
    private ServerSocketChannel server;

    public TCPServer(int port){
        this.port = port;
        //转发消息线程池
        this.forwardThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * 此处做了修改
     * @return
     */
    public boolean start(){
        try {
            selector = Selector.open();
            server = ServerSocketChannel.open();
            //设置为非阻塞
            server.configureBlocking(false);
            //绑定本地端口
            server.socket().bind(new InetSocketAddress(port));

            System.out.println("服务器信息: " + server.getLocalAddress().toString());

            //注册接收连接事件
            server.register(selector, SelectionKey.OP_ACCEPT);

            //启动客户端监听
            ClientListener listener = new ClientListener();
            this.listener = listener;
            listener.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop(){
        if(listener != null){
            listener.exit();
        }

        CloseUtils.close(server);
        CloseUtils.close(selector);

        //把客户端退出并清空列表
        synchronized (TCPServer.this){
            for(ClientHandler clientHandler : clientHandlerList){
                clientHandler.exit();
            }

            clientHandlerList.clear();
        }

        //退出时停止线程池
        forwardThreadPoolExecutor.shutdownNow();
    }

    //接收到消息后发送到所有客户端
    public synchronized void broadCast(String str) {
        for(ClientHandler clientHandler : clientHandlerList){
            clientHandler.send(str);
        }
    }


    private class ClientListener extends Thread{
        private boolean done = false;

        @Override
        public void run() {
            super.run();

            //监听当前有没有客户端连接到达
            Selector selector = TCPServer.this.selector;

            System.out.println("服务器准备就绪");

            //等待客户端连接
            do{
                try{
                    //select是一个阻塞操作,有可能被唤醒,唤醒时返回0
                    if(selector.select() == 0){
                        if(done){
                            break;
                        }
                        continue;
                    }

                    //当前有哪些事件就绪了
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()){
                        if(done){
                            break;
                        }

                        SelectionKey key = iterator.next();
                        iterator.remove();

                        //检查当前key的状态是否是我们关注的客户端到达状态
                        if(key.isAcceptable()){
                            //key.channel()拿到的是我们注册时的channel,也就是ServerSocketChannel
                            ServerSocketChannel server = (ServerSocketChannel)key.channel();
                            //本来accept操作是阻塞操作,但是这里已经select到了,说明肯定来了新连接接入
                            SocketChannel client = server.accept();

                            try {
                                //客户端构建异步线程
                                ClientHandler clientHandler = new ClientHandler(client, TCPServer.this);

                                //读取数据并打印,收发分离
                                clientHandler.readToPrint();
                                synchronized (TCPServer.this){
                                    clientHandlerList.add(clientHandler);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("客户端连接异常: " + e.getMessage());
                            }
                        }
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }while (!done);

            System.out.println("服务器已关闭");
        }

        void exit(){
            done = true;
            //唤醒当前阻塞的select操作
            selector.wakeup();
        }
    }

    /**
     * 实现两个回调方法
     * @param handler
     */
    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public void onNewMessageArrived(final ClientHandler handler, final String msg) {
        //打印到屏幕
        System.out.println("Received-" + handler.getClientInfo() + ": " + msg);
        //异步提交转发任务
        forwardThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (TCPServer.this){
                    for(ClientHandler clientHandler : clientHandlerList){
                        if(clientHandler.equals(handler)){
                            //如果是自己,跳过
                            continue;
                        }
                        clientHandler.send(msg);
                    }
                }
            }
        });
    }

}
