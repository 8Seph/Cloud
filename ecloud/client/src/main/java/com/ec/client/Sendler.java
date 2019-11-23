package com.ec.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Sendler {

    public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException, InterruptedException {

        FileRegion region = new DefaultFileRegion(new FileInputStream(path.toFile()).getChannel(), 0, Files.size(path));
        byte[] fileName = path.getFileName().toString().getBytes();


        // Команда для подготовки хендлепа на передачу файла
        ByteBuf byteBuf  = ByteBufAllocator.DEFAULT.directBuffer(1);
        byteBuf.writeByte((byte) 66);
        channel.write(byteBuf);

        // Отправка длины имени
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
        byteBuf.writeInt(path.getFileName().toString().length());
        channel.write(byteBuf);


        // Отправка имени
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(fileName.length);
        byteBuf.writeBytes(fileName);
        channel.write(byteBuf);

        // Отправка размера файла
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(8);
        byteBuf.writeLong(Files.size(path));
        channel.write(byteBuf);

        channel.flush();


        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }

    }

    public static void getServerFilesList(Channel channel) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte((byte) 25);
        channel.writeAndFlush(buf);
    }

    public static void deleteFileOnServer(Channel channel, Path path) {
        byte[] fileName = path.getFileName().toString().getBytes();

        //Отправка команды для начала удаления файла
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(1);
        byteBuf.writeByte((byte) 33);
        channel.write(byteBuf);

        // Отправка длины имени
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
        byteBuf.writeInt(path.getFileName().toString().length());
        channel.write(byteBuf);

        // Отправка имени
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(fileName.length);
        byteBuf.writeBytes(fileName);
        channel.write(byteBuf);

        channel.flush(); // вынесено отдельно, для того чтобы байты отправились 100% в правильном порядке
    }
}
