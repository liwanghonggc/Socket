package chp2.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author lwh
 * @date 2019-02-20
 * @desp
 */

/**
 * 完成的功能
 * 1. 构建TCP客户端、服务端
 * 2. 客户端发送数据
 * 3. 服务器读取数据并打印
 */
public class Server {

    public static void main(String[] args) throws IOException {
        //没有传IP地址参数,默认是本机IP地址
        //若本机电脑有多个IP地址,默认会在所有IP地址上进行监听
        ServerSocket server = new ServerSocket(2000);

        System.out.println("服务器准备就绪");
        System.out.println("服务器信息: " + server.getInetAddress() + ", " + server.getLocalPort());


        //等待客户端连接
        for(;;){
            //得到客户端
            Socket client = server.accept();
            //客户端构建异步线程,不要阻塞这里
            ClientHandler clientHandler = new ClientHandler(client);
            clientHandler.start();
        }
    }

    /**
     * 客户端消息处理
     */
    private static class ClientHandler extends Thread{
        private Socket socket;
        private boolean flag = true;

        ClientHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            super.run();

            System.out.println("客户端信息: " + socket.getInetAddress() + ", " + socket.getPort());
            try{
                //得到打印流,用于服务器回送数据
                PrintStream socketOutput = new PrintStream(socket.getOutputStream());
                //得到输入流,用于接收数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                do{
                    //客户端拿到一条信息
                    String str = socketInput.readLine();
                    if("bye".equalsIgnoreCase(str)){
                        flag = false;
                        //回送
                        socketOutput.println("bye");
                    }else{
                        //打印到屏幕,并回送数据长度
                        System.out.println(str);
                        socketOutput.println("回送: " + str.length());
                    }
                }while (flag);

                socketInput.close();
                socketOutput.close();

            }catch (Exception e){
                System.out.println("连接异常断开");
            }finally {
                try {
                    //连接关闭
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("客户端已关闭,客户端信息: " + socket.getInetAddress() + ", " + socket.getPort());
        }
    }
}
