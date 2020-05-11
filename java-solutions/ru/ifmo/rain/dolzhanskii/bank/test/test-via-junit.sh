#!/usr/bin/env bash

BASE_DIR=${PWD}

cd ../../../../../../../../

#echo ${PWD}

ROOT=$PWD

SOLUTION_PATH=${ROOT}/java-advanced-2020-solutions

PACKAGE_NAME=ru.ifmo.rain.dolzhanskii.bank
PACKAGE_PATH=ru/ifmo/rain/dolzhanskii/bank

OUT_PATH=${SOLUTION_PATH}/_build/production/${PACKAGE_NAME}
LIB_PATH=${SOLUTION_PATH}/lib:${ROOT}/java-advanced-2020/lib
SRC_PATH=${SOLUTION_PATH}/java-solutions/${PACKAGE_PATH}

JUNIT_JAR_PATH=${SOLUTION_PATH}/artifacts/junit-platform-console-standalone-1.7.0-M1.jar

rm -rf ${OUT_PATH}

javac -cp . -p . --module-path ${LIB_PATH} \
    --add-modules junit \
    --add-modules org.junit.jupiter.api \
    --add-modules org.junit.platform.commons \
    --add-modules org.junit.platform.launcher \
    ${SRC_PATH}/*/*.java -d ${OUT_PATH}

cd ${OUT_PATH}

java -jar ${JUNIT_JAR_PATH} -cp . --scan-class-path

RET_CODE=${?}

cd ${BASE_DIR}

if [[ ${RET_CODE} == "0" ]]
then
    exit 0
else
    exit 1
fi