#!/bin/bash

# Print message $2 with log-level $1 to STDERR, colorized if terminal
log() {
        local level=${1?}
        shift
        local code= line="[$(date '+%F %T')] $level: $*"
        if [ -t 2 ]
        then
                case "$level" in
                INFO) code=36 ;;
                DEBUG) code=30 ;;
                WARN) code=33 ;;
                ERROR) code=31 ;;
                *) code=37 ;;
                esac
                echo -e "\033[${code}m${line}\033[0m"
        else
                echo "$line"
        fi >&2
}

# check if bower is installed
command -v "bower" >/dev/null 2>&1 || { log "ERROR" "Please install bower first"; exit 1; }

# install the needed web stuff by using bower, if installed
bower install

# insure the destination dirs for scripts and styles exist
mkdir -p ./resources/public/js
mkdir -p ./resources/public/css  

# copy in place the needed styles and scripts

log "INFO" "Installing bootstrap"
cp bower_components/bootstrap/dist/css/bootstrap.min.css ./resources/public/css
cp bower_components/bootstrap/dist/js/bootstrap.min.js ./resources/public/js

log "INFO" "Installing bootstrapi-sortable"
cp bower_components/bootstrap-sortable/Contents/bootstrap-sortable.css ./resources/public/css
cp bower_components/bootstrap-sortable/Scripts/bootstrap-sortable.js ./resources/public/js
cp bower_components/moment/min/moment.min.js ./resources/public/js 

log "INFO" "Installing jquery"
cp bower_components/jquery/dist/jquery.min.js ./resources/public/js
cp bower_components/jquery/dist/jquery.min.map ./resources/public/js
