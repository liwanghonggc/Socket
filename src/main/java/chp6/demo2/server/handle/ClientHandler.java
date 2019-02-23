package chp6.demo2.server.handle;

import chp6.demo2.utils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lwh
 * @date 2019-02-23
 * @desp
 */
public class ClientHandler {

    private final Socket socket;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;

    public ClientHandler(Socket socket, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.socket = socket;
        this.readHandler = new ClientReadHandler(socket.getInputStream());
        this.writeHandler = new ClientWriteHandler(socket.getOutputStream());
        this.clientHandlerCallback = clientHandlerCallback;
        this.clientInfo = "Address[" + socket.getInetAddress().getHostAddress() + "] + Port[" + socket.getPort() + "]";
        System.out.println("新客户端连接: " + clientInfo);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    /**
     * 发送
     * @param str
     */
    public void send(String str) {
        writeHandler.send(str);
    }

    /**
     * 退出
     */
    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(socket);
        System.out.println("客户端已退出: " + socket.getInetAddress() + " port: " + socket.getPort());
    }

    private void exitBySelf(){
        exit();
        clientHandlerCallback.onSelfClosed(this);
    }

    public interface ClientHandlerCallback {
        //自身关闭
        void onSelfClosed(ClientHandler handler);

        //收到消息时通知
        void onNewMessageArrived(ClientHandler handler, String msg);
    }

    /**
     * 读取数据并打印,收发分离
     */
    public void readToPrint() {
        readHandler.start();
    }

    /**
     * 读取数据Handler
     */
    class ClientReadHandler extends Thread {

        private boolean done = false;
        private final InputStream inputStream;

        ClientReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();

            try {
                //得到输入流,用于接收数据
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                do {
                    //客户端拿到一条数据
                    String str = socketInput.readLine();
                    //可能是出现异常了
                    if(str == null){
                        System.out.println("客户端已无法读取数据!");
                        //退出当前客户端
                        ClientHandler.this.exitBySelf();
                        break;
                    }

                    //这里做了改造
                    //当收到消息时需要将自身以及消息通知回去给TcpServer
                    clientHandlerCallback.onNewMessageArrived(ClientHandler.this, str);

                }while (!done);
            } catch (IOException e) {
                if(!done){
                    System.out.println("连接异常断开");
                    //退出当前客户端
                    ClientHandler.this.exitBySelf();
                }

            } finally {
                CloseUtils.close(inputStream);
            }
        }

        void exit(){
            done = true;
            CloseUtils.close(inputStream);
        }
    }

    class ClientWriteHandler{
        private boolean done = false;
        private final PrintStream printStream;
        private final ExecutorService executorService;

        ClientWriteHandler(OutputStream outputStream){
            this.printStream = new PrintStream(outputStream);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        void send(String str) {
            if(done){
                return;
            }
            executorService.execute(new WriteRunnable(str));
        }

        void exit(){
            done = true;
            CloseUtils.close(printStream);
            executorService.shutdownNow();
        }

        class WriteRunnable implements Runnable {
            private final String msg;

            WriteRunnable(String msg){
                this.msg = msg;
            }

            @Override
            public void run() {

                if(ClientWriteHandler.this.done){
                    return;
                }

                try {
                    ClientWriteHandler.this.printStream.println(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
