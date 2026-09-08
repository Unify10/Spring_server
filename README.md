# Unify - server

## 환경 변수 설정 (.env)

DB 접속 정보는 `.env` 파일로 관리합니다.

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

`.env`는 `.gitignore`에 등록되어 커밋되지 않으며, 값은 `application.yaml`에서 `${DB_HOST:localhost}` 형태로 참조합니다.
