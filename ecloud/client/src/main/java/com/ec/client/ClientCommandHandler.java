package com.ec.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.IOException;

public class ClientCommandHandler extends ChannelInboundHandlerAdapter {

    public ClientCommandHandler(MainController controller) {
        this.controller = controller;
    }

    private MainController controller;
    protected static boolean fileSending = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        ByteBuf byteBuf = (ByteBuf) msg;

        if (!fileSending){
            int command = byteBuf.readByte();
            System.out.println("HANDLER:\nCommand: " + command);

            // Начало загрузки файла с сервера
            if (command == 66){
                fileSending = true;
                ClientRequests.downloadFile(ctx, byteBuf);
            }

            // Получение списка файлов
            if (command == 25) {
                ClientRequests.updateServerFileList(ctx, byteBuf, controller);
            }
        } else {
            ClientRequests.downloadFile(ctx, byteBuf);
        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
