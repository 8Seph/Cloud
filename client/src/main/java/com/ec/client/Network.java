package com.ec.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.LinkedList;

public class Network {
    protected static String IP_ADDRESS;
    private static Network instance = new Network();
    private Channel currentChannel;
    private EventLoopGroup group;
    private MainController controller;
    private boolean reconnect = true;


    private Network() {
    }

    public static Network getInstance() {
        return instance;
    }

    public void setController(MainController controller) {
        this.controller = controller;
    }

    public MainController getController() {
        return controller;
    }

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void start() {
        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group);
            b.channel(NioSocketChannel.class);
            b.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(
                            new ClientCommandHandler(controller)
                    );
                    currentChannel = socketChannel;
                }
            });

            ChannelFuture channelFuture = b.connect("localhost", 8189).sync();
            // todo
            controller.refreshConnectionState("ONLINE");
            controller.getFilesListOnServer();

            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                group.shutdownGracefully().sync();
                currentChannel.close();
                System.out.println("Channel closed");

                //todo
                controller.refreshServerFilesList(new LinkedList<>());
                controller.refreshConnectionState("OFFLINE");

                // todo переподключение
                if (reconnect){
                    Thread.sleep(10000);
                   getController().connectBtn();
               }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Новый поток для того чтобы не вис интерфейс при закрытии программы
    public void stop() {
        reconnect = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    group.shutdownGracefully().sync();
                    currentChannel.close();
                } catch (Exception e) {
                    System.out.println("Close, no connect");

                    //todo
                    controller.refreshServerFilesList(new LinkedList<>());
                    controller.refreshConnectionState("OFFLINE");
                }
            }
        }).start();
    }
}
