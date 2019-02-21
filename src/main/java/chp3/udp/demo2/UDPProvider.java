package chp3.udp.demo2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.UUID;

/**
 * @author lwh
 * @date 2019-02-20
 * @desp UDP提供者,用于提供服务
 */
public class UDPProvider {

    public static void main(String[] args) throws IOException {
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn);
        provider.start();

        //读取任意键盘字符后退出
        System.in.read();
        provider.exit();
    }

    private static class Provider extends Thread {

        private final String sn;
        private boolean done = false;
        private DatagramSocket ds = null;

        public Provider(String sn) {
            super();
            this.sn = sn;
        }

        @Override
        public void run() {
            super.run();

            System.out.println("UDPProvider Started");

            try {
                //作为接收者,计算机B,指定一个端口用于数据接收
                ds = new DatagramSocket(20000);

                while(!done){
                    //构建接收实体
                    final byte[] buf = new byte[512];
                    DatagramPacket receivePack = new DatagramPacket(buf, buf.length);

                    //接收数据
                    ds.receive(receivePack);

                    //打印接收到的信息与发送者信息
                    String ip = receivePack.getAddress().getHostAddress();
                    int port = receivePack.getPort();
                    int dataLen = receivePack.getLength();
                    String data = new String(receivePack.getData(), 0, dataLen);
                    System.out.println("UDPProvider receive from ip: " + ip + ", port: " + port + ", data: " + data);

                    //解析端口号
                    int responsePort = MessageCreator.parsePort(data);
                    if(responsePort != -1){
                        //构建一个回送数据
                        String responseData = MessageCreator.buildWithSn(sn);
                        byte[] responseDataBytes = responseData.getBytes();
                        //直接根据发送者构建一个回送消息
                        DatagramPacket responsePacket = new DatagramPacket(responseDataBytes, responseDataBytes.length, receivePack.getAddress(), responsePort);
                        ds.send(responsePacket);
                    }
                }
            } catch (IOException e) {

            } finally {
                close();
            }

            //完成
            System.out.println("UDPProvider Finished!");
        }

        private void close(){
            if(ds != null){
                ds.close();
                ds = null;
            }
        }

        /**
         * 结束
         */
        void exit(){
            done = true;
            close();
        }
    }

}
