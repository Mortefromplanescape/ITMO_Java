#!/usr/bin/env bash
impG="java/info/kgeorgiy/java/advanced/implementor/"
javadoc -d javadoc -link https://docs.oracle.com/javase/8/docs/api/ -cp java/:artifacts/JarImplementorTest.jar:lib/* -private ru.ifmo.rain.rykunov.implementor ${impG}Impler.java ${impG}JarImpler.java ${impG}ImplerException.java