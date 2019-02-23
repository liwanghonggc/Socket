package chp5.demo2.client.bean;

/**
 * @author lwh
 * @date 2019-02-23
 * @desp UDP搜索到的服务器的SN、IP、Port封装类
 */
public class ServerInfo {

    private String sn;
    private String ip;
    private int port;

    public ServerInfo(String sn, String ip, int port) {
        this.sn = sn;
        this.ip = ip;
        this.port = port;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "sn='" + sn + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
