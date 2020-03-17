#!/usr/bin/env bash

cd ../../../../../../../

ROOT=${PWD}

BASIC_PATH=${ROOT}/java-advanced-2020
SOLUTION_PATH=${ROOT}/java-advanced-2020-solutions

MODULE_NAME=ru.ifmo.rain.dolzhanskii.implementor

LIB_PATH=${BASIC_PATH}/lib:${BASIC_PATH}/artifacts:${SOLUTION_PATH}

java --module-path ${LIB_PATH} -m ${MODULE_NAME} ${@:1}