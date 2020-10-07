#!/usr/bin/env bash
# SCRIPT VERSION
# 2018-12-04: new sandbox updated to latest origin/master upon creation
#             optional <sandbox-suffix> parameter
# 2018-04-03

script_dir=$(dirname $(readlink -f $0))
source $script_dir/release-common.sh
#set -x

function print_help () {
    cat  << END
$0: create a new development sandbox as <sandbox_root_path>/${git_project_name}.YYYY_MM_DD

If an optional <sandbox-suffix> string is given, it will be appended to the sandbox name.

The new sandbox will be a git repository:
 - as a clone of '$git_repo_path' (all uncommited changes will be lost)
 - with 'origin' set to '$git_origin'
 - with branch 'master' set to track 'origin/master' (if 'origin' is reachable)
 - 'master' is updated to 'origin/master' (if origin is reachable)

args: [OPTIONS] -p sandbox_root_path [sandbox-suffix]
  -p sandbox_root_path  : path to the top level sandbox folder
  -g git_repo_path      : path to the git repository to clone, default is the one this script is called from ($git_repo_path)
  -n                    : dry run mode, print shell commands without executing them
  -h                    : show this help
END
}

sandbox_root_path=
sandbox_suffix=
dry_run=false
set -- $(getopt np:g:h "$@")
while [ $# -gt 0 ]
do
    case "$1" in
        (-h) print_help; exit 0;;
        (-n) dry_run=true ;;
        (-p) sandbox_root_path="$2"; shift;;
        (-g) git_repo_path=yes; echo "-g is not supported yet"; exit 1;;
        (--) shift; break;;
        (-*) echo "$0: error - unrecognized option $1" 1>&2; print_help; exit 1;;
        (*)  break;;
    esac
    shift
done

if [ $# -gt 0 ]; then
    echo "$*"
    sandbox_suffix=$(echo "$*" | perl -pe 's/ +/-/g')  # replace space with -
    sandbox_suffix=".$sandbox_suffix"
fi

if [ -z "$sandbox_root_path" ]; then
    echo "-p sandbox_root_path is required, use -h for help."
    exit 1
fi
sandbox_root_path="$(readlink -m $sandbox_root_path)"
if [ ! -e "$sandbox_root_path" ]; then
    run_cmd mkdir -p "$sandbox_root_path"
fi

release_name_prefix="${git_project_name}.$(date +%Y_%m_%d)${sandbox_suffix}"
release_name="$release_name_prefix"

# append a counter to release name if needed
release_counter=1
while [ -e "$sandbox_root_path/$release_name" ]; do
    release_name="${release_name_prefix}_${release_counter}"
    ((release_counter++))
done

run_cmd git clone "$git_repo_path" "$sandbox_root_path/$release_name"
run_cmd cd "$sandbox_root_path/$release_name"
run_cmd git remote rm origin
run_cmd echo "Updating 'origin':"
run_cmd git remote add origin "$git_origin"
run_cmd git remote -v
if run_cmd git fetch; then
    if run_cmd git branch -a --no-color | grep '^\* master' -q; then
        run_cmd git branch --set-upstream-to=origin/master master
        run_cmd git pull
    else
        run_cmd git checkout master
    fi
else
    echo "'git fetch' failed to update from '$git_origin' at this time. You need to manually update later"
fi

if [ -x "$script_dir/install-git-pre-commit.sh" ]; then
    run_cmd "$script_dir/install-git-pre-commit.sh" "$sandbox_root_path/$release_name"
fi

run_cmd echo "Sandbox created: $sandbox_root_path/$release_name"
