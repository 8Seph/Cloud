package com.ec.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class ServerRequests {


    /**
     * Блок для работы с загрузкой файлов
     * todo описание
     */

    public enum DownloadState {
        NAME_LENGTH, NAME, FILE_SIZE, SENDING
    }

    private DownloadState currentState = DownloadState.NAME_LENGTH;
    private int nameLength;
    private BufferedOutputStream out;
    private long receivedFileLength;
    private long fileSize;


    public void downloadFile(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        String fileName;
        // 1. Получение длины имени.
        if (currentState == DownloadState.NAME_LENGTH) {
            if (buf.readableBytes() >= 4) {
                nameLength = buf.readInt();
                currentState = DownloadState.NAME;
                System.out.println("Длина имени: " + nameLength);
            }
        }

        // 2. Инициализация имени.
        if (currentState == DownloadState.NAME) {
            if (buf.readableBytes() >= nameLength) {
                byte[] tmp = new byte[nameLength];
                buf.readBytes(tmp); // запись данных из буффера в массив
                fileName = new String(tmp);
                out = new BufferedOutputStream(new FileOutputStream(Server.FILES_PATH + fileName));
                currentState = DownloadState.FILE_SIZE;
                System.out.println("Имя файла: " + fileName);
            }
        }

        // 3. Получение размера файла.
        if (currentState == DownloadState.FILE_SIZE) {
            if (buf.readableBytes() >= 8) {
                fileSize = buf.readLong();
                System.out.println("Размер файла: " + fileSize);
                currentState = DownloadState.SENDING;
            }
        }

        // 4. Передача файла.
        if (currentState == DownloadState.SENDING) {
            while (buf.readableBytes() > 0) {
                out.write(buf.readByte());
                receivedFileLength++;
                if (fileSize == receivedFileLength) {
                    currentState = DownloadState.NAME_LENGTH;
                    System.out.println("Фаил загружен!");
                    ServerCommandHandler.fileSending = false;
                    sendFilesList(ctx);
                    out.close();
                    receivedFileLength = 0; // !!!!!
                }
            }
        }
        buf.release();
    }

    /**
     * Блок отправки файла клиенту
     * todo описание
     */

    public void sendFile(ChannelHandlerContext ctx, ByteBuf nameBuf) throws IOException {

        String fileName = "";
        Path path = Paths.get(Server.FILES_PATH + fileName);
        FileRegion region = new DefaultFileRegion(new FileInputStream(path.toFile()).getChannel(), 0, Files.size(path));
        byte[] fileName_byteArr = path.getFileName().toString().getBytes();

        // Команда для подготовки хендлепа на передачу файла
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(1);
        byteBuf.writeByte((byte) 66);
        ctx.write(byteBuf);

        // Отправка длины имени
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
        byteBuf.writeInt(path.getFileName().toString().length());
        ctx.write(byteBuf);


        // Отправка имени
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(fileName_byteArr.length);
        byteBuf.writeBytes(fileName_byteArr);
        ctx.write(byteBuf);

        // Отправка размера файла
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(8);
        byteBuf.writeLong(Files.size(path));
        ctx.write(byteBuf);

        ctx.flush();
        ctx.writeAndFlush(region);
    }

    /**
     * Блок для работы с удалением файлов
     * todo описание
     */

    private enum DeleteState {
        NAME_LENGTH, NAME
    }

    private DeleteState deleteState = DeleteState.NAME_LENGTH;

    public void deleteFile(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
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

        // 2. Инициализация имени и удаление файла.
        if (deleteState == DeleteState.NAME) {
            if (byteBuf.readableBytes() >= nameLength) {
                byte[] tmp = new byte[nameLength];
                byteBuf.readBytes(tmp); // запись данных из буффера в массив
                fileName = new String(tmp);

                Files.delete(Paths.get(Server.FILES_PATH + fileName));
                deleteState = DeleteState.NAME_LENGTH;

                sendFilesList(ctx);
                ctx.flush();
                System.out.println("Фаил удален: " + fileName);
            }
        }
    }

    /**
     * Отправка клиенту списка файлов
     */
    public void sendFilesList(ChannelHandlerContext ctx) throws Exception {

        List<String> filesList = new LinkedList<>();
        Files.list(Paths.get(Server.FILES_PATH)).map(p -> p.getFileName().toString()).forEach(o -> filesList.add(o));

        // Команда для подготовки принятия списка файлов
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(1);
        byteBuf.writeByte((byte) 25);
        ctx.write(byteBuf);

        // Количество файлов в каталоге
        byteBuf = ByteBufAllocator.DEFAULT.buffer(4);
        byteBuf.writeInt(filesList.size());
        ctx.write(byteBuf);

        ctx.flush();

        for (int i = 0; i < filesList.size(); i++) {
            int nameLength = filesList.get(i).length();
            byteBuf = ByteBufAllocator.DEFAULT.buffer(4);
            byteBuf.writeInt(nameLength);
            ctx.write(byteBuf);

            byteBuf = ByteBufAllocator.DEFAULT.directBuffer(nameLength);
            byte[] fileName = filesList.get(i).getBytes();
            byteBuf.writeBytes(fileName);
            ctx.write(byteBuf);
        }

        ctx.flush();

    }
}
