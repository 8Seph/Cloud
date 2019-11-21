package com.ec.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestHandler extends ChannelInboundHandlerAdapter {
    public enum State {
        VERIFICATION, NAME_LENGTH, NAME, FILE_LENGTH, FILE, FILES_LIST
    }

    private State currentState = State.VERIFICATION;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    private String FILES_PATH;

    public TestHandler(String FILES_PATH) {
        this.FILES_PATH = FILES_PATH;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);

        if (currentState == State.VERIFICATION) {
            byte readed = buf.readByte();

            if (readed == (byte) 66) {
                currentState = State.NAME_LENGTH;
                System.out.println("STATE: Start file uploading");
            } else if (readed == (byte) 99) {
                currentState = State.FILES_LIST;
                System.out.println("Request FilesList");
            } else {
                System.out.println("ERROR: Invalid first byte - " + readed);
            }
        }

        if (currentState == State.FILES_LIST) {
            sendFilesList(ctx);
        }

        if (currentState == State.NAME_LENGTH) {
            if (buf.readableBytes() >= 4) {
                System.out.println("STATE: Get filename length");
                nextLength = buf.readInt();
                currentState = State.NAME;
            }
        }

        if (currentState == State.NAME) {
            if (buf.readableBytes() >= nextLength) {
                byte[] fileName = new byte[nextLength];
                buf.readBytes(fileName);
                System.out.println("STATE: Filename received - _" + new String(fileName));
                out = new BufferedOutputStream(new FileOutputStream(FILES_PATH + new String(fileName)));
                currentState = State.FILE_LENGTH;
            }
        }

        if (currentState == State.FILE_LENGTH) {
            if (buf.readableBytes() >= 8) {
                fileLength = buf.readLong();
                System.out.println("STATE: File length received - " + fileLength);
                currentState = State.FILE;
            }
        }

        if (currentState == State.FILE) {
            while (buf.readableBytes() > 0) {
                out.write(buf.readByte());
                receivedFileLength++;
                if (fileLength == receivedFileLength) {
                    currentState = State.VERIFICATION;
                    System.out.println("File received");
                    out.close();
                }
            }
        }
       buf.release();

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      //  ctx.flush();
    }


    public void sendFilesList(ChannelHandlerContext ctx) throws Exception {
        ByteBuf bufout = null;
        bufout = ByteBufAllocator.DEFAULT.directBuffer(1);
        bufout.writeByte((byte) 99);
        ctx.writeAndFlush(bufout);
        System.out.println("FilesList sent to client");

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
