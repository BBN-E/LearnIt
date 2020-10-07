# common routines for making release/sandbox
# SCRIPT VERSION: 2018-12-04
# 2018-12-04: fix typo
# 2018-02-13

set -e
set -o pipefail
set -u

if ! git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
    echo "$script_dir is not inside a git work tree. This script must run from a valid git repository."
    exit 1
fi

git_repo_path=$(readlink -e $(git rev-parse --git-dir)/..)

if ! git_origin=$(git remote get-url origin); then
    echo "Can not find git address of 'origin' from $git_repo_path"
    exit 1
fi

git_project_name=$(basename "$git_origin" .git)

# Pre-commit hook is not installed upon clone. To make it less invasive we
# install it upon executing various release scripts.
if [ -x "$git_repo_path/tools/install-git-pre-commit.sh" ]; then
    "$git_repo_path/tools/install-git-pre-commit.sh"
fi

# do not execute command if $dry_run is true
function run_cmd() {
    if $dry_run; then
        echo "DRY RUN: $@"
    else
        eval "$@"
    fi
}
