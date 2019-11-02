package com.ec.server;

import com.ec.common.FileMessage;
import com.ec.common.FileRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;

public class MainHandler extends ChannelInboundHandlerAdapter {

    private final String FILES_PATH = "storage/server/";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            // Обработка запросов
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;

                // Команды для работы с файлами\списком
                if (fr.getFilename().startsWith("/")) {
                    if (fr.getFilename().contains("/getFilesList")) sendFilesListToClient(ctx);
                    if (fr.getFilename().contains("/delete")) delete(ctx, fr.getFilename());
                } else {
                    // Запрос на отправку файла клиенту
                    sendFileToClient(ctx, fr);
                }
            }

            // Обработка файлов
            if (msg instanceof FileMessage) {
                //что делать если прилетел фаил?
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get(FILES_PATH + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                sendFilesListToClient(ctx);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void delete(ChannelHandlerContext ctx, String filename) throws IOException {
        Files.delete(Paths.get(FILES_PATH + filename.substring(8)));
        sendFilesListToClient(ctx);
    }

    private void sendFilesListToClient(ChannelHandlerContext ctx) throws IOException {
        List<String> filesList_SERVER = new LinkedList<>();
        Files.list(Paths.get(FILES_PATH)).map(p -> p.getFileName().toString()).forEach(o -> filesList_SERVER.add(o));
        FileMessage fileMessage = new FileMessage(filesList_SERVER);
        ctx.writeAndFlush(fileMessage);
    }

    private void sendFileToClient(ChannelHandlerContext ctx, FileRequest fileRequest) throws IOException {
        // Проверяем файл на существование в каталоге на сервер
        if (Files.exists(Paths.get(FILES_PATH + fileRequest.getFilename()))) {
            FileMessage fm = new FileMessage(Paths.get(FILES_PATH + fileRequest.getFilename()));
            //отправляет посылку далее по конвееру, в обратную сторону (клиенту)
            ctx.writeAndFlush(fm);
        }
    }

    // ОБЯЗАТЕЛЬНЫЙ ОБРАБОТЧИК ИСКЛЮЧЕНИЙ!
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}