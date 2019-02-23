package chp6.demo1.server;

import chp6.demo1.server.handle.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lwh
 * @date 2019-02-21
 * @desp
 */
public class TCPServer implements ClientHandler.ClientHandlerCallback{

    private final int port;
    private ClientListener mListener;
    //要发送到所有客户端,这里要维护一个客户端列表
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardThreadPoolExecutor;

    public TCPServer(int port){
        this.port = port;
        //转发消息线程池
        this.forwardThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start(){
        try {
            ClientListener listener = new ClientListener(port);
            mListener = listener;
            listener.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop(){
        if(mListener != null){
            mListener.exit();
        }

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
        private ServerSocket server;
        private boolean done = false;


        private ClientListener(int port) throws IOException {
            server = new ServerSocket(port);
            System.out.println("服务器信息: " + server.getInetAddress() + " port: " + server.getLocalPort());
        }

        @Override
        public void run() {
            super.run();

            System.out.println("服务器准备就绪");

            //等待客户端连接
            do{
                //等待客户端
                Socket client;

                try{
                    client = server.accept();
                }catch (IOException e){
                    continue;
                }

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

            }while (!done);

            System.out.println("服务器已关闭");
        }

        void exit(){
            done = true;
            try{
                server.close();
            }catch (IOException e){
                e.printStackTrace();
            }
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
