#!/usr/bin/env bash
# docker compose 로 띄운 전체 스택이 실제로 이어지는지 확인하는 스모크 테스트.
#
#   docker compose up -d --build
#   ./scripts/smoke-test.sh
#
# 보호자 가입 → 피보호자 등록 → 약 두 개 담기 → 근거 카드 조회까지 한 번에 돈다.
# 약을 담을 때 Spring 이 Python_server 의 /drugs/{id} 를, 근거 카드에서 /drugs/cabinet-check 를 부르므로
# 두 서버 사이 연동을 함께 확인한다. 의약품 검색·바코드는 Spring 프록시(/drugs/**)로 확인한다.
#
# 메일 발송 없이 가입하려고 이메일 인증 완료 상태를 Redis 에 직접 넣는다. 로컬 스택 전용.
# 약은 데모 DB(narsha-dur/data/drugs.sqlite)에 있는 타이레놀정500mg·타세놀이알서방정(같은 성분)을 쓴다.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="smoke-$(date +%s)@example.com"
PASSWORD="smoke-test-1234"
DRUG_A="${DRUG_A:-195700020}"
DRUG_B="${DRUG_B:-200012345}"
SEARCH_QUERY="${SEARCH_QUERY:-타이레놀}"

cd "$(dirname "$0")/.."

fail() { echo "FAIL: $*" >&2; exit 1; }
step() { echo; echo "== $*"; }

BODY=$(mktemp)
trap 'rm -f "$BODY"' EXIT

# call METHOD PATH [JSON] — 상태 코드는 $STATUS, 응답 본문은 $BODY 파일에 남긴다.
# $(...) 안에서 부르면 서브셸이라 STATUS 가 사라지므로 항상 단독으로 부르고 본문은 jq 로 읽는다.
call() {
    local method=$1 path=$2 body=${3:-}
    local args=(-s -o "$BODY" -w '%{http_code}' -X "$method" "$BASE_URL$path"
                -H 'Content-Type: application/json')
    [[ -n "${TOKEN:-}" ]] && args+=(-H "Authorization: Bearer $TOKEN")
    [[ -n "$body" ]] && args+=(-d "$body")
    STATUS=$(curl "${args[@]}")
}

expect() {
    local want=$1 what=$2
    [[ "$STATUS" == "$want" ]] || fail "$what: HTTP $STATUS (기대 $want) — $(cat "$BODY")"
    echo "ok  $what ($STATUS)"
}

step "이메일 인증 완료 상태를 Redis 에 넣기"
docker compose exec -T redis redis-cli HSET "email_verification:$EMAIL" \
    email "$EMAIL" code 000000 verified 1 > /dev/null
docker compose exec -T redis redis-cli EXPIRE "email_verification:$EMAIL" 300 > /dev/null

step "보호자 가입·로그인"
call POST /auth/signup "$(jq -n --arg e "$EMAIL" --arg p "$PASSWORD" \
    '{email:$e, password:$p, password_confirm:$p, name:"스모크보호자", phone:"010-1234-5678"}')"
expect 201 "POST /auth/signup"
call POST /auth/login "$(jq -n --arg e "$EMAIL" --arg p "$PASSWORD" '{email:$e, password:$p}')"
expect 200 "POST /auth/login"
TOKEN=$(jq -r '.data.access_token' "$BODY")

step "피보호자 등록"
call POST /ward/auth/signup '{"name":"스모크어르신","birth_date":"1948-03-02","weight":58.5,"height":160.0,
    "gender":"F","is_pregnant":false,"medication_push_enabled":true,"sos_push_enabled":true}'
expect 201 "POST /ward/auth/signup"
WARD_ID=$(jq -r '.data.ward_id' "$BODY")
echo "ward_id=$WARD_ID"

step "Spring 프록시로 의약품 검색·상세"
call GET "/drugs/search?q=$(jq -rn --arg q "$SEARCH_QUERY" '$q|@uri')"
expect 200 "GET /drugs/search"
jq -c '.data | {total, first: .items[0].item_name}' "$BODY"
jq -e --arg id "$DRUG_A" '.data.items[] | select(.drug_id == $id)' "$BODY" > /dev/null \
    || fail "검색 결과에 $DRUG_A 가 없음"
call GET "/drugs/$DRUG_A"
expect 200 "GET /drugs/$DRUG_A"
jq -c '.data | {drug_id, item_name, classification}' "$BODY"
[[ $(jq -r '.data.drug_id' "$BODY") == "$DRUG_A" ]] || fail "상세 응답의 drug_id 가 $DRUG_A 가 아님"
call GET "/drugs/000000000"
expect 404 "GET /drugs/000000000 (없는 약)"

step "약장에 약 두 개 담기 (Spring → Python /drugs/{id})"
for seq in "$DRUG_A" "$DRUG_B"; do
    call POST "/wards/$WARD_ID/medications" "$(jq -n --arg s "$seq" \
        '{item_seq:$s, dosage:"1정", schedule_times:["08:00","20:00"]}')"
    expect 201 "POST /wards/$WARD_ID/medications ($seq)"
    jq -c '.data | {medication_id, drug_id, drug_name}' "$BODY"
done

step "근거 카드 (Spring → Python /drugs/cabinet-check)"
call GET "/wards/$WARD_ID/evidence-card"
expect 200 "GET /wards/$WARD_ID/evidence-card"
CARD=$(cat "$BODY")
echo "$CARD" | jq '.data | {level, headline, patient, medications: [.medications[] | {item_name, classification, lookup_failed}], interactions: [.interactions[] | {kind, severity, item_names}]}'

[[ $(echo "$CARD" | jq '.data.medications | length') == 2 ]] || fail "근거 카드에 약이 2개가 아님"
[[ $(echo "$CARD" | jq '[.data.medications[] | select(.lookup_failed)] | length') == 0 ]] \
    || fail "Python 쪽 조회에 실패한 약이 있음 (lookup_failed)"
echo "$CARD" | jq -e '.data.interactions[] | select(.kind == "성분중복")' > /dev/null \
    || fail "같은 성분 두 약인데 성분중복이 안 잡힘"
echo "ok  성분중복 판정"

echo
echo "PASS"
