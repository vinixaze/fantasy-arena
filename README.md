# Fantasy Arena

Plataforma de **fantasy game multiesporte**: o usuário monta a escalação de cada evento (corrida, rodada, card), entra em ligas com amigos e acompanha o ranking conforme os resultados reais saem.

Inspirada no Cartola, mas com um núcleo independente de esporte: cada modalidade é um módulo plugável com suas próprias regras de escalação e pontuação. A primeira versão traz a **Fórmula 1** com dados reais.

## Como funciona (F1)

- Cada GP é um evento. A escalação é **2 pilotos + 1 equipe**, dentro de um orçamento.
- O mercado fecha no horário da largada.
- Pontuação do piloto: `21 - posição final` + posições ganhas em relação ao grid; abandono vale `-10`. A equipe soma seus dois pilotos.
- Depois da corrida, o resultado é buscado automaticamente, os pontos são calculados e os **preços se ajustam** ao desempenho (média móvel). Ao subir pela primeira vez, as corridas já disputadas na temporada são processadas em ordem, o que define os preços iniciais.

## Arquitetura

```
React (Vite)  ──►  Spring Boot REST API  ──►  PostgreSQL
                        │
                        ├── núcleo: usuários (JWT), ligas, escalações, ranking, preços
                        ├── SportModule  ◄── contrato de cada esporte
                        │       └── F1Module ──► OpenF1 (cliente com rate limit)
                        └── SyncService (@Scheduled): eventos, elenco, resultados
```

O núcleo só conhece a interface `SportModule`:

```java
public interface SportModule {
    String code();
    LineupRules lineupRules();                       // orçamento e vagas por tipo
    List<EventData> fetchEvents(int season);
    List<AthleteData> fetchAthletes(String eventId);
    Map<String, ScoreLine> fetchScores(String eventId); // vazio até o resultado sair
}
```

Para adicionar um esporte basta implementar essa interface e registrá-la como bean; usuários, ligas, ranking, travamento de mercado e preços funcionam sem alteração.

## Stack

- **Back-end:** Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, Flyway, PostgreSQL, springdoc (OpenAPI)
- **Front-end:** React 19, TypeScript, Vite
- **Infra:** Docker Compose, nginx

## Rodando

Com Docker:

```bash
docker compose up --build
# app: http://localhost:3000
# API docs: http://localhost:8080/swagger-ui.html
```

Para desenvolvimento:

```bash
docker compose up db -d
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev   # http://localhost:5173
```

Variáveis úteis: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_USERNAME`,
`ADMIN_PASSWORD` e `SPORTMONKS_API_TOKEN` (veja `.env.example`). O usuário definido por
`ADMIN_USERNAME` é criado/promovido no início da aplicação e recebe acesso ao painel administrativo.

Testes: `cd backend && mvn test`.

## API

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | Cadastro e login (retorna JWT) |
| GET | `/api/sports` | Esportes e regras de escalação |
| GET | `/api/sports/{code}/events` | Eventos da temporada |
| GET | `/api/sports/{code}/athletes` | Atletas e preços atuais |
| GET/PUT | `/api/events/{id}/lineup` | Ver / salvar a escalação do usuário |
| GET | `/api/lineups/mine` | Histórico de rodadas |
| GET/POST | `/api/leagues` | Minhas ligas / criar liga |
| POST | `/api/leagues/join` | Entrar com código de convite |
| GET | `/api/leagues/{id}/ranking` | Ranking da liga |
| GET | `/api/auth/me` | Perfil e papel do usuário autenticado |
| GET | `/api/admin/dashboard` | Indicadores do painel (ADMIN) |
| POST | `/api/admin/sync` | Sincronização manual das modalidades (ADMIN) |
| GET/POST/PUT | `/api/admin/ufc/fighters` | Elenco manual do UFC (ADMIN) |
| GET/POST/PUT | `/api/admin/ufc/events` | Eventos manuais do UFC (ADMIN) |
| POST | `/api/admin/ufc/events/{id}/score` | Publicar pontuação do UFC (ADMIN) |

## Dados

- **F1:** [OpenF1](https://openf1.org), API aberta e gratuita (sem chave; limite de 3 req/s e 30 req/min no plano gratuito, respeitado pelo cliente). Dados de 2023 em diante. Projeto não oficial e sem vínculo com a Fórmula 1.
- **Futebol:** SportMonks Football API v3. Configure `SPORTMONKS_API_TOKEN`; sem token a modalidade
  permanece visível, mas não sincroniza eventos. A cobertura depende do plano associado ao token.
- **UFC:** dados, eventos e pontuações cadastrados manualmente pelo painel administrativo.
- O sync roda a cada hora (`arena.sync.interval-ms`).

## Roadmap

- [ ] Segundo esporte (candidatos: futebol e NBA, dependem de fonte de dados com estatística por jogador)
- [ ] Ranking em tempo real (SSE)
- [ ] Testes de integração com Testcontainers
- [ ] Pontuação por sessão (classificação e sprint)
- [ ] Notificações de fechamento de mercado

## Aviso

Projeto de estudo. Usa apenas pontos virtuais, sem dinheiro real nem apostas.
