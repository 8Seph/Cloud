package com.ec.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientCommandHandler extends ChannelInboundHandlerAdapter {

    public ClientCommandHandler(MainController controller) {
        this.controller = controller;
    }

    private MainController controller;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;


        if (byteBuf.readByte() == 25) {
            ClientRequests.updateServerFileList(ctx, byteBuf, controller);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
