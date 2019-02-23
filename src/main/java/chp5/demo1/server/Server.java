package chp5.demo1.server;

import chp5.demo1.constants.TCPConstants;

import java.io.IOException;

/**
 * @author lwh
 * @date 2019-02-21
 * @desp
 */
public class Server {

    public static void main(String[] args) {

        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER);
        boolean isSucceed = tcpServer.start();
        if(!isSucceed){
            System.out.println("Start TCP Server failed");
            return;
        }

        UDPProvider.start(TCPConstants.PORT_SERVER);

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        UDPProvider.stop();
        tcpServer.stop();
    }
}
