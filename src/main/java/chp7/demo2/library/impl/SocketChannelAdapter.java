package chp7.demo2.library.impl;

import chp7.demo2.library.core.IoArgs;
import chp7.demo2.library.core.IoProvider;
import chp7.demo2.library.core.Receiver;
import chp7.demo2.library.core.Sender;
import chp7.demo2.library.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 发送与接收的具体实现
 */
public class SocketChannelAdapter implements Sender, Receiver, Cloneable {

    /**
     * 标识该channel是否已经被关闭了
     */
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    /**
     * 具体发送的承载者
     */
    private final SocketChannel channel;

    private final IoProvider ioProvider;

    private final OnChannelStatusChangedListener listener;

    private IoArgs.IoArgsEventListener receiveIoEventListener;
    private IoArgs.IoArgsEventListener sendIoEventListener;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider, OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    /**
     * 接收消息
     */
    @Override
    public boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException {
        //已经关闭,抛出异常
        if(isClosed.get()){
            throw new IOException("Current Channel is Closed!");
        }

        receiveIoEventListener = listener;

        return ioProvider.registerInput(channel, inputCallback);
    }

    /**
     * 发送消息
     */
    @Override
    public boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException {
        //已经关闭,抛出异常
        if(isClosed.get()){
            throw new IOException("Current Channel is Closed!");
        }

        sendIoEventListener = listener;

        //当前要发送的数据附加到回调中
        outputCallback.setAttach(args);

        return ioProvider.registerOutput(channel, outputCallback);
    }

    @Override
    public void close() throws IOException {
        if(isClosed.compareAndSet(false, true)){
            //解除注册回调
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);

            //关闭
            CloseUtils.close(channel);

            //回调当前channel已关闭
            listener.onChannelClosed(channel);
        }
    }

    /**
     * channel关闭时发生的回调
     */
    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }

    /**
     * 具体的读取操作
     */
    private final IoProvider.HandleInputCallback inputCallback = new IoProvider.HandleInputCallback() {
        @Override
        protected void canProviderInput() {
            if(isClosed.get()){
                return;
            }

            IoArgs args = new IoArgs();
            IoArgs.IoArgsEventListener listener = SocketChannelAdapter.this.receiveIoEventListener;

            if(listener != null){
                listener.onStarted(args);
            }


            try{
                //具体的读取操作
                if(args.read(channel) > 0 && listener != null){
                    //读取完成的回调
                    listener.onCompleted(args);
                }else{
                    throw new IOException("Cannot read any data!");
                }
            }catch (IOException ignored){
                CloseUtils.close(SocketChannelAdapter.this);
            }

        }
    };

    private final IoProvider.HandleOutputCallback outputCallback = new IoProvider.HandleOutputCallback() {
        @Override
        protected void canProviderOutput(Object attach) {
            if(isClosed.get()){
                return;
            }

            //TODO
            sendIoEventListener.onCompleted(null);
        }
    };
}
