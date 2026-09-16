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

`.env`는 `.gitignore`에 등록되어 커밋되지 않으며, 값은 `application.yaml`에서 `${DB_HOST:localhost}` 형태로 참조합니다. 기본값이 없는 변수(`MAIL_*`, `JWT_SECRET`)는 반드시 채워야 앱이 정상적으로 시작됩니다.
