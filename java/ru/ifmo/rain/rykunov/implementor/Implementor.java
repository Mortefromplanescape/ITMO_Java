package ru.ifmo.rain.rykunov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Implementor implements Impler {

    public static void main(String[] args) {
        if (args == null || args[0] == null || args[1] == null) {
            System.out.println("ERROR: required non null arguments: first is interface name, second is path to interface"); // TODO
            return;
        }

        Impler implementor = new Implementor();
        try {
            implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (ClassNotFoundException e) {
            System.out.println("Class: " + args[0] + "; not found");
        } catch (InvalidPathException e) {
            System.out.println("Invalid path(" + args[1] + ") to class: " + args[1]);
        } catch (ImplerException e) {
            System.out.println("Error while implementing class: " + e.getMessage());
        }


    }

    private Path getImplInterfacePath(Class<?> token, Path root) throws IOException {
        if (token.getPackage() != null) {
            root = root.resolve(token.getPackage().getName().replace('.', '/') + "/");
            Files.createDirectories(root);
        }
        return root.resolve(token.getSimpleName() + "Impl.java");
    }

    private String getPackageString(Class<?> token) {
        if (token.getPackage() != null) {
            return String.format("package %s; %n", token.getPackage().getName());
        }
        return "";
    }

    private String getHeadString(Class<?> token, String className) {
        return String.format("class %s implements %s", className, token.getSimpleName());
    }

    private String getArgumentsString(Method method) {
        Parameter[] args = method.getParameters();
        return Arrays.stream(args)
                .map(parameter -> parameter.getType().getCanonicalName() + " " + parameter.getName())
                .collect(Collectors.joining(", "));
    }

    private String getExceptionsString(Method method) {
        Class<?> exceptions[] = method.getExceptionTypes();
        StringBuilder exceptionsBuilder = new StringBuilder();
        if (exceptions.length > 0) {
            exceptionsBuilder.append("throws ");
            exceptionsBuilder.append(Arrays.stream(exceptions)
                    .map(Class::getCanonicalName)
                    .collect(Collectors.joining(", ")));
        }
        return exceptionsBuilder.toString();
    }

    private String getMethodHeadString(Method method) {
        return String.format(
                "public %s %s ( %s ) %s",
                method.getReturnType().getCanonicalName(),
                method.getName(),
                getArgumentsString(method),
                getExceptionsString(method)
        );
    }

    private String getReturnString(Method method) {
        return String.format("return %s;%n", getReturnDefaultTypeString(method));
    }

    private String getReturnDefaultTypeString(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(boolean.class)) {
            return "true";
        } else if (returnType.equals(void.class)) {
            return "";
        } else {
            return returnType.isPrimitive() ? "0" : "null";
        }
    }

    private String getMethodString(Method method) {
        return String.format("%s{ %n %s }%n", getMethodHeadString(method), getReturnString(method));
    }

    private String getMethodsString(Class<?> token) {
        StringBuilder methods = new StringBuilder();
        while (token != null) {
            for (Method method : token.getDeclaredMethods()) {
                methods.append(getMethodString(method));
            }
            token = token.getSuperclass();
        }
        return methods.toString();
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Required non null arguments: interface name and path to interface");
        }

        if (!token.isInterface()) {
            throw new ImplerException("Required interface as first argument");
        }

        try (Writer out = Files.newBufferedWriter(getImplInterfacePath(token, root))) {
            out.write(String.format("%s %s{%n %s %n}%n",
                    getPackageString(token),
                    getHeadString(token, token.getSimpleName() + "Impl"),
                    getMethodsString(token)));
        } catch (IOException e) {
            throw new ImplerException("Error when writing to required path to Impl file");
        }
    }
}
