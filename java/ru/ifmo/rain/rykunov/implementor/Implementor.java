package ru.ifmo.rain.rykunov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;


public class Implementor implements JarImpler {

    private static void inputError() {
        System.out.println("ERROR: required non null arguments: [-jar] className [*.jar]");
    }

    public static void main(String[] args) {
        if (args == null) {
            inputError();
            return;
        }

        JarImpler implementor = new Implementor();
        try {
            if (args.length == 3 && args[0] != null && args[1] != null && args[2] != null) {
                if (args[0].equals("-jar")) {
                    implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
                } else {
                    inputError();
                }
                return;
            }
            if (args.length == 1 && args[0] != null) {
                implementor.implement(Class.forName(args[0]), Paths.get("."));
            } else {
                inputError();
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Class: " + args[0] + "; not found");
        } catch (InvalidPathException e) {
            System.out.println("Invalid path(" + args[1] + ") to class: " + args[1]);
        } catch (ImplerException e) {
            System.out.println("Error while implementing interface: " + e.getMessage());
        }

    }

    private String getIndent(int count) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < count; i++) {
            indent.append("    ");
        }
        return indent.toString();
    }

    private Path getImplInterfacePath(Class<?> token, Path root) throws IOException {
        if (token.getPackage() != null) {
            root = root.resolve(token.getPackage().getName().replace(".", File.separator) + File.separator);
            Files.createDirectories(root);
        }
        return root.resolve(token.getSimpleName() + "Impl.java");
    }

    private Path getImplInterfaceJarPath(Class<?> token, Path root) throws IOException {
        String interfacePath = getImplInterfacePath(token, root).toString();
        if (interfacePath.endsWith(".java")) {
            return Paths.get(interfacePath.substring(0, interfacePath.length() - 5) + ".class");
        } else {
            throw new IllegalArgumentException("Token is not a java file");
        }
    }

    private String getPackageString(Class<?> token) {
        if (token.getPackage() != null) {
            return String.format("package %s;%n", token.getPackage().getName());
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
                "%s%n%s%s %s %s (%s) %s",
                Arrays.stream(method.getDeclaredAnnotations()).map(Annotation::toString).collect(Collectors.joining("%n")),
                getIndent(1),
                Modifier.toString(method.getModifiers() & ~(Modifier.ABSTRACT | Modifier.TRANSIENT)),
                method.getReturnType().getCanonicalName(),
                method.getName(),
                getArgumentsString(method),
                getExceptionsString(method)
        );
    }

    private String getReturnString(Method method) {
        return String.format("return %s;", getReturnDefaultTypeString(method));
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
        return String.format(
                "%s%s{%n%s%s%n%s}%n",
                getIndent(1),
                getMethodHeadString(method),
                getIndent(2),
                getReturnString(method),
                getIndent(1)
        );
    }

    private String getMethodsString(Class<?> token) {
        StringBuilder methods = new StringBuilder();
        for (Method method : token.getMethods()) {
            methods.append(getMethodString(method));
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
            out.write(String.format("%s %s {%n%s%n}%n",
                    getPackageString(token),
                    getHeadString(token, token.getSimpleName() + "Impl"),
                    getMethodsString(token)));
        } catch (IOException e) {
            throw new ImplerException("Error when writing to required path to Impl file");
        }
    }

    private void compileFiles(Path root, String file) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final List<String> args = new ArrayList<>();
        args.add(file);
        args.add("-cp");
        args.add(root + File.pathSeparator + System.getProperty("java.class.path"));
        compiler.run(null, null, null, args.toArray(new String[args.size()]));
    }

    private void jarWrite(Path jarFile, Path file) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            out.putNextEntry(new ZipEntry(file.normalize().toString()));
            Files.copy(file, out);
            out.closeEntry();
        }
    }

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        try {
            Path root = Paths.get(".");
            JarImpler implementor = new Implementor();
            implementor.implement(token, root);
            Path javaFilePath = getImplInterfacePath(token, root);
            Path classFilePath = getImplInterfaceJarPath(token, root);
            compileFiles(root, javaFilePath.toString());
            jarWrite(jarFile, classFilePath);
            classFilePath.toFile().deleteOnExit();
        } catch (IOException e) {
            System.out.print("ERROR: can't create jar file, because some error with files.");
        }
    }
}
