#!/bin/sh
#
# The MIT License
#
# Copyright 2018 Yahoo Japan Corporation.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#
# AUTHOR:   Hirotaka Wakabayashi
# CREATE:   Fri, 14 Sep 2018
# REVISION:
#

# Sets the default locale. LC_ALL has precedence over other LC* variables.
unset LANG
unset LANGUAGE
LC_ALL=en_US.utf8
export LC_ALL

# Sets PATH. setup_*.sh uses useradd command
PATH=${PATH}:/usr/sbin:/sbin

# an unset parameter expansion will fail
set -u

# umask 022 is enough
umask 022

# environments
REPOSITORY_PATH=${GITHUB_REPOSITORY}
SRCDIR=$(cd $(dirname "$0") && pwd)
DEBUG=1
if test "${DEBUG}" -eq 1; then
    TAG="$(basename $0) -s"
else
    TAG=$(basename $0)
fi

# GITHUB_TOKEN should be defined.
if test -z "${GITHUB_TOKEN}"; then
    logger -t ${TAG} -p user.error "No GITHUB_TOKEN variable defined."
    exit 1
fi
# GITHUB_REPOSITORY should be defined.
# https://help.github.com/en/actions/automating-your-workflow-with-github-actions/using-environment-variables
if test -z "${GITHUB_REPOSITORY}"; then
    logger -t ${TAG} -p user.error "No GITHUB_REPOSITORY variable defined."
    exit 1
fi

# API SPEC
# https://developer.github.com/v3/repos/releases/#get-the-latest-release
TEMP_FILE=$(mktemp)
curl -sH "Authorization: token ${GITHUB_TOKEN}" "https://api.github.com/repos/${REPOSITORY_PATH}/releases/latest" -o ${TEMP_FILE}
if test "${?}" != "0"; then
    logger -t ${TAG} -p user.error "GitHub API get-the-latest-release returned error."
    rm -f ${TEMP_FILE}
    exit 1
fi
UPLOAD_URL=$(cat ${TEMP_FILE} | python3 -c "import sys, json; print(json.load(sys.stdin)['upload_url'])")
if test -z "${UPLOAD_URL}"; then
    logger -t ${TAG} -p user.error "UPLOAD_URL is empty(or undefined), which should be defined."
    rm -f ${TEMP_FILE}
    exit 1
fi
TAG_NAME=$(cat ${TEMP_FILE} | python3 -c "import sys, json; print(json.load(sys.stdin)['tag_name'])" | perl -ne '/v(.*)/; print $1;')
if test -z "${TAG_NAME}"; then
    logger -t ${TAG} -p user.error "TAG_NAME is empty(or undefined), which should be defined."
    rm -f ${TEMP_FILE}
    exit 1
fi

# Get the jar file in local target directory.
TARGET_FILE="./target/k2hash-${TAG_NAME}.jar"
if ! test -f "${TARGET_FILE}"; then
    logger -t ${TAG} -p user.error "${TARGET_FILE} doesn't exist, which should exist."
    rm -f ${TEMP_FILE}
    exit 1
fi
ASSET_FILE_NAME=$(basename ${TARGET_FILE})
if test -z "${ASSET_FILE_NAME}"; then
    logger -t ${TAG} -p user.error "ASSET_FILE_NAME is empty(or undefined), which should be defined."
    rm -f ${TEMP_FILE}
    exit 1
fi
UPLOAD_URL_WITH_QUERY=$(echo ${UPLOAD_URL} | perl -pe "s|{\?name,label}|?name=${ASSET_FILE_NAME}|g")
if test -z "${UPLOAD_URL_WITH_QUERY}"; then
    logger -t ${TAG} -p user.error "UPLOAD_URL_WITH_QUERY is empty(or undefined), which should be defined."
    rm -f ${TEMP_FILE}
    exit 1
fi
logger -t ${TAG} -p user.debug "TARGET_FILE:${TARGET_FILE} ASSET_FILE_NAME:${ASSET_FILE_NAME} UPLOAD_URL_WITH_QUERY:${UPLOAD_URL_WITH_QUERY}"

# API SPEC
# https://developer.github.com/v3/repos/releases/#upload-a-release-asset
# Note: curl exits with zero if the jar file already exists.
curl -s --request PATCH -L# --data-binary @"${TARGET_FILE}" -H "Authorization: token ${GITHUB_TOKEN}" -H "Content-Type: application/octet-stream" ${UPLOAD_URL_WITH_QUERY}
RET=${?}
if test "${RET}" != "0"; then
    logger -t ${TAG} -p user.error "GitHub API upload-a-release-asset returned error."
else
    logger -t ${TAG} -p user.info "GitHub API upload-a-release-asset success"
fi

POM_FILE="./pom.xml"
if ! test -f "${POM_FILE}"; then
    logger -t ${TAG} -p user.error "${POM_FILE} doesn't exist, which should exist."
    rm -f ${TEMP_FILE}
    exit 1
fi
POM_ASSET_FILE_NAME="k2hash-${TAG_NAME}.pom"
POM_UPLOAD_URL_WITH_QUERY=$(echo ${UPLOAD_URL} | perl -pe "s|{\?name,label}|?name=${POM_ASSET_FILE_NAME}|g")
curl -s --request PATCH -L# --data-binary @"${POM_FILE}" -H "Authorization: token ${GITHUB_TOKEN}" -H "Content-Type: application/xml" ${POM_UPLOAD_URL_WITH_QUERY}
RET=${?}
if test "${RET}" != "0"; then
    logger -t ${TAG} -p user.error "GitHub API upload-a-release-asset returned error."
else
    logger -t ${TAG} -p user.info "GitHub API upload-a-release-asset success"
fi

rm -f ${TEMP_FILE}
exit ${RET}

