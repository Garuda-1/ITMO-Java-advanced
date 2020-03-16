#!/usr/bin/env bash

#cd ../../../../../../
#
#ROOT=${PWD}
#
#MODULE_NAME=ru.ifmo.rain.dolzhanskii.implementor
#
#REQ_PATH=${ROOT}/lib:${ROOT}/artifacts
#
#SRC_NAME=info.kgeorgiy.java.advanced.implementor

cd ../../../../../../../

ROOT=${PWD}

BASIC_PATH=${ROOT}/java-advanced-2020
SOLUTION_PATH=${ROOT}/java-advanced-2020-solutions

MODULE_NAME=ru.ifmo.rain.dolzhanskii.implementor

SRC_NAME=info.kgeorgiy.java.advanced.implementor
SRC_PATH=info/kgeorgiy/java/advanced/implementor

OUT_PATH=${SOLUTION_PATH}/_build/production/${MODULE_NAME}
REQ_PATH=${BASIC_PATH}/lib:${BASIC_PATH}/artifacts
AUX_PATH=${BASIC_PATH}/modules/${SRC_NAME}/${SRC_PATH}

javadoc -d ${SOLUTION_PATH}/_javadoc -link https://docs.oracle.com/en/java/javase/11/docs/api/ -private -version -author \
    --module-path=${REQ_PATH} --module-source-path=${SOLUTION_PATH}/java-solutions:${BASIC_PATH}/modules \
    --module=${MODULE_NAME} \
    ${AUX_PATH}/Impler.java ${AUX_PATH}/JarImpler.java ${AUX_PATH}/ImplerException.java
