package com.ec.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

public class Network {
    protected static String IP_ADDRESS;
    private static Network instance = new Network();
    private Channel currentChannel;
    private EventLoopGroup group;
    private MainController controller;


    private Network() {
    }

    public static Network getInstance() {
        return instance;
    }

    public void setController(MainController controller) {
        this.controller = controller;
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
                            new ObjectDecoder(50 * 1024 * 1024, ClassResolvers.cacheDisabled(null)),
                           // new ObjectEncoder(),
                            new TmpHandler(controller)
                    );
                    currentChannel = socketChannel;
                }
            });
            ChannelFuture channelFuture = b.connect("localhost", 8189).sync();
            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                group.shutdownGracefully().sync();
                currentChannel.close();
                System.out.println("Channel closed");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    group.shutdownGracefully().sync();
                    currentChannel.close();
                } catch (Exception e) {
                    System.out.println("Close, no connect");
                }
            }
        }).start();
    }
}