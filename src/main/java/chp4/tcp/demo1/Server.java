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
     * @param socket
     */
    private static void initSocket(Socket socket) throws SocketException {
        //设置读取超时时间为2秒
        //setSoTimeout是设置后面有阻塞操作的超时等待时间,在socket当中有哪些操作阻塞呢?1.连接 2.读取数据
        //连接呢专门有连接超时进行设置,所以这里可以理解为就是设置读取数据超时时间
        socket.setSoTimeout(2000);

        //是否复用未完全关闭的Socket地址,对于指定bind操作后的套接字有效
        //复用本地地址和端口,但是它在关闭后2min之内默认是不允许复用的
        socket.setReuseAddress(true);

        //是否开启Nagle算法,默认开启,尽可能发送大块数据,避免网络中充斥着许多小数据块
        socket.setTcpNoDelay(false);

        //是否需要在长时间无数据响应时发送确认数据(心跳包),时间大约2小时
        socket.setKeepAlive(true);

        //对于close关闭操作行为进行怎样的处理,默认为false 0,close时缓冲区中可能还有数据未发送
        //false 0:默认情况,关闭时立即返回,底层系统接管输出流,将缓冲区内的数据发送完成,这样close操作立即完成,不会阻塞
        //true  0:关闭时立即返回,缓冲区数据抛弃,直接发送RST结束命令到对方,并无需经过2MSL等待
        //true 20:关闭时最长阻塞20ms,这期间数据可以发送,发送不完按第二种情况处理,这样close操作会阻塞
        socket.setSoLinger(true, 20);

        //是否让紧急数据内敛,默认false,紧急数据通过socket.setUrgentData(1)发送
        //设置true时就是将紧急数据和行为数据合并到一起,通常不建议使用,可能导致行为数据脏乱
        socket.setOOBInline(false);

        //设置接收发送缓冲区大小
        //发送缓冲区为64K时,来了32K数据可以立即被发送出去;发送65K数据时可能会进行拆分,分成64K和1K数据发送出去
        socket.setReceiveBufferSize(64 * 1024 * 1024);
        socket.setSendBufferSize(64 * 1024 * 1024);

        //设置性能参数:短连接、延迟、带宽的相对重要性,这里参数就是三个参数的权重比
        //如果认为你建立的socket连接很短时间后就会关闭,认为短连接是重要的,则可以设置2 1 1等
        //如果认为延迟比较关键,数据要在短时间内就发送出去,则可以提高延迟的权重
        //如果延迟较大,即可以理解为可以多包一起发送,则认为带宽利用率增加了,如传输文件时通常时间较长,对带宽利用率要求较高,则可以设置2 1 2
        socket.setPerformancePreferences(1, 1, 1);
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
