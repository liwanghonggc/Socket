package chp7.demo1.server;

import chp7.demo1.constants.TCPConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author lwh
 * @date 2019-02-21
 * @desp
 */
public class Server {

    public static void main(String[] args) throws IOException {

        //先启动TCPServer接收客户端的TCP连接
        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean isSucceed = tcpServer.start();
        if(!isSucceed){
            System.out.println("Start TCP Server failed!");
            return;
        }

        //然后再监听UDP的搜索
        UDPProvider.start(TCPConstants.PORT_SERVER);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            str = bufferedReader.readLine();
            tcpServer.broadCast(str);
        }while (!"00bye00".equalsIgnoreCase(str));

        UDPProvider.stop();
        tcpServer.stop();
    }
}
