package ru.ifmo.rain.rykunov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {

    private String getMessage(String prefix, int threadId, int requestId) {
        return String.format(
                "%s%d_%d",
                prefix,
                threadId,
                requestId);
    }

    private void send(final SocketAddress to, final String prefix, int requests, int threadId) {
        try (var socket = new DatagramSocket()) {
            socket.setSoTimeout(200);
            for (int requestId = 0; requestId < requests; requestId++) {
                final var requestMessage = getMessage(prefix, threadId, requestId);
                final var requestPacket = Packet.makePacket(
                        requestMessage.getBytes(StandardCharsets.UTF_8),
                        to);
                final var respondPacket = Packet.makePacket(socket.getReceiveBufferSize());
                while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                    try {
                        socket.send(requestPacket);
                        System.out.println(
                                String.format(
                                        "Sending request to %s: %s%n",
                                        to.toString(),
                                        requestMessage)
                        );
                        socket.receive(respondPacket);
                        final var respondMessage = Packet.toString(respondPacket);
                        if (isRespond(requestMessage, respondMessage)) {
                            System.out.println(String.format(
                                    "Message received: %s%n",
                                    respondMessage)
                            );
                            break;
                        }
                    } catch (IOException e) {
                        System.err.println(
                                String.format(
                                        "ERROR. Can't send request in thread %d: %s",
                                        threadId,
                                        e.getMessage())
                        );
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("ERROR. Can't create socket: " + e.getMessage());
        }
    }

    private boolean isRespond(final String requestMessage, final String respondMessage) {
        return requestMessage.length() != respondMessage.length() &&
                (respondMessage.contains(requestMessage + " ") || respondMessage.endsWith(requestMessage));
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        System.out.println(String.format(
                "[LOG]: %s, %d, %s, %d, %d",
                host,
                port,
                prefix,
                threads,
                requests)
        );
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.err.println("ERROR. Unknown host: " + host);
            return;
        }

        final var to = new InetSocketAddress(address, port);
        final var senders = Executors.newFixedThreadPool(threads);
        IntStream.range(0, threads)
                .forEach(
                        threadId -> senders.submit(
                                () -> send(to, prefix, requests, threadId)
                        ));
        senders.shutdown();
        try {
            senders.awaitTermination(threads * requests, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }
    }

    private static void printManualString() {
        System.err.println(String.format(
                "Running UDPClient%nHelloUDPClient <host> <port> <prefix> <threads count> <requests count>")
        );
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            printManualString();
            return;
        }

        for (var arg : args) {
            if (arg == null) {
                printManualString();
                return;
            }
        }

        String host = args[0];
        int port;
        String prefix = args[2];
        int threads;
        int requests;

        try {
            port = Integer.parseInt(args[1]);
            threads = Integer.parseInt(args[3]);
            requests = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            printManualString();
            return;
        }

        new HelloUDPClient().run(host, port, prefix, threads, requests);
    }
}
