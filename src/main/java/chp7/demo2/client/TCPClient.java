package chp7.demo2.client;

import chp7.demo2.client.bean.ServerInfo;
import chp7.demo2.library.utils.CloseUtils;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

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
            ReadHandler readHandler = new ReadHandler(socket.getInputStream());
            readHandler.start();
            //发送数据
            write(socket);
            //退出操作
            readHandler.exit();
        } catch (IOException e) {
            System.out.println("异常关闭");
        }

        //释放资源
        socket.close();
        System.out.println("客户端已退出");
    }

    private static void write(Socket client) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        //得到Socket输出流,并转换为打印流
        OutputStream outputStream = client.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(outputStream);

        do {
            //键盘读取一行
            String str = input.readLine();
            //发送到服务器
            socketPrintStream.println(str);

            if ("00bye00".equalsIgnoreCase(str)){
                break;
            }

        } while (true);

        //资源释放
        socketPrintStream.close();
    }

    /**
     * 读取数据Handler
     */
    static class ReadHandler extends Thread {

        private boolean done = false;
        private final InputStream inputStream;

        ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();

            try {
                //得到输入流,用于接收数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                do {
                    String str = null;
                    try {
                        //拿到一条数据
                        str = socketInput.readLine();
                    } catch (SocketTimeoutException e) {
                        continue;
                    }

                    if(str == null){
                        System.out.println("连接已关闭,无法读取数据");
                        break;
                    }

                    //打印到屏幕
                    System.out.println(str);

                }while (!done);
            } catch (IOException e) {
                if(!done){
                    System.out.println("连接异常断开: " + e.getMessage());
                }

            } finally {
                CloseUtils.close(inputStream);
            }
        }

        void exit(){
            done = true;
            CloseUtils.close(inputStream);
        }
    }
}
