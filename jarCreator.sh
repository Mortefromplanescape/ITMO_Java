#!/usr/bin/env bash
out="out/production/ITMO_Java"
impG="info/kgeorgiy/java/advanced/implementor/"
rykunov="ru/ifmo/rain/rykunov/implementor/"
mkdir -p $out
javac -d $out -cp ./artifacts/JarImplementorTest.jar java/${rykunov}*.java
cd $out
jar xvf ../../../artifacts/JarImplementorTest.jar ${impG}Impler.class ${impG}JarImpler.class ${impG}ImplerException.class
jar cfe ../../../Implementor.jar ru.ifmo.rain.rykunov.implementor.Implementor ${rykunov}*.class ${impG}*.class