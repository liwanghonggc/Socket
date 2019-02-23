package chp5.demo1.client;

import chp5.demo1.client.bean.ServerInfo;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author lwh
 * @date 2019-02-21
 * @desp
 */
public class TCPClient {

    public static void linkWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();

        //超时时间
        socket.setSoTimeout(3000);

        //连接
        socket.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()), 3000);

        System.out.println("已发起服务器连接,并进入后续流程");
        System.out.println("客户端信息: " + socket.getLocalAddress() + ", " + socket.getLocalPort());
        System.out.println("服务端信息: " + socket.getInetAddress() + ", " + socket.getPort());

        try {
            //发送接收数据,是阻塞的操作
            processData(socket);
        } catch (IOException e) {
            System.out.println("异常关闭");
        }

        //释放资源
        socket.close();
        System.out.println("客户端已退出");
    }

    private static void processData(Socket client) throws IOException{
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        //得到Socket输出流,并转换为打印流
        OutputStream outputStream = client.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(outputStream);

        //得到Socket输入流,并转换为BufferedReader
        InputStream inputStream = client.getInputStream();
        BufferedReader socketBufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        boolean flag = false;
        do{
            //键盘读取一行
            String str = input.readLine();
            //发送到服务器
            socketPrintStream.println(str);

            //从服务器接收一行数据
            String echo = socketBufferedReader.readLine();
            if("bye".equalsIgnoreCase(echo)){
                flag = true;
            }else {
                System.out.println(echo);
            }
        }while (!flag);

        //资源释放
        socketPrintStream.close();
        socketBufferedReader.close();
    }
}
