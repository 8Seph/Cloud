package com.ec.server;

import com.ec.common.FilesList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class Requests {


    private enum DeleteState {
        NAME_LENGTH, NAME
    }

    private static DeleteState deleteState = DeleteState.NAME_LENGTH;


    public static void sendFilesList(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Отправка списка файлов");
        List<String> filesList_SERVER = new LinkedList<>();
        Files.list(Paths.get(Server.FILES_PATH)).map(p -> p.getFileName().toString()).forEach(o -> filesList_SERVER.add(o));

        FilesList filesList = new FilesList(filesList_SERVER);
        ctx.writeAndFlush(filesList);

    }

    public static void deleteFile(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        int nameLength = 0;
        String fileName;

        // 1. Получение длины имени.
        if (deleteState == DeleteState.NAME_LENGTH) {
            if (byteBuf.readableBytes() >= 4) {
                nameLength = byteBuf.readInt();
                deleteState = DeleteState.NAME;
                System.out.println("Длина имени: " + nameLength);
            }
        }

        // 2. Получение массива байт имени.
        if (deleteState == DeleteState.NAME) {
            if (byteBuf.readableBytes() >= nameLength) {
                byte[] tmp = new byte[nameLength];
                byteBuf.readBytes(tmp); // запись данных из буффера в массив
                fileName = new String(tmp);
                System.out.println("Имя файла для удаления: " + fileName);
                deleteState = DeleteState.NAME_LENGTH;
                Files.delete(Paths.get(Server.FILES_PATH + fileName));
                sendFilesList(ctx);
                ctx.flush();
            }
        }


    }
}
