package chp5.demo3.client;

import chp5.demo3.client.bean.ServerInfo;

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
            try {
                //搜索到信息进行TCP连接
                TCPClient.linkWith(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
