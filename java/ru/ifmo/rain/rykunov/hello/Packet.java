package ru.ifmo.rain.rykunov.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

class Packet {
    static DatagramPacket makePacket(final byte[] buffer, final SocketAddress to) {
        return new DatagramPacket(buffer, 0, buffer.length, to);
    }

    static DatagramPacket makePacket(int bufferSize) {
        final var buffer = new byte[bufferSize];
        return new DatagramPacket(buffer, bufferSize);
    }

    static String toString(DatagramPacket packet) {
        return new String(
                packet.getData(),
                packet.getOffset(),
                packet.getLength(),
                StandardCharsets.UTF_8);
    }
}
