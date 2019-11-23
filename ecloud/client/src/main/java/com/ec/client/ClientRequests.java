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
import java.util.LinkedList;
import java.util.List;

public class ClientRequests {

    public static void sendFile(Path path, Channel ctx, ChannelFutureListener finishListener) throws IOException, InterruptedException {

        FileRegion region = new DefaultFileRegion(new FileInputStream(path.toFile()).getChannel(), 0, Files.size(path));
        byte[] fileName = path.getFileName().toString().getBytes();

        // Команда для подготовки хендлепа на передачу файла
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(1);
        byteBuf.writeByte((byte) 66);
        ctx.write(byteBuf);

        // Отправка длины имени
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
        byteBuf.writeInt(path.getFileName().toString().length());
        ctx.write(byteBuf);


        // Отправка имени
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(fileName.length);
        byteBuf.writeBytes(fileName);
        ctx.write(byteBuf);

        // Отправка размера файла
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(8);
        byteBuf.writeLong(Files.size(path));
        ctx.write(byteBuf);

        ctx.flush();
        ChannelFuture transferOperationFuture = ctx.writeAndFlush(region);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }

    }

    public static void requestFile(String fileName) {

    }

    public static void updateServerFileList(ChannelHandlerContext ctx, ByteBuf byteBuf, MainController controller) {

        List<String> filesList = new LinkedList<>();

        // Количество файлов
        int filesCount = byteBuf.readInt();

        // Прием списка файлов из каталога
        for (int i = 0; i < filesCount; i++) {
            int nameLength = byteBuf.readInt();
            byte[] name = new byte[nameLength];
            byteBuf.readBytes(name);
            filesList.add(new String(name));
        }

        controller.refreshServerFilesList(filesList);
        byteBuf.release();
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
