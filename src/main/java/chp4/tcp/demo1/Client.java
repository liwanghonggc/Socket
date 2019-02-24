package chp4.tcp.demo1;

import java.io.*;
import java.net.*;

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
    private static Socket createSocket() throws IOException{

        /*
        //无代理模式,等效于空构造函数
        Socket socket = new Socket(Proxy.NO_PROXY);

        //新建一份具有HTTP代理的套接字,传输数据将通过www.baidu.com:8800端口转发
        //这里仅是以百度为例,实际中通常需要自己建立好这样一个代理服务器,在代理服务器上做好代理配置,这也是翻墙的原理
        //如通过香港的一个可以访问外网的代理服务器为中介
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Inet4Address.getByName("www.baidu.com"), 8800));
        socket = new Socket(proxy);

        //新建一个套接字,并且直接连接到本地20000的服务器上
        socket = new Socket("localhost", PORT);

        //新建一个套接字,并且直接连接到本地20000的服务器上
        socket = new Socket(Inet4Address.getLocalHost(), PORT);

        //新建一个套接字,并且直接连接到本地20000的服务器上,且绑定到本地20001的端口上
        //这种方式是既绑定了本地地址端口,又建立了远程连接,相当于也调用了connect
        //不推荐这种方式,因为可能需要在建立连接之前做一些设置
        socket = new Socket("localhost", PORT, Inet4Address.getLocalHost(), LOCAL_PORT);
        socket = new Socket(Inet4Address.getLocalHost(), PORT, Inet4Address.getLocalHost(), LOCAL_PORT);
        */

        Socket socket = new Socket();
        //绑定到本地20001端口
        socket.bind(new InetSocketAddress(Inet4Address.getLocalHost(), LOCAL_PORT));

        return socket;
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
