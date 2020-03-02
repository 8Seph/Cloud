package com.ec.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerCommandHandler extends ChannelInboundHandlerAdapter {

    private ServerCommandManager serverCommandManager = new ServerCommandManager();
    private boolean downFlag = false; // флаг для обозначения загрузки файла

    public void setDownFlag(boolean downFlag) {
        this.downFlag = downFlag;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;

        if (!downFlag) {
            int command = byteBuf.readByte();
            System.out.println("HANDLER:\nCommand: " + command);

            // Инициализация загрузки файла на сервер
            if (command == 66) {
                downFlag = true;
                serverCommandManager.downloadFile(ctx, byteBuf, this);
            }

            // Удаление файла на сервере
            if (command == 33) {
                serverCommandManager.deleteFile(ctx, byteBuf);
            }

            // Отправка файл листа клиенту
            if (command == 25) {
                serverCommandManager.sendFilesList(ctx);
            }

            // Отправка файла клиенту
            if (command == 99) {
                serverCommandManager.sendFile(ctx, byteBuf);
            }

        } else {
            // Продолжение загрузки файла
            serverCommandManager.downloadFile(ctx, byteBuf, this);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
