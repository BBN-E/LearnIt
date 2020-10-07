#!/bin/sh
# install git pre-commit script into .git/hooks if necessary


script_dir=$(dirname $(readlink -f $0))
git_repo_path="$script_dir/.."

usage() {
    echo "usage: `basename $0` <git-repo-dir>"
    echo "if <git-repo-dir> is omitted the repo this script belongs to is used."
}

if [ "$#" -eq 1 ]; then
    git_repo_path=$(readlink -m "$1")
    if [ ! -d "$git_repo_path" ]; then
        echo "Repo directory '$git_repo_path' does not exist!"
        exit 1
    fi
elif [ "$#" -gt 1 ]; then
    usage
    exit 1
fi

if [ ! -L "$git_repo_path/.git/hooks/pre-commit" ] && [ -w "$git_repo_path/.git" ]; then
    mkdir -p "$git_repo_path/.git/hooks"
    cd "$git_repo_path/.git/hooks"
    ln -s "../../tools/git-pre-commit.sh" pre-commit
    echo "One time installation of git pre-commit hook '$git_repo_path/.git/hooks/pre-commit' is done."
fi
