#!/bin/bash

get_abs_script_path() {
  pushd . >/dev/null
  cd $(dirname $0)
  appdir=$(pwd)
  popd  >/dev/null
}
get_abs_script_path

"$appdir/stop_server.sh"
"$appdir/start_server.sh"
