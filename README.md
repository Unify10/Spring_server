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

## Docker로 전체 스택 띄우기

Spring·Python_server·PostgreSQL·Redis를 한 번에 띄웁니다. Python_server 저장소를 이 저장소 옆(`../narsha-dur`)에 두거나 `.env`에 `PYTHON_SERVER_DIR`로 경로를 지정하세요. `.env`에는 `JWT_SECRET`이 있어야 합니다.

```bash
docker compose up -d --build
./scripts/smoke-test.sh
```

- 밖으로 열리는 포트는 Spring(`8080`) 하나입니다. Python_server는 compose 네트워크 안에서만 보이고, 앱은 아래 의약품 API로 Spring을 거쳐 부릅니다.
- DB 스키마는 처음 뜰 때 `onyak.sql`로 만듭니다. 스키마를 바꿨으면 `docker compose down -v`로 볼륨을 지운 뒤 다시 띄우세요.
- Python_server의 `.env`(CLOVA 시크릿·공공데이터 인증키)와 `data/drugs.sqlite`를 그대로 씁니다. 이미지에는 PaddleOCR이 없어서 사진 인식은 바코드 → CLOVA 순으로만 동작합니다.
- `smoke-test.sh`는 가입 → 피보호자 등록 → 검색·상세 → 약 두 개 담기 → 근거 카드까지 확인합니다. 메일 없이 가입하려고 Redis에 인증 완료 상태를 직접 넣으므로 로컬 전용입니다.

## 의약품 검색 / OCR API (3장)

Python_server의 같은 경로로 그대로 넘기고, `data`에는 Python_server 응답이 그대로 들어갑니다. 로그인한 보호자·피보호자 모두 쓸 수 있습니다. 약장에 담을 때 쓰는 `item_seq`는 여기 응답의 `drug_id`입니다.

| Method | URL | 설명 |
| --- | --- | --- |
| GET | `/drugs/search?q=&limit=20&otc_only=false` | 3.4 약품명·성분명 검색 (`q` 2자 이상) |
| GET | `/drugs/barcode/{code}` | 3.3 바코드(GTIN-13) → 품목기준코드 |
| GET | `/drugs/{drugId}` | 3.5 상세 + 전문/일반 판정 근거 |
| GET | `/drugs/{drugId}/dur?age=&pregnant=` | DUR 금기·주의 |
| POST | `/drugs/ocr` (multipart `file`) | 3.1/3.2 사진 → 약품명 후보 |
| POST | `/drugs/analyze` (multipart `file`, `age`, `pregnant`) | 사진 한 장 → 판정까지 |

- **응답 스키마는 Python_server가 정합니다.** Spring은 DTO로 옮기지 않고 FastAPI 응답(JSON)을 `data`에 그대로 담습니다. 의도한 방식이며, 그래서 두 가지를 감수합니다.
  - Swagger(`/swagger-ui`)에는 이 API들의 응답 필드가 나오지 않습니다. 필드는 Python_server의 `/docs`를 보세요.
  - Python_server가 응답 필드를 바꾸면 앱 응답도 그대로 바뀝니다. 필드를 바꾸거나 지우는 변경은 앱 담당과 먼저 맞춰야 합니다. Spring이 직접 쓰는 `/drugs/{id}`·`/drugs/cabinet-check`는 `DrugApiClient`의 DTO로 받으므로 영향 범위가 다릅니다.
- 이미지는 10MB까지 받습니다 (넘으면 `413 FILE_TOO_LARGE`).
- 없는 약 `404 DRUG_NOT_FOUND`, 빈 파일 등 `400 DRUG_REQUEST_INVALID`, Python_server·CLOVA·공공데이터 장애 `502 DRUG_API_UNAVAILABLE`.

## 가상 약장 API (5.1 / 5.2 / 5.3 / 6.2)

보호자는 자기 피보호자의 약장 전체를 다룰 수 있습니다. 피보호자는 본인 약장의 조회·복용 일정 변경·근거 카드만 가능하고, 담기·중단·재개·용량 변경·삭제는 `403`입니다.

| Method | URL | 설명 |
| --- | --- | --- |
| GET | `/wards/{wardId}/medications?status=active\|stopped\|all` | 5.1 약장 목록 (기본 `active`) |
| POST | `/wards/{wardId}/medications` | 5.1 약장에 담기 — `item_seq`(품목기준코드), `dosage` 필수 |
| GET | `/medications/{medId}` | 5.1 단건 조회 |
| PATCH | `/medications/{medId}` | 5.2 용량 수정 · `active:false` 중단 · `active:true` 재개 |
| DELETE | `/medications/{medId}` | 5.2 삭제 (복약 체크 기록·분석 결과도 함께 삭제됨) |
| GET / PATCH | `/medications/{medId}/schedule` | 5.3 복용 시각(`["08:00","20:00"]`)·알림 변경 (보낸 필드만) |
| GET | `/wards/{wardId}/evidence-card` | 6.2 복약 확인 근거 카드 (DUR 점검) |

- 약 정보와 DUR 판정은 Python_server(FastAPI)가 담당합니다. 실행 중이 아니면 약장 담기(처음 담는 약)와 근거 카드가 `502`를 반환합니다.
- 약을 처음 담을 때 FastAPI `GET /drugs/{itemSeq}`로 받아 `drugs` 테이블에 저장하고, 이후에는 DB 값을 씁니다.
- 근거 카드는 FastAPI `POST /drugs/cabinet-check`로 약장 전체를 한 번에 점검하고, 약별 결과를 `analysis_results`에 남깁니다. 직전 스냅샷과 판정이 같으면 저장하지 않으므로 판정이 바뀐 시점만 쌓입니다.
