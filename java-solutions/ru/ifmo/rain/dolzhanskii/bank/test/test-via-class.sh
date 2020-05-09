#!/usr/bin/env bash

BASE_DIR=${PWD}

cd ../../../../../../../../

ROOT=$PWD

SOLUTION_PATH=${ROOT}/java-advanced-2020-solutions

PACKAGE_NAME=ru.ifmo.rain.dolzhanskii.bank
PACKAGE_PATH=ru/ifmo/rain/dolzhanskii/bank

OUT_PATH=${SOLUTION_PATH}/_build/production/${PACKAGE_NAME}
LIB_PATH=${SOLUTION_PATH}/lib:${ROOT}/java-advanced-2020/lib
SRC_PATH=${SOLUTION_PATH}/java-solutions/${PACKAGE_PATH}

rm -rf ${OUT_PATH}

javac -cp . -p . --module-path ${LIB_PATH} \
    --add-modules junit \
    --add-modules org.junit.jupiter.api \
    --add-modules org.junit.platform.commons \
    --add-modules org.junit.platform.launcher \
    ${SRC_PATH}/*/*.java -d ${OUT_PATH}

cd ${OUT_PATH}

java -cp . -p . --module-path ${LIB_PATH} \
    --add-modules junit \
    --add-modules org.junit.jupiter.api \
    --add-modules org.junit.platform.commons \
    --add-modules org.junit.platform.launcher \
    ru.ifmo.rain.dolzhanskii.bank.test.BankTests

RET_CODE=${?}

cd ${BASE_DIR}

if [[ ${RET_CODE} == "0" ]]
then
    exit 0
else
    exit 1
fi