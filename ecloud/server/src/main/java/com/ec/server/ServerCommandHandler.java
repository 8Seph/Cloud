package com.ec.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerCommandHandler extends ChannelInboundHandlerAdapter {

    private ServerRequests requests = new ServerRequests();
    protected static boolean fileSending = false; // флаг для обозначения загрузки файла

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;

        if (!fileSending) {
            int command = byteBuf.readByte();
            System.out.println("HANDLER:\nCommand: " + command);

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

            // Отправка файла
            if (command == 99){
                requests.sendFile(ctx, byteBuf);
            }

        } else {
            // Продолжение загрузки файла
            requests.downloadFile(ctx, byteBuf);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
