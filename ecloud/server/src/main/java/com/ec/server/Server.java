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
    public void run() throws Exception {
        // Пул потоков для обработки подключений клиентов
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        // Пул потоков для обработки сетевых сообщений
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // Создание настроек сервера
            ServerBootstrap b = new ServerBootstrap();
            b.group(mainGroup, workerGroup); // указание пулов потоков для работы сервера
            b.channel(NioServerSocketChannel.class); // указание канала для подключения новых клиентов
            b.childHandler(new ChannelInitializer<SocketChannel>() { // инициализация каналов, 3 - см конвеер

                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception { //настройка конвеера для каждого подключившегося клиента
                    socketChannel.pipeline().addLast(
                            new ObjectDecoder(50 * 1024 * 1024, ClassResolvers.cacheDisabled(null)),
                            new ObjectEncoder(), //вместо сериализации, используем стандартный декодер и энкодер
                            new MainHandler() // на этом этапе посылка летит к клиенту
                    );
                }
            })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture future = b.bind(8189).sync(); // запуск прослушивания порта 8189 для подключения клиентов
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
