package chp6.demo2.utils;

/**
 * @author lwh
 * @date 2019-02-21
 * @desp
 */
public class ByteUtils {

    public static boolean startWith(byte[] data, byte[] header) {
        if(data == null || data.length == 0){
            return false;
        }

        String dataStr = new String(data);
        String headerStr = new String(header);
        return dataStr.startsWith(headerStr);
    }
}
