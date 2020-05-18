package ru.ifmo.rain.dolzhanskii.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static ru.ifmo.rain.dolzhanskii.hello.HelloUDPUtils.*;

public class HelloUDPNonblockingServer implements HelloServer {
    private ExecutorService listener;
    private ExecutorService responders;

    private Selector selector;
    private DatagramChannel channel;

    private int rxBufferSize;

    @Override
    public void start(final int port, final int threads) {
        try {
            selector = Selector.open();

            channel = configureChannel(new InetSocketAddress(port), true);
            rxBufferSize = channel.socket().getReceiveBufferSize();
            channel.register(selector, SelectionKey.OP_READ, new Context(threads));

            listener = Executors.newSingleThreadExecutor();
            responders = Executors.newFixedThreadPool(threads);

            listener.submit(this::listen);
        } catch (IOException e) {
            logError("Failed to init UDP connection", e);
        }
    }

    @Override
    public void close() {
        try {
            if (channel != null) {
                channel.close();
            }
            if (selector != null) {
                selector.close();
            }
            if (listener != null) {
                listener.shutdown();
                listener.awaitTermination(1, TimeUnit.SECONDS);
            }
            if (responders != null) {
                responders.shutdown();
                responders.awaitTermination(1, TimeUnit.SECONDS);
            }
        } catch (final InterruptedException | IOException e) {
            // Ignored
        }
    }

    private void listen() {
        while (!Thread.interrupted() && !selector.keys().isEmpty()) {
            try {
                selector.select();
            } catch (final IOException e) {
                logError("Failed to select socket", e);
                break;
            }
            for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext();) {
                final SelectionKey key = i.next();

                if (key.isReadable()) {
                    handleRead(key);
                }
                if (key.isWritable()) {
                    handleWrite(key);
                }
                i.remove();
            }
        }
    }

    private void handleRead(SelectionKey key) {
        final Context context = (Context) key.attachment();
        final ByteBuffer buffer = context.getBuffer();

        try {
            final SocketAddress destination = channel.receive(buffer);
            responders.submit(() -> processParcel(buffer, destination, context));
        } catch (final IOException e) {
            logError("IO exception occurred during read handling", e);
        }
    }

    private void handleWrite(SelectionKey key) {
        final Context context = (Context) key.attachment();
        final Context.Parcel parcel = context.getParcel();
        try {
            channel.send(parcel.buffer, parcel.destination);
            context.addBuffer(parcel.buffer.clear());
        } catch (final IOException e) {
            logError("IO exception occurred during write handling", e);
        }
    }

    private void processParcel(final ByteBuffer buffer, final SocketAddress destination, final Context context) {
        buffer.flip();
        final String request = StandardCharsets.UTF_8.decode(buffer).toString();
        final String response = buildServerResponse(request);
        buffer.clear();
        buffer.put(response.getBytes());
        buffer.flip();
        context.addParcel(buffer, destination);
    }

    private class Context {
        private final List<ByteBuffer> freeBuffers;
        private final List<Parcel> parcels;

        Context(final int threads) {
            this.freeBuffers = new ArrayList<>(threads);
            IntStream.range(0, threads).forEach(i -> this.freeBuffers.add(ByteBuffer.allocate(rxBufferSize)));
            this.parcels = new ArrayList<>();
        }

        synchronized void addBuffer(final ByteBuffer buffer) {
            if (freeBuffers.size() == 0) {
                channel.keyFor(selector).interestOpsOr(SelectionKey.OP_READ);
                selector.wakeup();
            }
            freeBuffers.add(buffer);
        }

        synchronized ByteBuffer getBuffer() {
            if (freeBuffers.size() == 1) {
                channel.keyFor(selector).interestOpsAnd(~SelectionKey.OP_READ);
                selector.wakeup();
            }
            return freeBuffers.remove(freeBuffers.size() - 1);
        }

        synchronized void addParcel(final ByteBuffer buffer, final SocketAddress destination) {
            if (parcels.size() == 0) {
                channel.keyFor(selector).interestOpsOr(SelectionKey.OP_WRITE);
                selector.wakeup();
            }
            parcels.add(new Parcel(buffer, destination));
        }

        synchronized Parcel getParcel() {
            if (parcels.size() == 1) {
                channel.keyFor(selector).interestOpsAnd(~SelectionKey.OP_WRITE);
                selector.wakeup();
            }
            return parcels.remove(parcels.size() - 1);
        }

        class Parcel {
            ByteBuffer buffer;
            SocketAddress destination;

            Parcel(ByteBuffer buffer, SocketAddress destination) {
                this.buffer = buffer;
                this.destination = destination;
            }
        }
    }

    public static void main(final String[] args) {
        serverMain(args, HelloUDPNonblockingServer.class);
    }
}
