package chp7.demo1.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author lwh
 * @date 2019-02-23
 * @desp
 */
public class CloseUtils {

    public static void close(Closeable... closeables){
        if(closeables == null){
            return;
        }

        for(Closeable closeable : closeables){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
