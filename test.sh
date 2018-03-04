#!/usr/bin/env bash
function compile {
  javac java/ru/ifmo/rain/rykunov/$1/*.java
}

function removeClassFiles {
  rm java/ru/ifmo/rain/rykunov/$1/*.class
}

hw=$1
type=$2
salt=$3
case "$hw" in
  "1")
  compile "walk"
  case "$type" in
    "easy")
    java -cp ./java:./artifacts/WalkTest.jar:./lib/* info.kgeorgiy.java.advanced.walk.Tester Walk ru.ifmo.rain.rykunov.walk.RecursiveWalk $salt
    ;;
    "hard")
    java -cp ./java:./artifacts/WalkTest.jar:./lib/* info.kgeorgiy.java.advanced.walk.Tester RecursiveWalk ru.ifmo.rain.rykunov.walk.RecursiveWalk $salt
    ;;
  esac
  removeClassFiles "walk"
  ;;
  "2")
  compile "arraySet"
  case "$type" in
    "easy")
    java -cp ./java:./artifacts/ArraySetTest.jar:./lib/* info.kgeorgiy.java.advanced.arrayset.Tester SortedSet ru.ifmo.rain.rykunov.arraySet.ArraySet $salt
    ;;
    "hard")
    java -cp ./java:./artifacts/ArraySetTest.jar:./lib/* info.kgeorgiy.java.advanced.arrayset.Tester NavigableSet ru.ifmo.rain.rykunov.arraySet.ArraySet $salt
    ;;
  esac
  removeClassFiles "arraySet"
  ;;
esac

exit 0
