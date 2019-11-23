package com.ec.server;

import com.ec.common.FilesList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class Requests {

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
    private String fileName;

    public void downloadFile(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {

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
                    StartHandler.fileSending = false;
                    sendFilesList(ctx);
                    out.close();
                    receivedFileLength = 0; // !!!!!
                }
            }
        }
        buf.release();
    }

    /**
     * Блок для работы с удалением фалов
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

    /**
     * Отправка клиенту списка файлов
     */
    public void sendFilesList(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Отправка списка файлов");
        List<String> filesList_SERVER = new LinkedList<>();
        Files.list(Paths.get(Server.FILES_PATH)).map(p -> p.getFileName().toString()).forEach(o -> filesList_SERVER.add(o));

        FilesList filesList = new FilesList(filesList_SERVER);
        ctx.writeAndFlush(filesList);

    }
}
