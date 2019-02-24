package chp7.demo2.library.impl;

import chp7.demo2.library.core.IoProvider;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 注册连接、解除连接
 */
public class IoSelectorProvider implements IoProvider {

    /**
     * 标识该channel是否已经被关闭了
     */
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    /**
     * 是否处于某线程中
     */
    private final AtomicBoolean inReqInput = new AtomicBoolean(false);
    private final AtomicBoolean inReqOutput = new AtomicBoolean(false);

    private final Selector readSelector;

    private final Selector writeSelector;

    private final ExecutorService inputHandPool;

    private final ExecutorService outputHandPool;

    /**
     * 回调的Map
     */
    private final HashMap<SelectionKey, Runnable> inputCallbackMap = new HashMap<>();
    private final HashMap<SelectionKey, Runnable> outputCallbackMap = new HashMap<>();

    public IoSelectorProvider() throws IOException {
        readSelector = Selector.open();
        writeSelector = Selector.open();

        inputHandPool = Executors.newFixedThreadPool(4, new IoProviderThreadFactory("IoProvider-Input-Thread"));
        outputHandPool = Executors.newFixedThreadPool(4, new IoProviderThreadFactory("IoProvider-Output-Thread"));

        //开始输入输出的监听
        startRead();
        startWrite();
    }

    private void startWrite() {
        Thread thread = new Thread("IoSelectorProvider WriteSelector Thread"){
            @Override
            public void run() {
                while (!isClosed.get()){

                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    private void startRead() {
        Thread thread = new Thread("IoSelectorProvider ReadSelector Thread"){
            @Override
            public void run() {
                while (!isClosed.get()){
                    try {
                        //1处
                        if(readSelector.select() == 0){
                            continue;
                        }

                        Set<SelectionKey> selectionKeys = readSelector.selectedKeys();
                        for(SelectionKey selectionKey : selectionKeys){
                            if(selectionKey.isValid()){
                                //2处
                                //使用一个单独的线程去Select,Select到了之后就交给线程池去执行
                                handleSelection(selectionKey, SelectionKey.OP_READ, inputCallbackMap, inputHandPool);
                            }
                        }
                        selectionKeys.clear();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }


    @Override
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
        return regiserSelection(channel, readSelector, SelectionKey.OP_READ, inReqInput, inputCallbackMap, callback) != null;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
        return regiserSelection(channel, writeSelector, SelectionKey.OP_WRITE, inReqOutput, outputCallbackMap, callback) != null;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {

    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {

    }

    @Override
    public void close() throws IOException {

    }

    private static SelectionKey regiserSelection(SocketChannel channel, Selector selector,
                                int registerOps, AtomicBoolean locker,
                                HashMap<SelectionKey, Runnable> map,
                                Runnable runnable){
        synchronized (locker){
            //设置处于锁定状态
            locker.set(true);

            try{
                //唤醒Selector,让select不处于select状态,处于select状态时去注册是无效的
                selector.wakeup();

                SelectionKey key = null;
                if(channel.isRegistered()){
                    //查询是否已经注册过
                    key = channel.keyFor(selector);
                    if(key != null){
                        //重新设置下就行
                        key.interestOps(key.readyOps() | registerOps);
                    }
                }

                if(key == null){
                    //注册selector得到key
                    key = channel.register(selector, registerOps);
                    //注册回调
                    map.put(key, runnable);
                }

                return key;

            } catch (ClosedChannelException e) {
                return null;
            } finally {
                //解除锁定
                locker.set(false);
                try{
                    //通知
                    locker.notify();
                }catch (Exception ignored){
                }
            }
        }
    }


    /**
     * 交给线程池去执行具体的读数据
     */
    private static void handleSelection(SelectionKey key, int keyOps, HashMap<SelectionKey,Runnable> map, ExecutorService pool) {
        //重点
        //取消继续对keyOps的监听,也就是说之前channel注册了,现在解除注册,因为2处是一个异步的操作直接返回,如果2处线程池去读取数据这个操作缓慢,则1处select时还可以select到读事件
        //而事实上这个读事件已经被线程池去处理了,这里如果不解除绑定,可能造成数据多次读取
        key.interestOps(key.readyOps() & ~keyOps);

        Runnable runnable = null;

        try{
            runnable = map.get(key);
        }catch (Exception ignored){

        }

        if(runnable != null && !pool.isShutdown()){
            //异步调度
            pool.execute(runnable);
        }
    }


    static class IoProviderThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        IoProviderThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
