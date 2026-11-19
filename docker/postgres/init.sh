#!/bin/sh
# 처음 뜰 때(볼륨이 비어 있을 때)만 실행된다. 스키마를 바꿨으면 `docker compose down -v` 후 다시 띄울 것.
set -e

# onyak.sql 첫 줄의 CREATE DATABASE 는 로컬 PostgreSQL에 직접 넣을 때용이다.
# 컨테이너는 POSTGRES_DB 로 이미 만들어 두므로 그 줄만 빼고 실행한다.
sed '/^CREATE DATABASE/d' /schema/onyak.sql \
    | psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB"
