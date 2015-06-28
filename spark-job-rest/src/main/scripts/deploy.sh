#!/usr/bin/env bash

set -e

CMD=$1
ARG1=$2

CDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
PROJECT_DIR="${CDIR}/../../../.."

SJR_IS_REMOTE_DEPLOY=${SJR_IS_REMOTE_DEPLOY-false}
SJR_PACKAGE_PATH=${SJR_PACKAGE_PATH-${PROJECT_DIR}/spark-job-rest/target/spark-job-rest.tar.gz}

SJR_DEPLOY_PATH="${SJR_DEPLOY_PATH}"                 # Empty variable will cause error in action
SJR_REMOTE_DEPLOY_PATH="${SJR_REMOTE_DEPLOY_PATH}"   # Overrides SJR_DEPLOY_PATH in case of remote deploy
SJR_DEPLOY_KEY="${SJR_DEPLOY_KEY}"                   # Empty by default
SJR_DEPLOY_HOST="${SJR_DEPLOY_HOST}"                 # Empty for local deploy

CONFIGURATION_IS_SET="false"

function setup_defaults() {
    if [ -z "${SJR_DEPLOY_PATH}" ]; then
        echo "Spark-Job-REST deployment path is not defined. Set 'SJR_DEPLOY_PATH' before running this script."
        exit -1
    fi
}

function setup_remote() {
    SSH_KEY_EXPRESSION=""
    if [ ! -z "${SJR_DEPLOY_KEY}" ]; then
        echo "Using SSH key from '${SJR_DEPLOY_KEY}'"
        SSH_KEY_EXPRESSION="-i ${SJR_DEPLOY_KEY}"
    fi

    if [ -z "${SJR_DEPLOY_HOST}" ]; then
        echo "Spark-Job-REST deployment host is not defined. Set 'SJR_DEPLOY_HOST' before running this script."
        exit -1
    fi

    # Override deploy path in remote mode
    if [ ! -z "${SJR_REMOTE_DEPLOY_PATH}" ]; then
        SJR_DEPLOY_PATH="${SJR_REMOTE_DEPLOY_PATH}"
    fi
}

function setup() {
    if [ "${CONFIGURATION_IS_SET}" = "false" ]; then
        CONFIGURATION_IS_SET="true"
        setup_defaults
        if [ "${SJR_IS_REMOTE_DEPLOY}" = "true" ]; then
            setup_remote
        else
            SJR_DEPLOY_HOST="localhost"
        fi
    fi
}

function exec_remote() {
    setup
    ssh -i "${SJR_DEPLOY_KEY}" "${SJR_DEPLOY_HOST}" "$1"
}

function exec_local() {
    setup
    eval "$1"
}

function exec_cmd() {
    if [ "$SJR_IS_REMOTE_DEPLOY" = "true" ]; then
        exec_remote "$1"
    else
        exec_local "$1"
    fi
}

function stop_server() {
    echo "Stopping server"
    exec_cmd "if [ -d ${SJR_DEPLOY_PATH} ]; then ${SJR_DEPLOY_PATH}/bin/stop_server.sh; fi"
    exec_cmd "pkill -f 'java.*spark-job-rest.jar'" || true
}

function delete_server() {
    echo "Removing server"
    setup
    exec_cmd "rm -rf ${SJR_DEPLOY_PATH}"
}

function upload_tarball() {
    if [ "${SJR_IS_REMOTE_DEPLOY}" = "true" ]; then
        echo "Upload tarball"
        scp "${SSH_KEY_EXPRESSION}" "$SJR_PACKAGE_PATH" "${SJR_DEPLOY_HOST}":"/tmp/"
    fi
}

function extract_package() {
    echo "Extract from tarball"
    exec_cmd "mkdir -p ${SJR_DEPLOY_PATH}"
    if [ "${SJR_IS_REMOTE_DEPLOY}" = "true" ]; then
        exec_remote "tar zxf /tmp/spark-job-rest.tar.gz -C ${SJR_DEPLOY_PATH} --strip-components=1"
    else
        exec_local "tar zxf ${SJR_PACKAGE_PATH} -C ${SJR_DEPLOY_PATH} --strip-components=1"
    fi
}

function remove_server() {
    echo "Removing instance at ${SJR_DEPLOY_HOST}:${SJR_DEPLOY_PATH}"
    stop_server
    delete_server
}

function deploy_server() {
    echo "Deploing to ${SJR_DEPLOY_HOST}:${SJR_DEPLOY_PATH}"
    remove_server
    upload_tarball
    extract_package
    start_server
}

function start_server() {
    echo "Run server"
    exec_cmd "${SJR_DEPLOY_PATH}/bin/start_server.sh"
}

function server_log() {
    echo "Spark-Job-REST main log:"
    exec_cmd "tail -f ${SJR_DEPLOY_PATH}/logs/spark-job-rest.log"
}

function server_log_context() {
    CONTEXT_NAME=$ARG1
    echo "Spark-Job-REST '${CONTEXT_NAME}' log:"
    exec_cmd "tail -f ${SJR_DEPLOY_PATH}/logs/${CONTEXT_NAME}.log"
}

function show_help() {
    echo "Spark-Job-REST deployment tool"
    echo "Usage: deploy.sh [deploy|start|stop|restart|log|log-context <context>]"
}

function show_vars() {
    echo "SJR_DEPLOY_PATH=${SJR_DEPLOY_PATH}"
    echo "SJR_DEPLOY_HOST=${SJR_DEPLOY_HOST}"
    echo "SJR_DEPLOY_KEY=${SJR_DEPLOY_KEY}"
    echo "SJR_PACKAGE_PATH=${SJR_PACKAGE_PATH}"
    echo "SJR_IS_REMOTE_DEPLOY=${SJR_IS_REMOTE_DEPLOY}"
    echo "SJR_REMOTE_DEPLOY_PATH=${SJR_REMOTE_DEPLOY_PATH}"
}

function main() {
    case "$CMD" in
    deploy) setup
        deploy_server
        ;;
    remove) setup
        remove_server
        ;;
    stop) setup
        stop_server
        ;;
    start) setup
        start_server
        ;;
    restart) setup
        stop_server
        start_server
        ;;
    log) setup
        server_log
        ;;
    log-context) setup
        server_log_context
        ;;
    debug) show_vars
        ;;
    help) show_help
        ;;
    *) show_help
        ;;
    esac
}

main