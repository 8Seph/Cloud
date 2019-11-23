package com.ec.client;

import com.ec.common.FilesList;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class TmpHandler extends ChannelInboundHandlerAdapter {

    private MainController controller;

    public TmpHandler(MainController controller) {
        this.controller = controller;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        if (msg instanceof FilesList) {
            System.out.println("Получен список файлов");
            FilesList filesList = (FilesList)msg;
            controller.refreshServerFilesList(filesList);
            System.out.println(filesList.getFilesList());

        }


        ctx.flush();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
