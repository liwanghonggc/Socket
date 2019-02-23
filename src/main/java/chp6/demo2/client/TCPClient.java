package chp6.demo2.client;

import chp6.demo2.client.bean.ServerInfo;
import chp6.demo2.utils.CloseUtils;

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

    private final Socket socket;
    private final ReadHandler readHandler;
    private final PrintStream printStream;

    public TCPClient(Socket socket, ReadHandler readHandler) throws IOException {
        this.socket = socket;
        this.readHandler = readHandler;
        this.printStream = new PrintStream(socket.getOutputStream());
    }

    public void exit(){
        readHandler.exit();
        CloseUtils.close(printStream);
        CloseUtils.close(socket);
    }

    public void send(String msg){
        printStream.println(msg);
    }

    public static TCPClient startWith(ServerInfo info) throws IOException {
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
            return new TCPClient(socket, readHandler);
        } catch (IOException e) {
            System.out.println("连接异常");
            CloseUtils.close(socket);
        }

        return null;
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
