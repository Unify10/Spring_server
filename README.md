# Unify - server

## 사전 준비

- PostgreSQL, Redis가 로컬에 설치되어 실행 중이어야 합니다.

  ```bash
  brew install postgresql@16 redis
  brew services start postgresql@16
  brew services start redis
  ```

- 이메일 인증 기능을 실제로 테스트하려면 Gmail 계정의 [앱 비밀번호](https://myaccount.google.com/apppasswords)가 필요합니다(2단계 인증 활성화 필수).

## 환경 변수 설정 (.env)

DB/Redis 접속 정보, 메일 계정, JWT 서명 키는 `.env` 파일로 관리합니다.

1. `.env.example`을 복사해 `.env` 생성

   ```bash
   cp .env.example .env
   ```

2. 로컬 환경에 맞게 값 수정

   | 변수명 | 설명 | 기본값 |
   | --- | --- | --- |
   | DB_HOST | DB 호스트 | localhost |
   | DB_PORT | DB 포트 | 5432 |
   | DB_NAME | DB 이름 | onyak |
   | DB_USERNAME | DB 계정 | postgres |
   | DB_PASSWORD | DB 비밀번호 | postgres |
   | MAIL_USERNAME | Gmail 계정 (이메일 인증 발송용) | - |
   | MAIL_PASSWORD | Gmail 앱 비밀번호 | - |
   | JWT_SECRET | JWT 서명 키 (`openssl rand -base64 64`로 생성) | - |
   | REDIS_HOST | Redis 호스트 | localhost |
   | REDIS_PORT | Redis 포트 | 6379 |
   | CORS_ALLOWED_ORIGIN | CORS 허용 origin | http://localhost:5173 |
   | FASTAPI_BASE_URL | Python_server(FastAPI) 주소 — 약장 담기·근거 카드에서 호출 | http://localhost:8000 |

`.env`는 `.gitignore`에 등록되어 커밋되지 않으며, 값은 `application.yaml`에서 `${DB_HOST:localhost}` 형태로 참조합니다. 기본값이 없는 변수(`MAIL_*`, `JWT_SECRET`)는 반드시 채워야 앱이 정상적으로 시작됩니다.

## 가상 약장 API (5.1 / 5.2 / 5.3 / 6.2)

보호자는 자기 피보호자의 약장만, 피보호자는 본인 약장만 다룰 수 있습니다.

| Method | URL | 설명 |
| --- | --- | --- |
| GET | `/wards/{wardId}/medications?status=active\|stopped\|all` | 5.1 약장 목록 (기본 `active`) |
| POST | `/wards/{wardId}/medications` | 5.1 약장에 담기 — `item_seq`(품목기준코드), `dosage` 필수 |
| GET | `/medications/{medId}` | 5.1 단건 조회 |
| PATCH | `/medications/{medId}` | 5.2 용량 수정 · `active:false` 중단 · `active:true` 재개 |
| DELETE | `/medications/{medId}` | 5.2 삭제 (복약 체크 기록·분석 결과도 함께 삭제됨) |
| GET / PUT | `/medications/{medId}/schedule` | 5.3 복용 시각(`["08:00","20:00"]`)·알림 설정 |
| GET | `/wards/{wardId}/evidence-card` | 6.2 복약 확인 근거 카드 (DUR 점검) |

- 약 정보와 DUR 판정은 Python_server(FastAPI)가 담당합니다. 실행 중이 아니면 약장 담기(처음 담는 약)와 근거 카드가 `502`를 반환합니다.
- 약을 처음 담을 때 FastAPI `GET /drugs/{itemSeq}`로 받아 `drugs` 테이블에 저장하고, 이후에는 DB 값을 씁니다.
- 근거 카드는 FastAPI `POST /drugs/cabinet-check`로 약장 전체를 한 번에 점검하고, 약별 결과를 `analysis_results`에 남깁니다.
