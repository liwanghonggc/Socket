package chp4.tcp.demo1;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

/**
 * @author lwh
 * @date 2019-02-20
 * @desp
 */
public class Client {

    private static final int PORT = 20000;

    private static final int LOCAL_PORT = 20001;

    public static void main(String[] args) throws IOException {
        Socket socket = createSocket();

        initSocket(socket);

        //连接到本地20000端口,超时时间为3秒,超过则抛出异常
        socket.connect(new InetSocketAddress(Inet4Address.getLocalHost(), PORT), 3000);

        System.out.println("已发起服务器连接,并进入后续流程!");
        System.out.println("客户端信息: " + socket.getLocalAddress() + ", " + socket.getLocalPort());
        System.out.println("服务端信息: " + socket.getInetAddress() + ", " + socket.getPort());

        try {
            //发送接收数据,是阻塞的操作
            sendData(socket);
        } catch (IOException e) {
            System.out.println("异常关闭");
        }

        //释放资源
        socket.close();
        System.out.println("客户端已退出");
    }

    /**
     * 创建Socket的一些操作
     * @return
     */
    private static Socket createSocket(){

        //无代理模式,等效于空构造函数
        Socket socket = new Socket(Proxy.NO_PROXY);




        return null;
    }

    /**
     * 初始化Socket的一些操作
     * @param socket
     */
    private static void initSocket(Socket socket){

    }

    private static void sendData(Socket client) throws IOException{
        InputStream in = System.in;
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader input = new BufferedReader(isr);

        //得到Socket输出流,并转换为打印流
        OutputStream outputStream = client.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(outputStream);

        //得到Socket输入流,并转换为BufferedReader
        InputStream inputStream = client.getInputStream();
        BufferedReader socketBufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        boolean flag = true;
        do{
            //键盘读取一行
            String str = input.readLine();
            //发送到服务器
            socketPrintStream.println(str);

            //从服务器接收一行数据
            String echo = socketBufferedReader.readLine();
            if("bye".equals(echo)){
                flag = false;
            }else {
                System.out.println(echo);
            }
        }while (flag);

        //资源释放
        socketPrintStream.close();
        socketBufferedReader.close();
    }
}
