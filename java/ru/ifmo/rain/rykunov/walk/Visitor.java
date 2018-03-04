package ru.ifmo.rain.rykunov.walk;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

public class Visitor extends SimpleFileVisitor<Path> {
    private BufferedWriter writer;

    public Visitor(BufferedWriter out) {
        writer = out;
    }

    private FileVisitResult write(int hash, String filePath) throws IOException {
        writer.write(String.format("%08x", hash) + " " + filePath);
        writer.newLine();
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
        int h = 0x811c9dc5;
        try (BufferedInputStream reader = new BufferedInputStream(Files.newInputStream(path))) {
            byte[] bytes = new byte[1024];
            int cnt;
            while ((cnt = reader.read(bytes)) != -1) {
                for (int i = 0; i < cnt; i++) {
                    h = (h * 0x01000193) ^ (bytes[i] & 0xff);
                }
            }
        } catch (IOException e) {
            return write(0, path.toString());
        }
        return write(h, path.toString());
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
        return write(0, path.toString());
    }

}
