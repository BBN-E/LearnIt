#!/bin/sh
#
# This script is used to enforce python coding style.
# A hook script to verify what is about to be committed.
# Called by "git commit" with no arguments.  The hook should
# exit with non-zero status after issuing an appropriate message if
# it wants to stop the commit.
#
# To enable this hook, rename this file to "pre-commit".

script_dir=$(dirname $(readlink -f $0))
git_repo_path="$script_dir/.."

if git rev-parse --verify HEAD >/dev/null 2>&1
then
    against=HEAD
else
    # Initial commit: diff against an empty tree object
    against=4b825dc642cb6eb9a060e54bf8d69288fbee4904
fi

pycodestyle="/d4m/material/software/python/venv/default/bin/pycodestyle --ignore E501,E402"

# Redirect output to stderr.
exec 1>&2

changed_files=$(git diff --cached --name-only --diff-filter=ACMR | grep -E "\.py$")
if [ -n "$changed_files" ]; then
    for file in $changed_files; do
        if ! fgrep -q -x "./$file" "$git_repo_path/style-check.ignored"; then
            err_msg=$($pycodestyle $file)
            if [ $? -ne 0 ]; then
                cat <<EOF
===================================================================
Python coding style check failed for $file:

$err_msg

Python coding style: https://www.python.org/dev/peps/pep-0008/

Because coding style was not enforced before, you may see lots of
warnings even when only minor changes are made. Please fix the code
and try again. To manually run a check:

$pycodestyle $(readlink -f $file)

If you are adding a third-party code or want to ignore certain files
please add to '$git_repo_path/style-check.ignored' file with:
./$file
===================================================================
EOF
                exit 1
            fi
        fi
    done
fi
