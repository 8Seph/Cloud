package com.ec.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class FileHandler extends ChannelInboundHandlerAdapter {
    public enum State {
        NAME_LENGTH, NAME, FILE_SIZE, SENDING
    }

    private State currentState = State.NAME_LENGTH;
    private int nameLength;
    private String name;
    private long fileSize;
    private long receivedFileLength;
    private BufferedOutputStream out;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);

        // 1. Получение длины имени.
        if (currentState == State.NAME_LENGTH) {
            if (buf.readableBytes() >= 4) {
                nameLength = buf.readInt();
                currentState = State.NAME;
                System.out.println("Длина имени: " + nameLength);
            }
        }

        // 2. Получение массива байт имени.
        if (currentState == State.NAME) {
            if (buf.readableBytes() >= nameLength) {
                byte[] tmp = new byte[nameLength];
                buf.readBytes(tmp); // запись данных из буффера в массив
                name = new String(tmp);
                out = new BufferedOutputStream(new FileOutputStream(Server.FILES_PATH + name));
                currentState = State.FILE_SIZE;
                System.out.println("Имя файла: " + name);
            }
        }

        // 3. Получение размера файла.
        if (currentState == State.FILE_SIZE) {
            if (buf.readableBytes() >= 8) {
                fileSize = buf.readLong();
                System.out.println("Размер файла: " + fileSize);
                currentState = State.SENDING;
            }
        }

        // 4. Передача файла.
        if (currentState == State.SENDING) {
            while (buf.readableBytes() > 0) {
                out.write(buf.readByte());
                receivedFileLength++;

                if (fileSize == receivedFileLength) {
                    currentState = State.NAME_LENGTH;
                    System.out.println("Фаил загружен!");
                    Requests.sendFilesList(ctx);
                    out.close();
                    ctx.channel().pipeline().remove(this);
                    ctx.channel().pipeline().addFirst(new StartHandler());
                }
            }
        }

        buf.release();
    }

    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
