package chp5.client;

import chp5.client.bean.ServerInfo;

/**
 * @author lwh
 * @date 2019-02-21
 * @desp
 */
public class Client {

    public static void main(String[] args) {
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
