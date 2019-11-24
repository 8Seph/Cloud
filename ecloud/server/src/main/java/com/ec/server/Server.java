package com.ec.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;


public class Server {
    public static final String FILES_PATH = "D:/storage/server/";

    public void run() throws Exception {

        EventLoopGroup mainGroup = new NioEventLoopGroup(); // Пул потоков для обработки подключений клиентов
        EventLoopGroup workerGroup = new NioEventLoopGroup();  // Пул потоков для обработки сетевых сообщений
        try {
            ServerBootstrap b = new ServerBootstrap();  // Создание настроек сервера
            b.group(mainGroup, workerGroup); // указание пулов потоков для работы сервера
            b.channel(NioServerSocketChannel.class); // указание канала для подключения новых клиентов
            b.childHandler(new ChannelInitializer<SocketChannel>() { // инициализация каналов, 3 - см конвеер

                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception { //настройка конвеера для каждого подключившегося клиента
                    socketChannel.pipeline().addLast(
                            new ServerCommandHandler()
                    );
                }
            })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = b.bind(8189).sync(); // прослушивание порта 8189 для подключения клиентов
            future.channel().closeFuture().sync(); // ожидание завершения работы сервера
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new Server().run();
    }
}
