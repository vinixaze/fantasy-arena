# Fantasy Arena

Fantasy game multiesporte em que usuários montam escalações para eventos reais, participam de ligas privadas e acompanham o ranking conforme os resultados são processados.

O projeto possui um núcleo independente de modalidade. Cada esporte implementa suas próprias regras de escalação, fonte de dados e pontuação por meio da interface `SportModule`.

## Funcionalidades

- cadastro e autenticação com JWT;
- escalações por evento, respeitando orçamento e posições de cada modalidade;
- fechamento automático do mercado no início do evento;
- criação de ligas privadas e entrada por código de convite;
- ranking das ligas e histórico de escalações;
- atualização de preços dos atletas com base no desempenho;
- sincronização automática de eventos, elencos e resultados;
- painel administrativo com indicadores, usuários, auditoria e sincronização manual;
- gerenciamento manual de lutadores, eventos e pontuações do UFC.

## Modalidades disponíveis

### Fórmula 1

- Dados obtidos pela [OpenF1](https://openf1.org).
- Escalação: **2 pilotos e 1 equipe**, com orçamento de 100 pontos.
- Pontuação do piloto: `21 - posição final`, acrescida das posições ganhas em relação ao grid.
- Abandono vale `-10`; a equipe soma a pontuação dos seus dois pilotos.
- Eventos, participantes e resultados são sincronizados automaticamente.

### Futebol

- Dados obtidos pela SportMonks Football API v3.
- Escalação: **1 goleiro, 4 defensores, 4 meio-campistas e 2 atacantes**, com orçamento de 100 pontos.
- A pontuação considera gols, assistências, cartões e pênaltis perdidos ou defendidos.
- Requer `SPORTMONKS_API_TOKEN`. Sem o token, a modalidade permanece visível, mas não sincroniza dados.
- A disponibilidade de campeonatos e partidas depende do plano contratado na SportMonks.

### UFC

- Escalação de **5 lutadores**, com orçamento de 100 pontos.
- Lutadores, eventos e pontuações são cadastrados manualmente pelo painel administrativo.
- Não depende de uma API externa.

## Arquitetura

```text
React + Vite ──► nginx ──► Spring Boot REST API ──► PostgreSQL
                                  │
                                  ├── autenticação, ligas, escalações e ranking
                                  ├── F1Module ───────► OpenF1
                                  ├── FootballModule ► SportMonks
                                  ├── UfcModule ─────► administração manual
                                  └── SyncService ───► sincronização agendada
```

O núcleo se comunica com cada modalidade pelo contrato:

```java
public interface SportModule {
    String code();
    String displayName();
    LineupRules lineupRules();
    List<EventData> fetchEvents(int season);
    List<AthleteData> fetchAthletes(String eventId);
    Map<String, ScoreLine> fetchScores(String eventId);
}
```

Uma nova modalidade pode ser adicionada implementando essa interface e registrando a implementação como um bean do Spring.

## Tecnologias

- **Back-end:** Java 21, Spring Boot 3.5, Spring Security, JWT, Spring Data JPA, Flyway e OpenAPI;
- **Front-end:** React 19, TypeScript 7 e Vite 8;
- **Banco de dados:** PostgreSQL 16;
- **Infraestrutura:** Docker, Docker Compose e nginx.

## Executando com Docker

### Pré-requisitos

- Docker;
- Docker Compose.

Clone o repositório e entre na pasta:

```bash
git clone https://github.com/vinixaze/fantasy-arena.git
cd fantasy-arena
```

Crie o arquivo local de configuração:

```bash
cp .env.example .env
```

Edite o `.env`, defina senhas seguras e, se desejar usar Futebol, informe o token da SportMonks. Depois, inicie os serviços:

```bash
docker compose up --build
```

Endereços locais:

- aplicação: <http://localhost:3000>;
- API: <http://localhost:8080>;
- documentação Swagger: <http://localhost:8080/swagger-ui.html>.

O Docker Compose inicia três serviços: PostgreSQL, API Spring Boot e frontend servido pelo nginx.

## Executando para desenvolvimento

Inicie apenas o banco de dados:

```bash
docker compose up db -d
```

Execute o back-end:

```bash
cd backend
mvn spring-boot:run
```

Em outro terminal, execute o frontend:

```bash
cd frontend
npm install
npm run dev
```

Durante o desenvolvimento, o frontend estará disponível em <http://localhost:5173>.

## Configuração

| Variável | Finalidade | Obrigatória |
|---|---|---|
| `DB_URL` | URL JDBC do PostgreSQL | Não, possui valor local padrão |
| `DB_USER` | Usuário do banco | Não, possui valor local padrão |
| `DB_PASSWORD` | Senha do banco | Recomendada |
| `JWT_SECRET` | Chave de assinatura dos tokens | Sim em produção |
| `ADMIN_USERNAME` | Usuário promovido/criado como administrador | Não |
| `ADMIN_PASSWORD` | Senha inicial do administrador | Sim ao criar o administrador |
| `SPORTMONKS_API_TOKEN` | Token da API de Futebol | Apenas para sincronizar Futebol |
| `SPORTMONKS_BASE_URL` | Endereço-base da SportMonks | Não |

Nunca envie o arquivo `.env` ao repositório. O projeto inclui somente o `.env.example`, sem credenciais reais.

## Sincronização

O `SyncService` executa na inicialização e, por padrão, repete o processo a cada hora. Ele:

1. atualiza os eventos de cada modalidade;
2. atualiza os atletas disponíveis;
3. procura eventos finalizados ainda não pontuados;
4. calcula as pontuações, atualiza preços e rankings.

O intervalo pode ser alterado pela propriedade `arena.sync.interval-ms`. Administradores também podem iniciar a sincronização pelo painel.

## Principais endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/auth/register` | Cadastro de usuário |
| `POST` | `/api/auth/login` | Login e emissão do JWT |
| `GET` | `/api/auth/me` | Perfil autenticado |
| `GET` | `/api/sports` | Modalidades e regras de escalação |
| `GET` | `/api/sports/{code}/events` | Eventos da modalidade |
| `GET` | `/api/sports/{code}/athletes` | Atletas e preços atuais |
| `GET`, `PUT` | `/api/events/{id}/lineup` | Consulta ou alteração da escalação |
| `GET` | `/api/lineups/mine` | Histórico de escalações |
| `GET`, `POST` | `/api/leagues` | Consulta ou criação de ligas |
| `POST` | `/api/leagues/join` | Entrada em uma liga por convite |
| `GET` | `/api/leagues/{id}/ranking` | Ranking da liga |
| `GET` | `/api/admin/dashboard` | Indicadores administrativos |
| `GET` | `/api/admin/users` | Consulta de usuários |
| `PATCH` | `/api/admin/users/{id}/role` | Alteração do papel de um usuário |
| `POST` | `/api/admin/sync` | Sincronização manual |
| `GET`, `POST`, `PUT` | `/api/admin/ufc/fighters` | Gerenciamento de lutadores |
| `GET`, `POST`, `PUT` | `/api/admin/ufc/events` | Gerenciamento de eventos do UFC |
| `POST` | `/api/admin/ufc/events/{id}/score` | Publicação da pontuação do UFC |
| `GET` | `/api/admin/audit` | Histórico recente de ações administrativas |

Os endpoints administrativos exigem um usuário com papel `ADMIN`.

## Testes e build

Back-end:

```bash
cd backend
mvn test
```

Frontend:

```bash
cd frontend
npm ci
npm run build
```

## Estado atual e próximos passos

O projeto já possui fluxo funcional para F1, Futebol e UFC. Melhorias planejadas:

- ampliar a cobertura de testes com testes de integração e Testcontainers;
- adicionar atualização de ranking em tempo real;
- suportar pontuação separada para classificação e sprint na F1;
- enviar notificações antes do fechamento do mercado;
- adicionar CI/CD, observabilidade e uma estratégia de implantação em nuvem.

## Aviso

Projeto de estudo. Todos os pontos são virtuais; não há apostas nem movimentação de dinheiro real. O projeto não possui vínculo oficial com Fórmula 1, UFC, OpenF1 ou SportMonks.
