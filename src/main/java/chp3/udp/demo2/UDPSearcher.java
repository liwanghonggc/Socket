package chp3.udp.demo2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author lwh
 * @date 2019-02-20
 * @desp UDP搜索者,用于搜索服务支持方
 */
public class UDPSearcher {

    /**
     * 监听端口号,用于监听回送回来的数据
     */
    private static final int LISTEN_PORT = 30000;

    public static void main(String[] args) throws IOException, InterruptedException{
        System.out.println("UDPSearcher Started");

        Listener listener = listen();

        sendBroadCast();

        //读取键盘任意信息后退出
        System.in.read();

        List<Device> devices = listener.getDevicesAndClose();
        for(Device device : devices){
            System.out.println("Device: " + device.toString());
        }

        System.out.println("UDPSearcher Finished");
    }

    private static Listener listen() throws InterruptedException {
        System.out.println("UDPSearcher Listener Started!");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, countDownLatch);
        listener.start();

        countDownLatch.await();
        return listener;
    }

    /**
     * 发送广播
     */
    private static void sendBroadCast() throws IOException{
        System.out.println("UDPSearcher sendBroadCast Started!");

        //作为搜索方,让系统自动分配端口
        DatagramSocket ds = new DatagramSocket();

        //构建一个请求数据
        String requestData = MessageCreator.buildWithPort(LISTEN_PORT);
        byte[] requestDataBytes = requestData.getBytes();
        DatagramPacket requestPacket = new DatagramPacket(requestDataBytes, requestDataBytes.length);

        //设置广播地址
        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        requestPacket.setPort(20000);

        ds.send(requestPacket);
        ds.close();

        System.out.println("UDPSearcher sendBroadCast Finished!");
    }

    private static class Device{
        final int port;
        final String ip;
        final String sn;

        public Device(int port, String ip, String sn) {
            this.port = port;
            this.ip = ip;
            this.sn = sn;
        }

        @Override
        public String toString() {
            return "Device{" +
                    "port=" + port +
                    ", ip='" + ip + '\'' +
                    ", sn='" + sn + '\'' +
                    '}';
        }
    }

    private static class Listener extends Thread {
        private final int listenPort;
        private final CountDownLatch countDownLatch;
        private final List<Device> devices = new ArrayList<>();
        private boolean done = false;
        private DatagramSocket ds = null;

        public Listener(int listenPort, CountDownLatch countDownLatch){
            super();
            this.listenPort = listenPort;
            //标识是否已经启动
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            super.run();

            //通知已启动
            countDownLatch.countDown();
            try {
                //监听回送端口
                ds = new DatagramSocket(listenPort);

                while (!done){
                    final byte[] buf = new byte[512];
                    DatagramPacket receivePack = new DatagramPacket(buf, buf.length);

                    //接收数据
                    ds.receive(receivePack);

                    //打印接收到的信息与发送者信息
                    String ip = receivePack.getAddress().getHostAddress();
                    int port = receivePack.getPort();
                    int dataLen = receivePack.getLength();
                    String data = new String(receivePack.getData(), 0, dataLen);
                    System.out.println("UDPSearcher receive from ip: " + ip + ", port: " + port + ", data: " + data);

                    String sn = MessageCreator.parseSn(data);
                    if(sn != null){
                        Device device = new Device(port, ip, sn);
                        devices.add(device);
                    }
                }

            }catch (Exception e){

            }finally {
                close();
            }

            System.out.println("UDPSearcher Listener Finished!");

        }

        private void close(){
            if(ds != null){
                ds.close();
                ds = null;
            }
        }

        List<Device> getDevicesAndClose(){
            done = true;
            close();
            return devices;
        }
    }

}
