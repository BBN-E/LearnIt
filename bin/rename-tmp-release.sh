#!/usr/bin/env bash
# SCRIPT VERSION: 2017-12-12

#set -x

script_dir=$(dirname $(readlink -f $0))
source $script_dir/release-common.sh

function print_help () {
    cat  << END
$0: tmp releases $release_root_path/TMPXXX created $delete_tmp_release_after_days days ago will be moved to $release_root_path/TMP/.

args: [OPTIONS] -p release_root_path
  -p release_root_path  : path to the top level release folder
  -n                    : dry run mode, print shell commands without executing them
  -d days               : tmp releases will be removed if created DAYS ago, default=$delete_tmp_release_after_days
  -h                    : show this help
END
}

delete_tmp_release_after_days=45
release_root_path=
dry_run=false
set -- $(getopt np:d:h "$@")
while [ $# -gt 0 ]
do
    case "$1" in
        (-h) print_help; exit 0;;
        (-n) dry_run=true ;;
        (-p) release_root_path="$2"; shift;;
        (-d) delete_tmp_release_after_days="$2"; shift;;
        (--) shift; break;;
        (-*) echo "$0: error - unrecognized option $1" 1>&2; print_help; exit 1;;
        (*)  break;;
    esac
    shift
done

if [ -z "$release_root_path" ]; then
    echo "-p release_root_path is required, use -h for help."
    exit 1
fi

release_root_path="$(readlink -m $release_root_path)"
if [ ! -e "$release_root_path" ]; then
    echo "$release_root_path does not exist"
    exit 1
fi

run_cmd mkdir -p "$release_root_path/TMP"
for release in $(find "$release_root_path" -maxdepth 1 -type d -name 'TMP20*' -ctime "+$delete_tmp_release_after_days"); do
    release_name=$(basename $release)
    run_cmd mv -v "$release" "$release_root_path/TMP/$release_name"
done
