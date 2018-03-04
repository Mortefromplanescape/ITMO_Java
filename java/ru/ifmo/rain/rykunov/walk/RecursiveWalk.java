package ru.ifmo.rain.rykunov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {

    private static void walk(String path, BufferedWriter writer) throws IOException{
        try {
            Path filePath = Paths.get(path);
            try {
                Files.walkFileTree(filePath, new Visitor(writer));
            } catch (IOException e) {
                writer.write(String.format("%08x", 0) + " " + filePath);
            }
        } catch (InvalidPathException e) {
            writer.write(String.format("%08x", 0) + " " + path);
        }
    }

    private static void parseInputFile(Path inputFile, Path outputFile) {
        try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
            try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        try {
                            walk(line, writer);
                        } catch (IOException e) {
                            System.out.println("ERROR: something went wrong when was writing to output file");
                        }
                    }
                } catch (IOException e) {
                    System.out.println("ERROR: something went wrong when was reading from input file");
                }
            } catch (IOException e) {
                System.out.println("ERROR: couldn't open output file: " + outputFile);
            }
        } catch (IOException e) {
            System.out.println("ERROR: couldn't open input file: " + inputFile);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("ERROR: 2 elements(not null) required (input and output files)");
            return;
        }
        Path inputFile;
        Path outputFile;

        try {
            inputFile = Paths.get(args[0]);
        } catch (InvalidPathException e) {
            System.out.println("ERROR: invalid path: " + args[0] + " for input file(first argument)");
            return;
        }

        try {
            outputFile = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            System.out.println("ERROR: invalid path: " + args[1] + " for output file(second argument)");
            return;
        }

        parseInputFile(inputFile, outputFile);
    }
}
