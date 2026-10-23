# Liga Coach

**O que treinar e quais rounds rever, a partir das partidas do time.**

Análise pós-partida de VALORANT para times universitários. Para cada time, a plataforma mede 12 situações de jogo em cada mapa e as compara com a liga (ou com os 25% melhores times). O resultado é uma frase de coaching com o principal ponto de atenção, o nível de evidência estatística e a **fila de VOD**: os rounds exatos para rever.

> *Do número ao round que precisa ser revisto.*

<!-- IMAGEM PRINCIPAL: tela inicial com um time aberto (recomendação no topo, cartões e comparativo). Largura sugerida: 1440 px. -->
![Visão geral da Liga Coach](docs/img/01-visao-geral.png)

Projeto do Hackathon VALORANT (SEMCOMP / USP São Carlos, 19–23/10/2026).


> **Regra do hackathon:** os únicos dados de partida usados são os JSON fornecidos pela organização. Nada de APIs externas, scraping ou integração com a Riot.

---

## O problema e a ideia

Times universitários têm pouco tempo para rever partidas. Estatística de jogo costuma parar no número ("seu pós-plant é 62%") e deixa o treinador sozinho para decidir o que fazer com ele.

A Liga Coach responde três perguntas, nesta ordem:

1. **Onde o time perde mais rounds do que a referência?** Situações ordenadas pelo que está em jogo.
2. **Posso confiar nesse número?** Cada taxa vem com amostra, intervalo de confiança e veredito.
3. **Quais rounds eu assisto?** A fila de VOD lista partida, mapa e número do round, com botão para copiar a referência.

## O que a plataforma entrega

- **Recomendação de treino:** a frase de coaching com o principal ponto de atenção, o foco sugerido e a quantidade de rounds para rever.
- **Assistente tática da Killjoy:** responde sobre plano de treino, pontos fortes, mapas, derrotas e rounds para rever usando a análise local.
- **Indicadores principais:** first blood convertido, pós-plant e retake, cada um com a diferença para a referência e o veredito.
- **Comparativo equipe × referência** nas seis situações centrais (first blood, pós-plant, retake, pistol, ataque e defesa).
- **Melhores e piores desempenhos**, só com o que tem evidência estatística.
- **Análise por mapa e lado:** taxa e rounds em ataque e defesa, mapa a mapa.
- **Fila de VOD** com seletor de situação e botão de copiar a referência do round.
- **Onde os rounds foram perdidos:** fatia das derrotas por tipo de round, comparada com a liga.
- **Página "Como calculamos":** definições, parâmetros estatísticos e referências usadas.
- **Duas referências:** média da liga ou quartil superior (times com 8 mapas ou mais).

## Telas

<!-- Substitua cada arquivo em docs/img/ pelo print correspondente. Dica: use sempre o mesmo time (ex.: ufu_saints) e a mesma referência para as telas ficarem coerentes. -->

| | |
|---|---|
| ![Indicadores e comparativo com a liga](docs/img/02-comparativo.png)<br>**Indicadores e comparativo com a liga** | ![Melhores e piores desempenhos](docs/img/03-melhores-e-piores.png)<br>**Melhores e piores desempenhos** |
| ![Análise por mapa e lado](docs/img/04-mapa-e-lado.png)<br>**Análise por mapa e lado** | ![Fila de VOD com botão de copiar](docs/img/05-fila-de-vod.png)<br>**Fila de VOD** |
| ![Onde os rounds foram perdidos](docs/img/06-onde-perdemos-rounds.png)<br>**Onde os rounds foram perdidos** | ![Página Como calculamos](docs/img/07-como-calculamos.png)<br>**Como calculamos** |

**No celular:**

<!-- Print com a janela em 375 px de largura (F12 > Ctrl+Shift+M). -->
<img src="docs/img/08-mobile.png" alt="Liga Coach no celular" width="280">

**Demonstração:** <!-- Link do vídeo de demonstração, se houver -->

**Site publicado:** <!-- Link do protótipo publicado (Netlify, etc.) -->

---

## Por que dá para confiar nos números

