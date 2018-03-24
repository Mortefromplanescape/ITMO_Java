#!/usr/bin/env bash
function compile {
  javac -cp ./artifacts/$2.jar java/ru/ifmo/rain/rykunov/$1/*.java
}

function removeClassFiles {
  rm java/ru/ifmo/rain/rykunov/$1/*.class
}

function execute {
  java -cp ./java/:./artifacts/$1.jar:./lib/* info.kgeorgiy.java.advanced.$2.Tester $3 ru.ifmo.rain.rykunov.$2.$4 $5
}

function test {
  compile $1 $2
  case "$3" in
    "easy")
    execute $2 $1 $4 $6 $salt
    ;;
    "hard")
    execute $2 $1 $5 $6 $salt
    ;;
  esac
  removeClassFiles $1
}

hw=$1
type=$2
salt=$3
case "$hw" in
  "1")
  test "walk" "WalkTest" $2 "Walk" "RecursiveWalk" "RecursiveWalk"
  ;;
  "2")
  test "arrayset" "ArraySetTest" $2 "SortedSet" "NavigableSet" "ArraySet"
  ;;
  "3")
  test "student" "StudentTest" $2 "StudentQuery" "StudentGroupQuery" "StudentDB"
  ;;
  "4")
  test "implementor" "JarImplementorTest" $2 "interface" "class" "Implementor"
  ;;
  "5")
  test "implementor" "JarImplementorTest" $2 "jar-interface" "jar-class" "Implementor"
  ;;
  "6")
  echo "Use JDCreator.sh to generate JD"
  ;;
  esac

exit 0
