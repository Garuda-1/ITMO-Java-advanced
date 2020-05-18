package ru.ifmo.rain.dolzhanskii.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static ru.ifmo.rain.dolzhanskii.hello.HelloUDPUtils.*;

public class HelloUDPNonblockingClient implements HelloClient {
    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final Selector selector;
        try {
            selector = Selector.open();
            for (int i = 0; i < threads; i++) {
                final DatagramChannel channel = configureChannel(
                        new InetSocketAddress(InetAddress.getByName(host), port), false);
                final int rxBufferSize = channel.socket().getReceiveBufferSize();
                channel.register(selector, SelectionKey.OP_WRITE,
                        new Context(i, ByteBuffer.allocate(rxBufferSize)));
            }
        } catch (final IOException e) {
            logError("Failed to init channels", e);
            return;
        }

        while (!Thread.interrupted() && !selector.keys().isEmpty()) {
            try {
                selector.select(SOCKET_TIMEOUT_MS);

            } catch (final IOException e) {
                e.printStackTrace();
                break;
            }

            if (selector.selectedKeys().isEmpty()) {
                for (SelectionKey key : selector.keys()) {
                    if (key.isWritable()) {
                        handleWrite(key, prefix);
                    }
                }
                continue;
            }

            for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext();) {
                final SelectionKey key = i.next();
                if (key.isWritable()) {
                    handleWrite(key, prefix);
                }
                if (key.isReadable()) {
                    handleRead(key, requests);
                }
                i.remove();
            }
        }
    }

    private void handleWrite(final SelectionKey key, final String prefix) {
        final Context context = (Context) key.attachment();
        final ByteBuffer buffer = context.getBuffer();
        final DatagramChannel channel = (DatagramChannel) key.channel();

        try {
            final SocketAddress hostSocketAddress = channel.getRemoteAddress();
            final String request = buildClientRequest(prefix, context.channelId, context.requestId);
            logInfo(context.channelId, String.format("Sending message: '%s'", request));
            buffer.clear();
            buffer.put(request.getBytes());
            buffer.flip();

            try {
                channel.send(buffer, hostSocketAddress);
                buffer.flip();

                key.interestOpsOr(SelectionKey.OP_READ);
                key.interestOpsAnd(~SelectionKey.OP_WRITE);
            } catch (final ClosedChannelException e) {
                logError("Channel is already closed", e);
            }
        } catch (final IOException e) {
            logError("IO exception occurred during write handling", e);
        }
    }

    private void handleRead(final SelectionKey key, final int requests) {
        final Context context = (Context) key.attachment();
        final ByteBuffer buffer = context.getBuffer();
        final DatagramChannel channel = (DatagramChannel) key.channel();

        try {
            buffer.clear();
            channel.receive(buffer);
            buffer.flip();
            String response = StandardCharsets.UTF_8.decode(buffer).toString();

            if (HelloUDPUtils.validate(response, context.channelId, context.requestId)) {
                context.incrementRequestId();
            }
        } catch (final IOException e) {
            logError("IO exception occurred during read handling", e);
        }
        if (context.requestId != requests) {
            key.interestOpsAnd(~SelectionKey.OP_READ);
            key.interestOpsOr(SelectionKey.OP_WRITE);
        } else {
            try {
                key.channel().close();
            } catch (final IOException e) {
                // Ignored
            }
        }
    }

    private class Context {
        private final int channelId;
        private int requestId;
        private final ByteBuffer buffer;

        Context(final int channelId, final ByteBuffer buffer) {
            this.channelId = channelId;
            this.requestId = 0;
            this.buffer = buffer;
        }

        void incrementRequestId() {
            this.requestId++;
        }

        ByteBuffer getBuffer() {
            return buffer;
        }
    }

    public static void main(final String[] args) {
        clientMain(args, HelloUDPNonblockingClient.class);
    }
}
