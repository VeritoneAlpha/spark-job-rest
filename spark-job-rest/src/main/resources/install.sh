#!/usr/bin/env bash

CDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
APP_DIR="${CDIR}/.."

CONFIG_TEMPLATE_PATH="${CDIR}/application.conf.template"
CONFIG_PATH="${CDIR}/application.conf"

source "${CDIR}/settings.sh"

sed -e "s|{app_dir}|${APP_DIR}|" -e "s|{spark_home}|${SPARK_HOME}|" "${CONFIG_TEMPLATE_PATH}" > "${CONFIG_PATH}"