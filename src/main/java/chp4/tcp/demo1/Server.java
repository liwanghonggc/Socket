package chp4.tcp.demo1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;

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

    private static final int PORT = 20000;

    public static void main(String[] args) throws IOException {
        //没有传IP地址参数,默认是本机IP地址
        //若本机电脑有多个IP地址,默认会在所有IP地址上进行监听
        ServerSocket server = createSocket();

        initSocket(server);

        //绑定到本地端口上,backlog设置的不是可以连接的最大socket数量
        //它是设置的允许的最大可以等待的连接队列,当你bind之后并没有accept去获取连接时,当队列中等待连接数超过50时将会抛出异常,该异常是在客户端触发
        server.bind(new InetSocketAddress(Inet4Address.getLocalHost(), PORT), 50);

        System.out.println("服务器准备就绪");
        System.out.println("服务器信息: " + server.getInetAddress() + ", " + server.getLocalPort());


        //等待客户端连接
        for(;;){
            //得到客户端
            Socket client = server.accept();
            //客户端构建异步线程
            ClientHandler clientHandler = new ClientHandler(client);
            clientHandler.start();
        }
    }

    /**
     * 创建ServerSocket的一些操作
     * @return
     */
    private static ServerSocket createSocket() throws IOException{

        ServerSocket server = new ServerSocket();

        /*
        //绑定到本地端口上,backlog设置的不是可以连接的最大socket数量
        //它是设置的允许的最大可以等待的连接队列,当你bind之后并没有accept去获取连接时,当队列中等待连接数超过50时将会抛出异常,该异常是在客户端触发
        server.bind(new InetSocketAddress(Inet4Address.getLocalHost(), PORT), 50);

        /*
        //绑定到本地端口上20000上,并且设置当前可以允许等待的连接的队列为50个
        server = new ServerSocket(PORT);

        //等效于上面的方案,队列设置为50个
        server = new ServerSocket(PORT, 50);

        //等效
        server = new ServerSocket(PORT, 50, Inet4Address.getLocalHost());
        */

        return server;
    }

    /**
     * 初始化Socket的一些操作
     * @param server
     */
    private static void initSocket(ServerSocket server) throws SocketException {

        //是否复用未完全关闭的Socket地址,对于指定bind操作后的套接字有效
        //复用本地地址和端口,但是它在关闭后2min之内默认是不允许复用的
        server.setReuseAddress(true);

        //设置的是Socket的接收缓冲区大小,不是ServerSocket的,它是server.accept()得到的客户端的Socket的缓冲区,该值需要在accept操作之前进行设置
        server.setReceiveBufferSize(64 * 1024 * 1024);

        //设置ServerSocket的accept超时时间,超过2秒没有得到客户端连接将会触发异常,当然触发异常后server还是可以用的,你可以在上述for循环中捕获异常后继续用accept方法接收连接
        //通常不建议这么做,而是不设置该参数,永久等待
        //server.setSoTimeout(2000);

        //设置性能参数:短连接、延迟、带宽的相对重要性,这里参数就是三个参数的权重比
        //如果认为你建立的socket连接很短时间后就会关闭,认为短连接是重要的,则可以设置2 1 1等
        //如果认为延迟比较关键,数据要在短时间内就发送出去,则可以提高延迟的权重
        //如果延迟较大,即可以理解为可以多包一起发送,则认为带宽利用率增加了,如传输文件时通常时间较长,对带宽利用率要求较高,则可以设置2 1 2
        server.setPerformancePreferences(1, 1, 1);
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
                        socketOutput.println("回送数据长度: " + str.length());
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
