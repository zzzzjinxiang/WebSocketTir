package com;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

public class ServerBind {
    public static void main(String[] args) {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(8080));
            serverSocketChannel.configureBlocking(false);
            //设置通道为非阻塞才可向Selector中注册
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            Handler handler = new Handler(1024);

            while (true) {
                if (selector.select(3000) == 0) {
                    System.out.println("outline");
                    continue;
                }
                System.out.println("handler request");
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey selectionKey = keyIterator.next();
                    try {
                        if (selectionKey.isAcceptable()) {
                            handler.handleAccept(selectionKey);
                        }
                        if (selectionKey.isReadable())
                            handler.handleRead(selectionKey);
                    } catch (IOException e) {
                        keyIterator.remove();
                        continue;
                    }
                }
                keyIterator.remove();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static class Handler {
        private int bufferSize = 1024;
        private String localCharset = "UTF-8";

        public Handler() {
        }

        public Handler(int bufferSize) {
            this(bufferSize, null);
        }

        public Handler(String localCharset) {
            this(-1, localCharset);
        }

        public Handler(int bufferSize, String localCharset) {
            if (bufferSize > 0) {
                this.bufferSize = bufferSize;
            }
            if (localCharset != null) {
                this.localCharset = localCharset;
            }
        }

        public void handleAccept(SelectionKey selectionKey) throws IOException {
            //通过选择器键获取服务器套接字通道，通过accept()方法获取套接字通道连接
            SocketChannel socketChannel = ((ServerSocketChannel) selectionKey.channel()).accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selectionKey.selector(), selectionKey.OP_READ, ByteBuffer.allocate(bufferSize));
        }

        public void handleRead(SelectionKey selectionKey) throws IOException {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            ByteBuffer byteBuffer = (ByteBuffer) selectionKey.attachment();
            byteBuffer.clear();
            if (socketChannel.read(byteBuffer) == -1)
                System.out.println("error");
            else {
                byteBuffer.flip();
                String receivedRequestData = Charset.forName(localCharset).newDecoder().decode(byteBuffer).toString();
                System.out.println("server msg:" + receivedRequestData);
                String responseMsg = "client response msg";
                byteBuffer = ByteBuffer.wrap(responseMsg.getBytes(localCharset));
                socketChannel.write(byteBuffer);
                socketChannel.close();
            }
        }
    }
}