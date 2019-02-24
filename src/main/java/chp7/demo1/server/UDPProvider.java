package chp7.demo1.server;

import chp7.demo1.constants.UDPConstants;
import chp7.demo1.utils.ByteUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @author lwh
 * @date 2019-02-21
 * @desp
 */
public class UDPProvider {

    private static Provider PROVIDER_INSTANCE;

    static void start(int port){
        stop();

        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn, port);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    static void stop(){
        if(PROVIDER_INSTANCE != null){
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }

    private static class Provider extends Thread {
        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket ds = null;

        //存储消息的buffer
        final byte[] buffer = new byte[128];

        Provider(String sn, int port){
            super();
            this.sn = sn.getBytes();
            this.port = port;
        }

        @Override
        public void run() {
            super.run();

            System.out.println("UDPProvider Started");

            try {
                //监听30201端口
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);
                //接收消息的Packet
                DatagramPacket receivePack = new DatagramPacket(buffer, buffer.length);

                while (!done){
                    //接收
                    ds.receive(receivePack);

                    //打印接收到的信息与发送者信息
                    String clientIp = receivePack.getAddress().getHostAddress();
                    int clientPort = receivePack.getPort();
                    int clientDataLen = receivePack.getLength();
                    byte[] clientData = receivePack.getData();

                    boolean isValid = clientDataLen >= (UDPConstants.HEADER.length + 2 + 4) && ByteUtils.startWith(clientData, UDPConstants.HEADER);

                    System.out.println("UDPProvider receive from ip: " + clientIp + " port: " + clientPort + " dataValid: " + isValid);

                    if(!isValid){
                        //无效继续
                        continue;
                    }

                    //解析命令与回送端口
                    int index = UDPConstants.HEADER.length;
                    short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xff));

                    int responsePort = ((clientData[index++]) << 24) |
                                       ((clientData[index++] & 0xff) << 16) |
                                       ((clientData[index++] & 0xff) << 8) |
                                       ((clientData[index] & 0xff));


                    //判断合法性,1代表搜索命令
                    if(cmd == 1 && responsePort > 0){
                        //构建一份回送数据
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPConstants.HEADER);
                        byteBuffer.putShort((short)2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);
                        int len = byteBuffer.position();

                        //直接根据发送者构建一份回送信息
                        DatagramPacket responsePacket = new DatagramPacket(buffer, len, receivePack.getAddress(), responsePort);

                        ds.send(responsePacket);

                        System.out.println("UDPProvider response to: " + clientIp + " port: " + responsePort + " dataLen: " + len);
                    }else{
                        System.out.println("UDPProvider receive nonsupport; cmd: " + cmd + " port: " + port);
                    }
                }
            } catch (IOException e) {

            } finally {
                close();
            }

            System.out.println("UDPProvider Finished");
        }

        private void close(){
            if(ds != null){
                ds.close();
                ds = null;
            }
        }

        void exit(){
            done = true;
            close();
        }
    }
}
