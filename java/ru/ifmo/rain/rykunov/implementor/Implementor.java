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

/**
 * The class implements {@link JarImpler} interface.
 */
public class Implementor implements JarImpler {
    /**
     * Prints error from main function.
     */
    private static void inputError() {
        System.out.println("ERROR: required non null arguments: [-jar] className [*.jar]");
    }

    /**
     * This function is used to choose which way of implementation to execute.
     * Runs {@link Implementor} in two possible ways:
     * <ul>
     * <li> 2 arguments: <tt>className rootPath</tt> - runs {@link #implement(Class, Path)} with given arguments</li>
     * <li> 3 arguments: <tt>-jar className jarPath</tt> - runs {@link #implementJar(Class, Path)} with two second arguments</li>
     * </ul>
     * If arguments are incorrect or an error occurs during implementation returns message with information about error
     *
     * @param args arguments for running an application
     */
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
                implementor.implement(Class.forName(args[0]), Paths.get("./implemented"));
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

    /**
     * Generates count of 4spaces tabs.
     *
     * @param count count of 4spaces tabs needed
     * @return {@link String} includes count of 4spaces tabs
     */
    private String getIndent(int count) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < count; i++) {
            indent.append("    ");
        }
        return indent.toString();
    }

    /**
     * Gets {@link Path} to lowest directory from <tt>root</tt> to <tt>token</tt>'s package
     *
     * @param token     {@link Class} which package is used to get directory names
     * @param root      {@link Path} uses to start from this point
     * @param createDir <tt>true</tt> if creates directories, <tt>false</tt> else
     * @return {@link Path} to lowest directory from <tt>root</tt> to <tt>token</tt>'s package
     * @throws IOException if couldn't create directories or resolve path to <tt>token</tt>'s package
     */
    private Path getImplInterfaceDirectoryPath(Class<?> token, Path root, boolean createDir) throws IOException {
        Path directoryPath = root;
        if (token.getPackage() != null) {
            directoryPath = root.resolve(token.getPackage().getName().replace(".", File.separator) + File.separator);
            if (createDir) {
                Files.createDirectories(directoryPath);
            }
        }
        return directoryPath;
    }

    /**
     * Generates {@link Path} to realization of <tt>token</tt>.
     *
     * @param token     {@link Class} needed to be realized
     * @param root      {@link Path} to root of realization of interface
     * @param createDir <tt>true</tt> if creates directories, <tt>false</tt> else
     * @param suffix    suffix of file to create
     * @return {@link Path} to realization of interface
     * @throws IOException if couldn't create directories to realization of interface or get {@link Path} to it
     */
    private Path getImplInterfacePath(Class<?> token, Path root, boolean createDir, String suffix) throws IOException {
        root = getImplInterfaceDirectoryPath(token, root, createDir);
        return root.resolve(token.getSimpleName() + suffix);
    }

    /**
     * Generates {@link String} with package of <tt>token</tt>.
     *
     * @param token {@link Class} which package outputs.
     * @return {@link String} with format: "package [<tt>package of token</tt>];" with end of line.
     */
    private String getPackageString(Class<?> token) {
        if (token.getPackage() != null) {
            return String.format("package %s;%n", token.getPackage().getName());
        }
        return "";
    }

    /**
     * Generates {@link String} with head of class implementation of <tt>token</tt>.
     *
     * @param token     implemented {@link Class}
     * @param className class name of implementation
     * @return {@link String} with format: "class [<tt>className</tt>] implements [<tt>token</tt> name]" with end of line.
     */
    private String getHeadString(Class<?> token, String className) {
        return String.format("class %s implements %s", className, token.getSimpleName());
    }

    /**
     * Generates {@link String} with all arguments of <tt>method</tt>.
     *
     * @param method {@link Method} which methods generates
     * @return {@link String} with format: "[[argument type] [argument name] separated by ',']"
     */
    private String getArgumentsString(Method method) {
        Parameter[] args = method.getParameters();
        return Arrays.stream(args)
                .map(parameter -> parameter.getType().getCanonicalName() + " " + parameter.getName())
                .collect(Collectors.joining(", "));
    }

    /**
     * Generates {@link String} with exceptions of <tt>token</tt>.
     *
     * @param method {@link Method} which exceptions generates
     * @return {@link String} with format: "throws [exceptions separated by ',']"
     */
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

    /**
     * Generates {@link String} with head of realization of <tt>method</tt>.
     *
     * @param method {@link Method} which head generates
     * @return {@link String} with head of realization of <tt>method</tt>
     */
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

    /**
     * Generates <tt>return</tt> statement for realization of <tt>method</tt>.
     *
     * @param method {@link Method} which <tt>return</tt> generates
     * @return {@link String} String with format "return [<tt>return</tt> type of <tt>method</tt>]"
     */
    private String getReturnString(Method method) {
        return String.format("return %s;", getReturnDefaultTypeString(method));
    }

    /**
     * Generates <tt>return</tt> type of <tt>method</tt>.
     *
     * @param method {@link Method} which return type generates
     * @return {@link String} with <tt>return</tt> type of <tt>method</tt>
     */
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

    /**
     * Generates method of implemented interface.
     *
     * @param method {@link Method} needs to be generated
     * @return {@link String} includes realization of <tt>method</tt>.
     */
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

    /**
     * Generates all methods of implemented interface.
     *
     * @param token {@link Class} whose methods implements
     * @return {@link String} includes realizations of all methods needs to be implemented
     */
    private String getMethodsString(Class<?> token) {
        StringBuilder methods = new StringBuilder();
        for (Method method : token.getMethods()) {
            methods.append(getMethodString(method));
        }
        return methods.toString();
    }

    /**
     * Generate unicode-safety char
     *
     * @param ch char needs to get unicode-safety
     * @return unicode-safety char
     */
    private String getNormalizeUnicodeChar(char ch) {
        return ch < 127 ? Character.toString(ch) : String.format("\\u%04X", (int) ch);
    }

    /**
     * Prints unicode safety <tt>content</tt> to file
     *
     * @param content {@link String} needs to get unicode-safety
     * @param out     file where prints
     * @throws IOException if couldn't write to <tt>out</tt>
     */
    private void unicodeEscapingOutput(String content, Writer out) throws IOException {
        for (char ch : content.toCharArray()) {
            out.write(getNormalizeUnicodeChar(ch));
        }
    }

    /**
     * @throws ImplerException if the given class cannot be generated for one of such reasons:
     *                         <ul>
     *                         <li> Some arguments are <tt>null</tt></li>
     *                         <li> Given <tt>class</tt> is not interface. </li>
     *                         <li> The process is not allowed to create files or directories. </li>
     *                         <li> The problems with I/O occurred during implementation. </li>
     *                         </ul>
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Required non null arguments: interface name and path to interface");
        }

        if (!token.isInterface()) {
            throw new ImplerException("Required interface as first argument");
        }

        try (Writer out = Files.newBufferedWriter(getImplInterfacePath(token, root, true, "Impl.java"))) {
            unicodeEscapingOutput(
                    String.format("%s %s {%n%s%n}%n",
                            getPackageString(token),
                            getHeadString(token, token.getSimpleName() + "Impl"),
                            getMethodsString(token)
                    ), out);
        } catch (IOException e) {
            throw new ImplerException("Error when writing to required path to Impl file");
        }
    }

    /**
     * Compiles <tt>.java</tt> <tt>file</tt>.
     *
     * @param root {@link Path} to root of <tt>file</tt>
     * @param file {@link Path} to file needs to compile from root
     */
    private void compileFiles(Path root, String file) {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final List<String> args = new ArrayList<>();
        args.add(file);
        args.add("-cp");
        args.add(root + File.pathSeparator + System.getProperty("java.class.path"));
        args.add("-encoding");
        args.add("UTF-8");
        compiler.run(null, null, null, args.toArray(new String[args.size()]));
    }

    /**
     * Writes <tt>file</tt> to <tt>.jar</tt> file <tt>jarFile</tt>.
     *
     * @param jarFile       {@link Path} to <tt>.jar</tt> file where writes <tt>file</tt>
     * @param tempFile      {@link Path} to file needs to be written to <tt>jarFile</tt>
     * @param filePathInJar {@link Path} to file in <tt>jarFile</tt>
     * @throws IOException if can't write to <tt>jarFile</tt>
     */
    private void jarWrite(Path jarFile, Path tempFile, Path filePathInJar) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            out.putNextEntry(new ZipEntry(filePathInJar.normalize().toString()));
            Files.copy(tempFile, out);
            out.closeEntry();
        }
    }

    /**
     * Produces <tt>.jar</tt> file implementing class or interface specified by provided <tt>token</tt>.
     * <p>
     * Generated class full name should be same as full name of the type token with <tt>Impl</tt> suffix
     * added.
     * <p>
     * During implementation creates temporary folder to store temporary <tt>.java</tt> and <tt>.class</tt> files.
     *
     * @throws ImplerException if the given class cannot be generated for one of such reasons:
     *                         <ul>
     *                         <li> Some arguments are <tt>null</tt></li>
     *                         <li> Error occurs during implementation via {@link #implement(Class, Path)} </li>
     *                         <li> The process is not allowed to create files or directories. </li>
     *                         <li> {@link JavaCompiler} failed to compile implemented class </li>
     *                         <li> The problems with I/O occurred during implementation. </li>
     *                         </ul>
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        try {
            Path rootTemp = Files.createTempDirectory(".");
            JarImpler implementor = new Implementor();
            implementor.implement(token, rootTemp);

            Path javaTempFilePath = getImplInterfacePath(token, rootTemp, true, "Impl.java");
            compileFiles(rootTemp, javaTempFilePath.toString());

            Path classTempFilePath = getImplInterfacePath(token, rootTemp, false, "Impl.class");
            Path classFilePathInJar = getImplInterfacePath(token, Paths.get(""), false, "Impl.class");
            jarWrite(jarFile, classTempFilePath, classFilePathInJar);


        } catch (IOException e) {
            System.out.print("ERROR: can't create jar file, because some error with files.");
        }
    }
}
