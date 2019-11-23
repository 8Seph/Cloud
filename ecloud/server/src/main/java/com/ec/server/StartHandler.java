package com.ec.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class StartHandler extends ChannelInboundHandlerAdapter {

    // Обьект для обработки запросов на сервере

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("--------- START HANDLER ---------");
        ByteBuf byteBuf = (ByteBuf) msg;
        int command = byteBuf.readByte();
        System.out.println(command);

        // Получения файла
        if (command == 66) {
            ctx.channel().pipeline().addLast(new FileHandler());
            ctx.fireChannelRead(msg);
            ctx.channel().pipeline().remove(this);
        }

        // Отправка файл листа
        if (command == 25) {
            Requests.sendFilesList(ctx);
        }

        // Удаление файла на сервере
        if (command == 33) {
            Requests.deleteFile(ctx, byteBuf);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
