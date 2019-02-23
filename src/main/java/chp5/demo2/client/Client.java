package chp5.demo2.client;

import chp5.demo2.client.bean.ServerInfo;


/**
 * @author lwh
 * @date 2019-02-23
 * @desp 客户端启动类,发起搜索,获取服务器列表后,建立TCP连接进行数据传输
 */
public class Client {

    public static void main(String[] args) {
        ServerInfo serverInfo = UDPSearcher.searchServerInfo(222);

        System.out.println("搜索到的服务器信息: " + serverInfo);

        if(serverInfo != null){
            try{
                TCPClient.linkWith(serverInfo);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
