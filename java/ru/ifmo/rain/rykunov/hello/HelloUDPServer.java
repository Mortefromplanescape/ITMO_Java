package ru.ifmo.rain.rykunov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer {

    private DatagramSocket socket;
    private int requestBufferSize;
    private ExecutorService listener;
    private ExecutorService requestsSender;
    private static int QUEUE_SIZE = 10_000;
    private boolean closed = true;

    private void openSocket(int port) throws SocketException {
        socket = new DatagramSocket(port);
        requestBufferSize = socket.getReceiveBufferSize();
    }

    private void sendResponse(DatagramPacket packet) {
        final var requestMessage = Packet.toString(packet);
        final var responseMessage = "Hello, " + requestMessage;
        final var responsePacket = Packet.makePacket(
                responseMessage.getBytes(StandardCharsets.UTF_8),
                packet.getSocketAddress());

        try {
            socket.send(responsePacket);
        } catch (IOException e) {
            System.err.println(
                    String.format(
                            "ERROR. Can't send packet to %s%nLog:%s",
                            packet.getSocketAddress(),
                            e.getMessage())
            );
        }
    }

    private void listen() {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            final var packet = Packet.makePacket(requestBufferSize);

            try {
                socket.receive(packet);
                requestsSender.submit(() -> sendResponse(packet));
            } catch (IOException e) {
                if (!closed) {
                    System.err.println("ERROR. Can't receive packet" + e.getMessage());
                }
            }
        }
    }

    @Override
    public void start(int port, int threads) {
        try {
            openSocket(port);
        } catch (SocketException e) {
            System.err.println("ERROR. Failed to create socket at port " + port);
            return;
        }

        listener = Executors.newSingleThreadExecutor();
        requestsSender = new ThreadPoolExecutor(
                threads,
                threads,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(QUEUE_SIZE),
                new ThreadPoolExecutor.DiscardPolicy());
        closed = false;
        listener.submit(this::listen);
    }

    @Override
    public void close() {
        closed = true;
        listener.shutdownNow();
        requestsSender.shutdownNow();
        try {
            requestsSender.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        socket.close();
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println(String.format(
                    "Running UDPServer%nHelloUDPServer <port> <threads count>")
            );
            return;
        }

        int port;
        int threads;

        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("ERROR. Can't parse number " + e.getMessage());
            return;
        }

        new HelloUDPServer().start(port, threads);
    }
}