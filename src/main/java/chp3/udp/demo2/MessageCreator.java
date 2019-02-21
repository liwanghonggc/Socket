package chp3.udp.demo2;

/**
 * @author lwh
 * @date 2019-02-20
 * @desp
 */
public class MessageCreator {

    private static final String SN_HEADER = "收到暗号,我是(SN):";

    private static final String PORT_HEADER = "这是暗号,请回电端口(Port):";

    public static String buildWithPort(int port){
        return PORT_HEADER + port;
    }

    public static int parsePort(String data){
        if(data == null || data.length() == 0){
            return -1;
        }

        if(data.startsWith(PORT_HEADER)){
            return Integer.parseInt(data.substring(PORT_HEADER.length()));
        }

        return -1;
    }

    public static String buildWithSn(String sn){
        return SN_HEADER + sn;
    }

    public static String parseSn(String data){
        if(data == null || data.length() == 0){
            return null;
        }

        if(data.startsWith(SN_HEADER)){
            return data.substring(SN_HEADER.length());
        }

        return null;
    }
}
