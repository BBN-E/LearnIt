#!/usr/bin/env bash
# SCRIPT VERSION: 2018-12-04
# 2018-12-04: renamed to release-helper.sh to avoid confusion with make-xxx-release.sh scripts
# 2017-12-12

set -x

script_dir=$(dirname $(readlink -f $0))
source $script_dir/release-common.sh

function print_help () {
    cat  << END
$0: create a new release as <release_root_path>/RYYYY_MM_DD

The new release will be a git repository:
 - cloned from '$git_repo_path' (all uncommited changes will be lost)
 - with 'origin' set to '$git_origin'
 - readonly to non-owners
 - .git directory removed

args: [OPTIONS] -p release_root_path
  -p release_root_path  : path to the top level release folder
  -r release_name       : release name. Default is RYYYY_MM_DD_N where N is a tie breaking counter.
  -c release_commit     : release commit SHA. Default is 'master'.
  -g git_repo_path      : path to the git repository to clone, default is the one this script is called from ($git_repo_path)
  -n                    : dry run mode, print shell commands without executing them
  -h                    : show this help

Note: this script is not meant to be called directly by end-user, use make-xxx-release.sh instead.

END
}

release_name=
release_root_path=
release_commit=master
dry_run=false
set -- $(getopt np:g:c:r:h "$@")
while [ $# -gt 0 ]
do
    case "$1" in
        (-h) print_help; exit 0;;
        (-n) dry_run=true ;;
        (-p) release_root_path="$2"; shift;;
        (-c) release_commit="$2"; shift;;
        (-r) release_name="$2"; shift;;
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
    run_cmd mkdir -p "$release_root_path"
fi

if [ ! -w "$release_root_path" ]; then
    echo "Do not have write permission to $release_root_path"
    echo "Note this script is not meant to be called directly to make a public release. Use make-xxx-release.sh instead."
    exit 1
fi

if [ -z "$release_name" ]; then
    release_name_prefix="R$(date +%Y_%m_%d)"
    release_name="$release_name_prefix"

    # append a tie breaking counter if needed
    release_counter=1
    while [ -e "$release_root_path/$release_name" ]; do
        release_name="${release_name_prefix}_${release_counter}"
        ((release_counter++))
    done
fi

release_dir="$release_root_path/$release_name"
if [ -e "$release_dir" ]; then
    echo "Release dir $release_dir already exists!"
    exit 1
fi

run_cmd git clone "file:///$git_repo_path" "$release_dir"
run_cmd cd "$release_dir"
run_cmd git checkout $release_commit
run_cmd git remote rm origin
run_cmd echo "Updating 'origin':"
run_cmd git remote add origin "$git_origin"
run_cmd git remote -v
run_cmd rm -rf .git
run_cmd chmod -R u=rwX,g=rX,o=rX "$release_dir"
