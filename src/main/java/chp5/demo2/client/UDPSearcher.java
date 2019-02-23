package chp5.demo2.client;

import chp5.demo2.client.bean.ServerInfo;

import java.util.List;

/**
 * @author lwh
 * @date 2019-02-23
 * @desp UDP搜索方,搜索局域网内服务器的IP地址与端口号
 */
public class UDPSearcher {

    //只知道局域网内服务器的UPD端口,在该端口上进行搜索
    public static ServerInfo searchServerInfo(int port){

        //先监听
        listen(port);
        sendBroadCast();

        //再发送广播消息
        return null;
    }

    //先监听
    private static List<ServerInfo> listen(int port){
        return null;
    }

    //发送广播消息
    private static void sendBroadCast(){

    }

    //监听线程
    private static class Listener extends Thread{

    }
}
