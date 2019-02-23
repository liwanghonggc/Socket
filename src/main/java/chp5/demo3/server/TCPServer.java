package chp5.demo3.server;

import chp5.demo3.server.handle.ClientHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lwh
 * @date 2019-02-21
 * @desp
 */
public class TCPServer {

    private final int port;
    private ClientListener mListener;
    //要发送到所有客户端,这里要维护一个客户端列表
    private List<ClientHandler> clientHandlerList = new ArrayList<>();

    public TCPServer(int port){
        this.port = port;
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
        for(ClientHandler clientHandler : clientHandlerList){
            clientHandler.exit();
        }

        clientHandlerList.clear();
    }

    //接收到消息后发送到所有客户端
    public void broadCast(String str) {
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
                    ClientHandler clientHandler = new ClientHandler(client, handler -> clientHandlerList.remove(handler));

                    //读取数据并打印,收发分离
                    clientHandler.readToPrint();
                    clientHandlerList.add(clientHandler);
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

}
