#!/bin/sh
# 只整理已测试的候选产物；此脚本不生成标签、不会发布 Release。
set -eu
version=$(sed -n 's/^version=//p' gradle.properties | tr -d '\r')
mc=$(sed -n 's/^mcVersion=//p' gradle.properties | tr -d '\r')
test -n "$version" && test -n "$mc"
set -- mili-server/build/libs/*-paperclip-"$version".jar
if [ "$#" -ne 1 ] || [ ! -s "$1" ]; then
    printf '%s\n' '需要恰好一个本次构建的 Paperclip JAR' >&2
    exit 1
fi
out="${RUNNER_TEMP:?}/mili-candidate"
mkdir "$out"
name="mili-$mc-paperclip.jar"
cp "$1" "$out/$name"
(cd "$out" && sha256sum "$name" > "$name.sha256")
printf '%s\n' "${GITHUB_SHA:?}" > "$out/commit.txt"
