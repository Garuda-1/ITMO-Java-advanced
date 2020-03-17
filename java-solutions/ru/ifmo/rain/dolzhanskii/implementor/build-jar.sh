#!/usr/bin/env bash

cd ../../../../../../../

ROOT=${PWD}

BASIC_PATH=${ROOT}/java-advanced-2020
SOLUTION_PATH=${ROOT}/java-advanced-2020-solutions

MODULE_NAME=ru.ifmo.rain.dolzhanskii.implementor
MODULE_PATH=ru/ifmo/rain/dolzhanskii/implementor

OUT_PATH=${SOLUTION_PATH}/_build/production/${MODULE_NAME}

REQ_PATH=${BASIC_PATH}/lib:${BASIC_PATH}/artifacts
SRC_PATH=${SOLUTION_PATH}/modules/${MODULE_NAME}
JAR_PATH=${SOLUTION_PATH}

rm -rf ${OUT_PATH}

javac --module-path ${REQ_PATH} ${SRC_PATH}/module-info.java ${SRC_PATH}/${MODULE_PATH}/*.java -d ${OUT_PATH}

cd ${OUT_PATH}

mkdir ${JAR_PATH} 2> /dev/null

jar -c --file=${JAR_PATH}/_implementor.jar --main-class=${MODULE_NAME}.JarImplementor --module-path=${REQ_PATH} \
    module-info.class ${MODULE_PATH}/*
