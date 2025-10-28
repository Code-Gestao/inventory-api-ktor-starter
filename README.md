
# Inventory API (Ktor + Kotlin)

## Rodar (Windows)
1) Instale Java JDK 17.
2) Defina as variáveis de ambiente do Postgres (use as credenciais da Railway):
   - DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
3) No terminal, dentro do projeto:
   - `./gradlew.bat run`

A API sobe em `http://localhost:8080`.
Rotas: /health, /v1/sessions, /v1/sessions/{id}/items, /v1/sessions/{id}.
