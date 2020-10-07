#!/usr/bin/env bash
# SCRIPT VERSION: 2019-02-15

#set -x

script_dir=$(dirname $(readlink -f $0))
source $script_dir/release-common.sh

function print_help () {
    cat  << END
$0: schedule to create a new public release as
/d4m/material/releases/$git_project_name/RYYYY_MM_DD

If a commit SHA is given via -c release_commit, a temporary release will be made
as /d4m/material/releases/$git_project_name/TMPYYYY_MM_DD.RELEASE_COMMIT

The new release will be a git repository:
 - cloned from '$git_repo_path' with .git directory removed
 - readonly to non-owners

args: [OPTIONS] [-c release_commit]
  -c release_commit     : release commit SHA. Default is 'master'.
  -n                    : dry run mode, print shell commands without executing them.
  -f                    : force public release even when local repo is not on master.
  -h                    : show this help.
END
}

release_root_path="/d4m/nlp/releases/${git_project_name}"
release_commit=master
dry_run=false
force_release=false # force public release even when local repo is not on origin/master
set -- $(getopt fnc:r:h "$@")
while [ $# -gt 0 ]
do
    case "$1" in
        (-h) print_help; exit 0;;
        (-n) dry_run=true ;;
        (-f) force_release=true ;;
        (-c) release_commit="$2"; shift;;
        (--) shift;
             if [ $# -gt 0 ]; then
                 echo "$0: unrecognized argument '$@'"
                 echo "Use -c '$@' if you want to create a temporary release using commit '$@'."
                 echo "Use -h for help."
                 exit 1
             fi
             break;;
        (-*) echo "$0: error - unrecognized option $1" 1>&2; print_help; exit 1;;
        (*)  break;;
    esac
    shift
done

if [ ! -e "$release_root_path" ]; then
    echo "$release_root_path does not exist!"
    exit 1
fi

cd "$git_repo_path"

if [ -n "$(git status --untracked-files=no --porcelain)" ]; then
    echo "$0 needs to operate from a clean repository in order to create new tag to trigger a release."
    echo "$git_repo_path contains uncommitted changes. Either commit or stash uncommited changes and try again."
    exit 1
fi

# fetch origin in case $release_commit does not exist in current repository.
run_cmd git fetch origin
if [ "$release_commit" != "master" ]; then
    if ! release_commit_short=$(git rev-parse --short $release_commit); then
        echo "commit '$release_commit' does not exist in $git_repo_path"
        exit 1
    fi
    release_name="TMP$(date +%Y_%m_%d).$release_commit_short"
else
    head_commit=$(git rev-parse --short HEAD)
    master_commit=$(git rev-parse --short origin/master)
    # check to see if HEAD is origin/master
    head_is_master=false
    nAtoB=$(git rev-list --count origin/master..HEAD)
    nBtoA=$(git rev-list --count HEAD..origin/master)
    if [ $nAtoB -eq 0 -a $nBtoA -eq 0 ]; then
        head_is_master=true
    fi
    if ! $head_is_master && ! $force_release; then
        cat  << END
================================ WARNING ======================================
You are trying to make a public release when you are not on master:

HEAD:          $head_commit ($(git log -1 --format=%cd --date=local $head_commit))
origin/master: $master_commit ($(git log -1 --format=%cd --date=local $master_commit))

A public release can only be made off origin/master. If you want to make a
temporary release off your local repo's HEAD please re-run with:

$0 -c $head_commit

To force a public release off HEAD ($head_commit):

$0 -f

===============================================================================
END
        exit 1
    fi
    release_name_prefix="R$(date +%Y_%m_%d)"
    release_name=$release_name_prefix
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

# check to see if release tag exists
if git rev-parse $release_name > /dev/null 2>&1; then
    cat  << END
Tag '$release_name' already exists in $git_repo_path but corresponding release dir '$release_dir' does not exist.
This can happen when there was an error during previous release creation. To fix this, do one of the followings:
  - Go to http://e-gitlab.bbn.com/<group-name>/$git_project_name/pipelines and re-try failed release pipeline
  - Run 'git tag --delete $release_name && git push --delete origin $release_name' to remove the tag and try again
END
fi

run_cmd git tag $release_name
run_cmd git push origin $release_name

cat  << END

New tag '$release_name' pushed to '$git_origin'
This will trigger a new CI pipeline to create a new release:

Release Directory: $release_dir
Release Tag:       $release_name
Release Commit:    $release_commit

END

if [ "$release_commit" != "master" ]; then
    cat  << END
This is a temporary release and will be renamed to $release_root_path/TMP/$release_name after 45 days.
Please consider switching to a public release at that point.

END
    fi
cat  << END

If there is an error during release creation the release directory will not be
created but the release tag still exists. You can run the commands below to
remove the tag and try again:
git tag --delete $release_name && git push --delete origin $release_name
===============================================================================
END
