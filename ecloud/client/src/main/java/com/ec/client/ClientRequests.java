package com.ec.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public static void requestFile(Channel ctx, Path path) throws IOException {
        int nameLength = path.getFileName().toString().length();
        byte[] fileName = path.getFileName().toString().getBytes();

        // Command
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(1);
        byteBuf.writeByte(99);
        ctx.write(byteBuf);

        // Length
        byteBuf = ByteBufAllocator.DEFAULT.buffer(4);
        byteBuf.writeInt(nameLength);
        ctx.write(byteBuf);

        // FileName
        byteBuf = ByteBufAllocator.DEFAULT.buffer(8);
        byteBuf.writeBytes(fileName);
        ctx.write(byteBuf);

        ctx.flush();

    }

    public static void updateServerFileList(ChannelHandlerContext ctx, ByteBuf byteBuf, MainController controller) {

        List<String> filesList = new LinkedList<>();
        int filesCount = 0;

        // Количество файлов
        if (byteBuf.readableBytes() >= 4) {
            filesCount = byteBuf.readInt();
        }

        // Прием списка файлов из каталога
        if (byteBuf.readableBytes() >= 4) {
            for (int i = 0; i < filesCount; i++) {
                int nameLength = byteBuf.readInt();
                byte[] name = new byte[nameLength];
                byteBuf.readBytes(name);
                filesList.add(new String(name));
            }
        }
        controller.refreshServerFilesList(filesList);
        byteBuf.release();
    }

    public static void getServerFilesList(Channel ctx) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte((byte) 25);
        ctx.writeAndFlush(buf);
    }

    public static void deleteFileOnServer(Channel ctx, Path path) {
        byte[] fileName = path.getFileName().toString().getBytes();

        //Отправка команды для начала удаления файла
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(1);
        byteBuf.writeByte((byte) 33);
        ctx.write(byteBuf);

        // Отправка длины имени
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(4);
        byteBuf.writeInt(path.getFileName().toString().length());
        ctx.write(byteBuf);

        // Отправка имени
        byteBuf = ByteBufAllocator.DEFAULT.directBuffer(fileName.length);
        byteBuf.writeBytes(fileName);
        ctx.write(byteBuf);

        ctx.flush(); // вынесено отдельно, для того чтобы байты отправились 100% в правильном порядке
    }


    /**
     * Блок для работы с загрузкой файлов
     * todo описание
     */


    public enum DownloadState {
        NAME_LENGTH, NAME, FILE_SIZE, SENDING
    }

    private static DownloadState currentState = DownloadState.NAME_LENGTH;
    private static int nameLength;
    private static BufferedOutputStream out;
    private static long receivedFileLength;
    private static long fileSize;


    public static void downloadFile(ChannelHandlerContext ctx, ByteBuf buf) throws IOException {
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
                out = new BufferedOutputStream(new FileOutputStream(MainController.FILES_PATH + fileName));
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
                    ClientCommandHandler.fileSending = false;
                    Network.getInstance().getController().refreshLocalFilesList(); // todo
                    out.close();
                    receivedFileLength = 0; // !!!!!
                }
            }
        }
        buf.release();

    }
}
