package com.ec.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class StartHandler extends ChannelInboundHandlerAdapter {

    // Обьект для обработки запросов на сервере
    static boolean fileSending = false;

    Requests requests = new Requests();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;


        if (!fileSending) {
            System.out.println("--------- START HANDLER ---------");
            int command = byteBuf.readByte();
            System.out.println(command);

            // Инициализация загрузки файла на сервер
            if (command == 66) {
                fileSending = true;
                requests.downloadFile(ctx, byteBuf);
            }

            // Удаление файла на сервере
            if (command == 33) {
                requests.deleteFile(ctx, byteBuf);
            }

            // Отправка файл листа
            if (command == 25) {
                requests.sendFilesList(ctx);
            }

        } else {
            requests.downloadFile(ctx, byteBuf);
        }


    }

//        // Получения файла
//        if (fileSending) {
//            ctx.channel().pipeline().addLast(new FileHandler());
//            ctx.fireChannelRead(msg);
//            ctx.channel().pipeline().remove(this);
//            Requests.downloadFile(ctx, byteBuf);
//        }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
