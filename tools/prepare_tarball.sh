#!/usr/bin/env bash

set -xe

MAVEN=$(which mvn)
REPO_PATH=$(cd $(dirname $0) && cd .. && pwd)

if [[ -z "${MAVEN}" ]]; then
    echo "Maven isn't installed"
    exit 1
fi

pushd ${REPO_PATH}

${MAVEN} clean
${MAVEN} package

MAIN_JAR=$(basename $(find "${REPO_PATH}/target/" -maxdepth 1 -name '*.jar' | tail -1))
TAR_FILE="${MAIN_JAR%.*}.tar.gz"
TMP_DIR=$(mktemp -d)

cp "${REPO_PATH}/target/${MAIN_JAR}" "${TMP_DIR}/${MAIN_JAR}"
cp -R "${REPO_PATH}/target/lib" "${TMP_DIR}/lib"
cp "${REPO_PATH}/examples/observer.ini" "${TMP_DIR}/observer.ini"
cp "${REPO_PATH}/tools/run.sh" "${TMP_DIR}/run.sh"
cp -R "${REPO_PATH}/templates" "${TMP_DIR}/templates"
cp -R "${REPO_PATH}/static" "${TMP_DIR}/static"

pushd "${TMP_DIR}"
tar czvf "${TAR_FILE}" "${MAIN_JAR}" observer.ini run.sh lib static templates
popd

mv "${TMP_DIR}/${TAR_FILE}" "${REPO_PATH}/${TAR_FILE}"
rm -rf "${TMP_DIR}"

popd