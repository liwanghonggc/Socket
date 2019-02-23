package chp6.demo2.client;

import chp6.demo2.client.bean.ServerInfo;

import java.io.*;
import java.net.Socket;

/**
 * @author lwh
 * @date 2019-02-21
 * @desp
 */
public class Client {

    public static void main(String[] args) {
        //UDP广播搜索并监听,得到服务器的IP地址与端口信息
        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server: " + info);

        if(info != null){
            TCPClient tcpClient = null;
            try {
                //搜索到信息进行TCP连接
                tcpClient = TCPClient.startWith(info);
                if(tcpClient == null){
                    return;
                }

                write(tcpClient);
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if(tcpClient != null){
                    tcpClient.exit();
                }
            }
        }
    }

    private static void write(TCPClient tcpClient) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        do {
            //键盘读取一行
            String str = input.readLine();
            //发送到服务器
            tcpClient.send(str);

            if ("00bye00".equalsIgnoreCase(str)){
                break;
            }

        } while (true);
    }

}