- **Base validada:** os abates limpos batem com o placar oficial em 3360 de 3360 linhas (reconciliação feita a cada subida do backend).
- **Intervalo de confiança** (Wilson, 95%) em toda taxa mostrada, e a amostra sempre visível.
- **Sem conclusões apressadas:** menos de 20 ocorrências é "amostra pequena". A cor de destaque só aparece quando a diferença para a referência tem evidência.
- **Múltiplas comparações controladas:** a "evidência forte" resiste ao ajuste de Benjamini-Hochberg sobre todos os itens do time.
- **Referência justa:** a referência exclui o próprio time e, no geral, é ponderada pelos mapas que ele jogou.
- **Testes de ouro:** o Java é comparado, número a número, com cálculos independentes feitos em Python sobre a base real.

### Método em uma tela

| Conceito | Como é calculado |
|---|---|
| Situação | Uma regra por classe em `metrics/situations` (ex.: "pós-plant": rounds de ataque com spike plantada; sucesso = vencer) |
| Referência | Liga inteira ou quartil superior (times com ≥ 8 mapas), **sem o próprio time**. No geral, é a média das taxas por mapa, ponderada pelos mapas que o time jogou |
| Intervalo | Wilson, 95% |
| Indício | Referência fora do intervalo (teste de score, p < 0,05) |
| Evidência forte | Continua significativo após Benjamini-Hochberg (FDR 10%) sobre todos os itens do time |
| Amostra pequena | Menos de 20 ocorrências |
| Saldo ("em jogo") | (taxa da referência − taxa do time) × ocorrências |

---

## Arquitetura

```
data/**/*.json → MatchFileName (nome do time vem do nome do arquivo)
              → RawMatch (Jackson) → MatchAssembler (limpeza) → Match/Round/Kill (memória)
              → SituationEngine (12 situações, por time e por mapa)
              → OpportunityService (referência por mapa, Wilson, Benjamini-Hochberg, ranking)
              → RecommendationService (frase por template) · LossBreakdownService
              → /api/... → React
```

<!-- Opcional: insira aqui um diagrama da arquitetura. -->
<!-- ![Arquitetura](docs/img/09-arquitetura.png) -->

**Regras de limpeza da base** (implementadas no `MatchAssembler`):

- O nome do time só existe no nome do arquivo; o lado (Red/Blue) com `roundsWon` igual ao placar A é o time A.
- Observadores (`isObserver=true`, `stats=null`) são removidos.
- Autoabates (bomba, queda) e fogo amigo saem dos eventos de abate, e com isso os abates batem 100% com `stats.kills`.
- O lado atacante de cada round vem de `winningTeam` + `winningTeamRole`.
- `roundNum` é 0-based; a fila de VOD mostra 1-based.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Spring Boot 4.1 · Java 21 · API REST (sem banco: tudo em memória) |
| Frontend | React 19 · Vite · Tailwind 4 · lucide-react |
| Análise da base | Scripts Python em `tools/analise/` |
| Entrega | Site estático, jar único ou Docker |

## Estrutura do repositório

```
.
├── backend/          API REST + motor de métricas
├── frontend/         Interface React
├── data/             JSON da organização (336 partidas)
├── docs/             Estratégia, pitch, tarefas e imagens (docs/img)
├── tools/analise/    Scripts Python que geraram os "números de ouro" dos testes
├── scripts/          Preparar dados, gerar snapshot estático
└── Dockerfile        Imagem única (front + API + base)
```

---

## Como rodar

**Pré-requisitos:** JDK 21 (o Maven vem pelo `mvnw`) e Node 20+. Python 3 é opcional (só para `tools/analise`).

A base de partidas já vem no repositório, em `data/`. Não há download nem preparo de dados.

```bash
git clone https://github.com/treles/onetap.git
cd onetap/backend && ./mvnw spring-boot:run      # terminal 1: API em http://localhost:8080
cd onetap/frontend && npm install && npm run dev  # terminal 2: tela em http://localhost:5173
```

No Windows (PowerShell), use `.\mvnw.cmd spring-boot:run` no lugar de `./mvnw spring-boot:run`.

