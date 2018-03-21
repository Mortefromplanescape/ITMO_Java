#!/usr/bin/env bash
function compile {
  javac -cp ./artifacts/$2.jar java/ru/ifmo/rain/rykunov/$1/*.java
}

function removeClassFiles {
  rm java/ru/ifmo/rain/rykunov/$1/*.class
}

function execute {
  java -cp ./java:./artifacts/$1.jar:./lib/* info.kgeorgiy.java.advanced.$2.Tester $3 ru.ifmo.rain.rykunov.$2.$4 $5
}

hw=$1
type=$2
salt=$3
case "$hw" in
  "1")
  compile "walk" "WalkTest"
  case "$type" in
    "easy")
    execute "WalkTest" "walk" "Walk" "RecursiveWalk" $salt
    ;;
    "hard")
    execute "WalkTest" "walk" "RecursiveWalk" "RecursiveWalk" $salt
    ;;
  esac
  removeClassFiles "walk"
  ;;
  "2")
  compile "arrayset" "ArraySetTest"
  case "$type" in
    "easy")
    execute "ArraySetTest" "arrayset" "SortedSet" "ArraySet" $salt
    ;;
    "hard")
    execute "ArraySetTest" "arrayset" "NavigableSet" "ArraySet" $salt
    ;;
  esac
  removeClassFiles "arrayset"
  ;;
  "3")
  compile "student" "StudentTest"
  case "$type" in
    "easy")
    execute "StudentTest" "student" "StudentQuery" "StudentDB" $salt
    ;;
  esac
  removeClassFiles "student"
  ;;
  "4")
  compile "implementor" "JarImplementorTest"
  case "$type" in
    "easy")
        execute "JarImplementorTest" "implementor" "interface" "Implementor" $salt
    ;;
    "hard")
        execute "JarImplementorTest" "implementor" "class" "Implementor" $salt
    ;;
    esac
  ;;
  "5")
  compile "implementor" "JarImplementorTest"
  case "$type" in
    "easy")
        execute "JarImplementorTest" "implementor" "jar-interface" "Implementor" $salt
    ;;
    "hard")
        execute "JarImplementorTest" "implementor" "jar-class" "Implementor" $salt
  ;;
  esac
  removeClassFiles "implementor"
  ;;
  esac

exit 0
