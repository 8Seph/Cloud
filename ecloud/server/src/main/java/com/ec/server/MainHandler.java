package com.ec.server;

import com.ec.common.FileMessage;
import com.ec.common.FileRequest;
import com.ec.common.FileText;
import com.ec.common.FilesList;
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

    private final String FILES_PATH = "D:/storage/";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            // Обработка запросов на получение / удаление файлов
            if (msg instanceof FileRequest) {
                FileRequest fr = (FileRequest) msg;
                if (fr.getFilename().startsWith("/")) {
                    if (fr.getFilename().contains("/delete")) delete(ctx, fr.getFilename());
                } else {
                    sendFileToClient(ctx, fr);
                }
            }

            if (msg instanceof FilesList) {
                sendFilesListToClient(ctx);
            }

            // Обработка файлов
            if (msg instanceof FileMessage) {
                FileMessage fm = (FileMessage) msg;
                Files.write(Paths.get(FILES_PATH + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                sendFilesListToClient(ctx);
            }

            //tmp text
            if (msg instanceof FileText) {
                FileText fileText = (FileText) msg;
                System.out.println(fileText);
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
        FilesList filesList = new FilesList(filesList_SERVER);
        ctx.writeAndFlush(filesList);
    }

    private void sendFileToClient(ChannelHandlerContext ctx, FileRequest fileRequest) throws IOException {
        if (Files.exists(Paths.get(FILES_PATH + fileRequest.getFilename()))) {
            FileMessage fm = new FileMessage(Paths.get(FILES_PATH + fileRequest.getFilename()));
            ctx.writeAndFlush(fm);
        }
    }

    // обработка исключений
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}