O log do backend deve mostrar:

```
336 partidas, 6618 rounds, 0 arquivos rejeitados ... reconciliacao de kills 3360/3360
```

Links diretos funcionam, por exemplo `#time=octacore_python&ref=top25`. Se o backend estiver em outra porta, rode o front com `PAUTA_API=http://localhost:8092 npm run dev`. Para usar outra pasta de dados, defina `PAUTA_DATA_DIR`; para atualizar a base a partir dos zips do edital, há o `scripts/preparar-dados.ps1`.

## Testes

```bash
cd backend
./mvnw test
```

São 47 testes. O `GoldenNumbersTest` compara o Java com os valores calculados pelos scripts Python sobre a base real (referências da liga, números de times, identidades de soma, referência por mapa, Benjamini-Hochberg). Como a base está no repositório, ele roda em qualquer clone e no CI.

## API

| Rota | Retorno |
|---|---|
| `GET /api/health/data` | Integridade da base (arquivos, rounds, abates, reconciliação) |
| `GET /api/teams` | Times com nome legível, mapas e win rate |
| `GET /api/teams/{team}/report?benchmark=league\|top25` | Visão geral, recomendação, pontos de atenção, pontos fortes e todas as situações |
| `GET /api/teams/{team}/situations/{id}?benchmark=` | A situação no geral e por mapa |
| `GET /api/teams/{team}/situations/{id}/vod?map=` | Fila de VOD (rounds sem sucesso) |
| `GET /api/teams/{team}/losses` | Onde os rounds foram perdidos (categorias excludentes) vs liga |
| `GET /api/methodology` | Definições, referências da liga e parâmetros estatísticos |

Erros: `{ "code", "message", "timestamp" }` (`TEAM_NOT_FOUND`, `SITUATION_NOT_FOUND`, `INVALID_PARAMETER`).

## Como adicionar uma situação

1. Crie uma classe em `backend/src/main/java/br/pauta/metrics/situations/` que implemente `Situation`, anotada com `@Component`. Use `PostPlant.java` como modelo.
2. Escreva em `definition()` exatamente o que é contado e o que é "sucesso": o texto aparece para o usuário.
3. Adicione os números de ouro no `GoldenNumbersTest`.
4. Pronto: o `SituationEngine` encontra a classe sozinho, e ela aparece na pauta e na fila de VOD.

## Publicar o protótipo

### Opção A: site estático (não precisa de servidor)

O snapshot contém só agregados e referências de rounds, não a base bruta.

```bash
cd backend && ./mvnw spring-boot:run          # terminal 1
node scripts/gerar-snapshot.mjs                # terminal 2: gera frontend/public/snapshot (~10 MB)
cd frontend && npm run build:static            # gera frontend/dist
```

Publique a pasta `frontend/dist` em qualquer hospedagem estática, como o Netlify Drop (app.netlify.com/drop).

### Opção B: jar ou Docker (com servidor)

```bash
cd frontend && npm run build:jar && cd ../backend && ./mvnw package && java -jar target/pauta.jar
```

Ou `docker build -t pauta . && docker run -p 8080:8080 pauta` (a imagem já inclui a base).

## Próximos passos

- Análise individual por jogador (dano por round, % de headshot), a partir do bloco de dano que o JSON já traz.
- Desempenho por arma e agente, dependendo de uma tabela de tradução dos identificadores (o JSON traz só UUIDs).
- Consistência entre partidas e tendência ao longo do campeonato.

## Front-end e créditos

O visual parte do protótipo do time, aplicado sobre o app Vite + React e alimentado pela API real: nenhum número é fixo no código. As imagens dos mapas ficam em `frontend/src/assets/maps/` (veja o `LEIA-ME.md` dessa pasta).

**Equipe:** <!-- Nome 1 · Nome 2 · Nome 3 -->

**Aviso:** projeto independente, feito só com os arquivos fornecidos pela organização do Hackathon VALORANT. Não é afiliado à Riot Games, e VALORANT é marca registrada da Riot Games, Inc.
