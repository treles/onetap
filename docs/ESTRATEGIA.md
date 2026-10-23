# Hackathon VALORANT — Estratégia, arquitetura e plano de execução

> **Atualização (21/10): o método evoluiu desde este documento; o código e o [PITCH.md](PITCH.md) são a referência atual.**
> 1. A referência agora é **ajustada aos mapas que o time jogou** e exclui o próprio time. Com isso, o "ataque comprovado" da octacore_python (seção 4) **deixa de ter evidência no geral**. O problema real aparece em **Ascent**: depois de sofrer o primeiro abate lá, o time venceu 2 de 38 rounds (5% contra 29%), com evidência forte.
> 2. Os vereditos passaram a ter três níveis: indício (fora do IC 95%), evidência forte (após correção de Benjamini-Hochberg para múltiplas comparações) e amostra pequena.
> 3. Entraram o referencial "quartil superior", a visão "onde os rounds foram perdidos", a recomendação em frase e a página "Como calculamos".

> Base analisada de verdade: **336 partidas, 6.618 rounds, 46.753 eventos de abate, 413 jogadores, 68 nomes de time**, extraídos dos 4 zips em `Downloads` (Rush Series Esquenta, Esquenta 2, Inclusivo e Univavá C1–C4 + Fase de Grupos).
> Todos os números deste documento foram calculados pelos scripts em [tools/analise/](../tools/analise/) — nada aqui é suposição sobre o formato.

---

## 1. Diagnóstico do seu perfil

| Competência | Nível evidenciado | Como usar no hackathon |
|---|---|---|
| Java + Spring Boot, APIs REST | **Profissional** (Bradesco) | Núcleo do produto: ingestão, regras, métricas, API |
| Regras de negócio, validações | **Profissional** | Cada indicador vira uma "regra" testável, com definição explícita |
| Testes, migração para Xray | **Profissional** | Testes com "números de ouro" (valores esperados calculados independentemente) |
| Logs, observabilidade, investigação de bugs | **Profissional** | Relatório de integridade da base na inicialização — vira argumento de confiabilidade no pitch |
| Métricas e dashboards operacionais | **Contato profissional** | Você já sabe que "número sem contexto" não ajuda — é exatamente o problema do desafio |
| React / Next.js / Tailwind / JS | **Projetos pessoais/acadêmicos** | Interface simples; **risco médio** — por isso o escopo de front é pequeno |
| Análise de dados / estatística | **Lacuna declarada** | Usar só estatística simples e explicável (proporções, intervalo de confiança). Explico tudo na seção 9 |
| Machine learning | **Lacuna** | **Não usar.** Não é necessário e aumentaria o risco |

**Ambiente verificado na sua máquina:** JDK 21 (`~/.jdks/ms-21.0.11`), JDK 25 e 26, Node 24, Python 3.13 e um `~/.m2` com wrapper do Maven. Ou seja, nada precisa ser instalado para começar (o Maven vem pelo `mvnw` do projeto).

**Ponto forte que diferencia você:** a maioria das equipes vai fazer um notebook ou um dashboard bonito com números soltos. Seu diferencial natural é **engenharia confiável**: definição precisa de cada indicador, testes, validação da base e honestidade sobre amostra. A banca é **Riot Games + AcadArena (organizadora do UNIVAVÁ)** — gente que conhece os dados e vai perceber número errado.

**Lacunas e como neutralizá-las:**
- Estatística → só proporções + intervalo de Wilson (uma fórmula, explicada abaixo).
- Front-end → uma única tela principal, componentes de tabela/barra, sem mapas 2D nem animações.
- Tempo → você trabalha durante o dia; o plano assume **~5–6 h úteis por dia** de hackathon (ajuste se tiver mais).

---

## 2. Resumo do desafio

- **Problema:** transformar dados de partidas em informação útil para evolução individual e **coletiva** (coletiva é explicitamente valorizada).
- **Público:** jogadores, treinadores, analistas e times universitários (os dados são do circuito universitário: UNIVAVÁ/Rush Series).
- **Restrições:** só os JSON fornecidos; nada de APIs externas, scraping ou integração com Riot; foco pós-partida; respeitar licenças.
- **Entregáveis (até 23/10, 23h59):** vídeo pitch ≤ 5 min (YouTube não listado), link da apresentação, link do protótipo (opcional, mas conta pontos), via Formulário Google.
- **Critérios (1–5 cada, mesmo peso, 30 pts):** Inovação · Impacto · **Qualidade da análise e do uso dos dados** · Viabilidade · Usabilidade · Qualidade do protótipo e do pitch.

Observação: três dos seis critérios (análise, viabilidade, protótipo) recompensam diretamente o seu perfil de engenharia.

---

## 3. Comparação de ideias

### Ideia A — "Painel do Time" (dashboard estatístico)
1. **Problema:** time não tem visão consolidada dos próprios números.
2. **Público:** jogadores. 3. **Valor:** ver K/D, ADR, win rate por mapa/agente.
4. **Funcionalidades:** filtros por time/mapa/jogador, gráficos.
5. **Dados:** `players[].stats`, `teams[]`, nome do mapa. 6. **Tech:** Spring + React + gráficos.
7. **Complexidade:** baixa. 8. **Riscos:** baixo técnico, **alto de ser igual a todo mundo**.
9. **Diferencial:** quase nenhum — é o "dashboard genérico" que o enunciado critica.
10. **MVP até 23/10:** muito viável.

### Ideia B — "Pauta de Treino": do número ao round que precisa ser revisto ✅
1. **Problema:** o treinador vê dezenas de números e não sabe **o que treinar primeiro** nem **quais rounds rever no VOD**.
2. **Público:** treinador/analista/capitão de time universitário.
3. **Valor:** para um time, ranqueia as **situações de jogo** (pós-plant, retake, reação ao first blood, trade, eco, pistol, ataque por mapa…) pelo **número de rounds em jogo** comparado a um referencial, com **tamanho de amostra e intervalo de confiança**, e entrega a **fila de VOD**: "partida X, round Y" de cada round perdido naquela situação.
4. **Funcionalidades:** seletor de time → pauta ranqueada → detalhamento (por mapa/lado) → fila de VOD.
5. **Dados:** eventos de kill com tempo, economia por jogador por round, plant/defuse, lado vencedor por round, nome do time e mapa via nome do arquivo.
6. **Tech:** Spring Boot (motor de métricas + API) + React/Vite (uma tela).
7. **Complexidade:** média (as regras são o trabalho; a UI é simples).
8. **Riscos:** definição dos indicadores (mitigado: já prototipados e validados neste documento); amostras pequenas (mitigado: IC + selo de "amostra pequena").
9. **Diferencial:** recomendação **transparente e auditável**, sem IA, que **aponta a evidência** (os rounds). Diz quando *não há* evidência suficiente — honestidade estatística que a banca valoriza.
10. **MVP até 23/10:** viável — o protótipo em Python já roda (ver seção 5.6).

### Ideia C — "Scout de Adversário / Preparação de Confronto"
1. **Problema:** preparar-se para um oponente específico.
2. **Público:** treinador. 3. **Valor:** tendências do adversário (sites de plant, pistol, mapas fortes).
4. **Funcionalidades:** comparador time × time, veto de mapas sugerido.
5. **Dados:** os mesmos de B. 6. **Tech:** idem.
7. **Complexidade:** média. 8. **Riscos:** **amostra por adversário muito pequena** (metade dos times tem ≤ 8 partidas; por mapa fica em 1–3 jogos) → conclusões frágeis; foco em "vencer o outro" se afasta de "evolução do próprio time".
9. **Diferencial:** bom para pitch, fraco em confiabilidade. 10. **MVP:** viável, mas com conclusões pouco defensáveis.

### Ideia D (descartada cedo) — Replay 2D de rounds com posições
Os JSON têm `x,y` de todos os jogadores a cada abate, mas converter isso para a imagem do minimapa exige parâmetros de calibração por mapa que **não estão na base** (teria de buscar externamente → viola a regra). Alto risco, alto custo. Fica fora.

### Matriz comparativa (1–5)

| Critério | A Painel | **B Pauta** | C Scout | Justificativa curta |
|---|---|---|---|---|
| Adequação ao desafio | 3 | **5** | 4 | B responde literalmente "o que treinar" e é coletiva |
| Impacto p/ times | 2 | **5** | 4 | B gera decisão concreta (treino + VOD) |
| Viabilidade c/ seu perfil | 5 | **4** | 4 | B é regra de negócio + API: seu terreno |
| Tempo de implementação | 5 | **4** | 3 | C exige mais telas de comparação |
| Facilidade de demonstrar | 4 | **5** | 4 | "Time → problema → rounds" em 60 s de demo |
| Diferenciação | 1 | **5** | 3 | Ranking por rounds em jogo + fila de VOD é raro |
| Confiabilidade das conclusões | 3 | **4** | 2 | B agrega todas as partidas do time; C fatia demais |
| Risco técnico (5 = baixo) | 5 | **4** | 4 | |
| Qualidade do pitch | 2 | **5** | 4 | História clara de problema → evidência → ação |
| **Total** | 30 | **41** | 32 | |

---

## 4. Recomendação principal: **Pauta de Treino**

> **"Seu time perdeu 120 rounds. Quais deles você deveria rever primeiro — e por quê?"**

A Pauta transforma 46 mil eventos em **uma lista curta e priorizada de pontos de treino**, cada um com:
- a situação (ex.: "ataque sem conseguir plantar");
- taxa do time × referencial (liga ou quartil superior);
- **rounds em jogo** = (taxa do referencial − taxa do time) × nº de ocorrências → "se vocês fossem medianos nisso, teriam ~10 rounds a mais";
- amostra e intervalo de confiança (com selo "amostra pequena" / "diferença não comprovada");
- **fila de VOD**: a lista exata de rounds perdidos naquela situação, com partida e número do round.

Por que essa e não as outras: é a única que transforma estatística em **decisão** (o que treinar) **e** em **ação verificável** (quais rounds rever), é coletiva por natureza, e cada recomendação é rastreável até o dado bruto — exatamente o que um engenheiro de backend com mentalidade de auditoria faz bem.

### Exemplo real já calculado (octacore_python, 12 partidas)

| Situação | Time | Liga | n | Rounds em jogo | Leitura |
|---|---|---|---|---|---|
| Rounds de ataque | 41,4% | 51,8% | 99 | **+10,3** | IC 95% [32–51] → **diferença significativa** |
| Reagir a first blood sofrido | 22,3% | 28,7% | 103 | +6,6 | dentro do IC → *não comprovado* |
| Pós-plant (ataque) | 64,4% | 70,8% | 59 | +3,8 | dentro do IC |

Detalhando o ataque:
- **Ascent: 13/45 rounds de ataque (29%)** vs Split 57% e Lotus 48%.
- **40 de 99 ataques (40%) terminaram sem plant**, e nesses o time venceu **3 de 40 (7,5%)**. Na liga, ~30% dos ataques terminam sem plant (4.653 plants em 6.618 rounds).
- Quando planta, o time converte razoavelmente (A 16/26, B 15/21).

**Pauta gerada:** "O problema não é o pós-plant; é **chegar ao plant**, sobretudo em **Ascent**. Treino sugerido: execuções e entradas de site no ataque em Ascent. Revisar primeiro: os 37 rounds de ataque perdidos sem plant (lista)."

Isso é **uma hipótese baseada em evidência**, não um diagnóstico tático: o produto mostra *onde* os rounds se perdem; o *porquê* (utilitária, timing, comunicação) é o treinador que confirma no VOD. O produto deve dizer isso explicitamente.

### Lição já aprendida com os dados (importante para o produto)
Para a **ufu_saints** (time forte, 59% dos rounds ganhos), **todas** as situações ficaram acima da média da liga. Comparar só com a média não gera pauta para times fortes. Solução (barata): o referencial é selecionável — **"média da liga"** ou **"quartil superior"** (25% melhores times por win rate de rounds) — e há uma visão alternativa **"onde perdemos rounds"** (decomposição dos rounds perdidos por situação), que funciona para qualquer time.

---

## 5. Análise da base JSON

### 5.1 Arquivos e organização
| Pasta | Arquivos | Tamanho | Prefixo do arquivo |
|---|---|---|---|
| Rush Series Esquenta | 29 | 17 MB | `RUSHE_` |
| Rush Series Esquenta 2 | 26 | 15 MB | `RUSH2_` |
| Rush Series Inclusivo | 13 | 7 MB | `RUSH_INCLUSIVO_` |
| Univavá / Classificatória 1–4 | 55 / 39 / 43 / 62 | | `UNIVAVA_C1_` … `UNIVAVA_C4_` |
| Univavá / Fase de Grupos | 69 | (Univavá total 156 MB) | `UNIVAVA_G_` |

**Um arquivo = um mapa jogado.** Nome do arquivo:
`UNIVAVA_C1_036_27-06-26_macklogic_red_13_x_5_octacore_javascript_lotus.json`
→ `prefixo _ número(+letra opcional, ex. 014a) _ data(dd-mm-aa) _ timeA _ placarA _x_ placarB _ timeB _ mapa`.
Regex validada nos 336 arquivos sem nenhuma falha:
`^(?<prefix>.+?)_(?<num>\d+[a-z]?)_(?<date>\d\d-\d\d-\d\d)_(?<a>.+)_(?<sa>\d+)_x_(?<sb>\d+)_(?<b>.+)_(?<map>[a-z]+)$`

### 5.2 Estrutura de cada JSON (formato de partida da Riot, sem nomes — só IDs)
```
matchInfo        { matchId, mapId ("/Game/Maps/Jam/Jam"), gameVersion, gameLengthMillis,
                   gameStartMillis, provisioningFlowId ("CustomGame"), isCompleted, isRanked, ... }
players[10|11]   { puuid, gameName, tagLine, teamId ("Red"|"Blue"), partyId, characterId (UUID do agente),
                   isObserver, stats { score, roundsPlayed, kills, deaths, assists, playtimeMillis,
                   abilityCasts { grenadeCasts, ability1Casts, ability2Casts, ultimateCasts } } }
coaches[0..2]    { puuid, teamId }
teams[2]         { teamId, won, roundsPlayed, roundsWon, numPoints }
roundResults[13..36] {
   roundNum (0-based), roundResult ("Eliminated" | "Bomb defused" | "Bomb detonated" | "Round timer expired"),
   roundResultCode, roundCeremony (Default|Flawless|Closer|Clutch|Thrifty|TeamAce|Ace),
   winningTeam, winningTeamRole ("Attacker"|"Defender"),
   bombPlanter, plantSite ("A"|"B"|"C"|""), plantRoundTime, plantLocation, plantPlayerLocations[],
   bombDefuser, defuseRoundTime, defuseLocation, defusePlayerLocations[],
   playerStats[10] {
      puuid, score,
      kills[]  { timeSinceGameStartMillis, timeSinceRoundStartMillis, killer, victim, assistants[],
                 victimLocation{x,y}, playerLocations[{puuid, viewRadians, location{x,y}}],
                 finishingDamage { damageType (Weapon|Ability|Bomb|Fall|Melee|Crush),
                                   damageItem (UUID da arma | "Ultimate" | "Ability1" | ...),
                                   isSecondaryFireMode } },
      damage[] { receiver, damage, headshots, bodyshots, legshots },
      economy  { loadoutValue, weapon (UUID), armor (UUID | ""), remaining, spent },
      ability  { grenadeEffects, ability1Effects, ... }   // sempre null na amostra
   } }
```

### 5.3 Relações entre entidades
- **Partida ↔ times:** dentro do JSON os times são só `Red`/`Blue`. **O nome do time só existe no nome do arquivo.** Mapeamento: o lado cujo `teams[].roundsWon` é igual a `placarA` é o `timeA`. Funcionou nos 336 arquivos (empate é impossível em partida concluída).
- **Jogador ↔ time:** `players[].teamId` + mapeamento acima. Identidade estável entre partidas: `puuid` (413 distintos; 244 aparecem em ≥ 5 partidas).
- **Round ↔ lado de cada time:** não existe campo "lado do time X"; deriva-se de `winningTeam` + `winningTeamRole` (o perdedor tem o papel oposto).
- **Eventos de kill:** ficam dentro de `playerStats[].kills` (do jogador que matou). Para ter a linha do tempo do round, junte todos e ordene por `timeSinceRoundStartMillis`.
- **Mapa:** `mapId` usa codinomes internos; o nome do arquivo traz o nome real. Correspondência verificada pela contagem: Jam=Lotus (82), Triad=Haven (75), Ascent (86), Bonsai=Split (36), Juliett=Sunset (24), Infinity=Abyss (13), Plummet=Summit (10), Foxtrot=Breeze (5), Pitt=Pearl (3), Canyon=Fracture (2).

### 5.4 Qualidade dos dados (problemas encontrados)
| Achado | Volume | Tratamento |
|---|---|---|
| **Observadores** em `players` (`isObserver=true`, `stats=null`, `characterId=null`) | 22 partidas com 11 "jogadores" | Filtrar na ingestão |
| `stats.kills` ≠ contagem de eventos de kill | 344 de 3.382 jogador-partida (sempre +1 a +3 nos eventos) | **Explicado e validado:** os eventos incluem 373 autoabates (bomba 327, queda 40…) e 9 de fogo amigo. Removendo-os, **3.360/3.360 batem 100%** |
| `bombPlanter`/`bombDefuser`/`*PlayerLocations` nulos | 1.965 / 5.269 rounds | Esperado (round sem plant/defuse) — tratar como ausência, não erro |
| `armor = ""` | 10.555 linhas de economia | Sem colete (eco) |
| `roundResultCode = ""` | 150 rounds | Equivale a "Round timer expired" |
| Arma vazia na economia | 16 de 66.180 linhas | Ignorar |
| Rounds sem nenhum abate | 24 | Excluir de métricas que dependem de first blood |
| `ability.*Effects` | sempre `null` | Inútil — ignorar |
| Prorrogação | até 36 rounds | Rounds ≥ 24 são OT; pistols são rounds 0 e 12 |
| Elencos repetidos entre nomes de time | `fei_whiteowls`↔`fei_darkowls` (6 jogadores em comum), `unicamp_tritons_red`↔`_black` (4), `caap_momentum`↔`caap_malditos` (3) | Manter o nome do arquivo como identidade do time (é a convenção da organização) e **mostrar o elenco**; não fundir automaticamente |
| Nomes parecidos mas elencos diferentes | `ceub_octopus` × `ceub_octopus_vulgaris`, `usp_stars` × `usp_stars_gc`, `ufu_saints` × `_auryn` × `_trigons` | São times distintos (sem sobreposição relevante) |
| Duplicidade de partidas | 0 `matchId` duplicados; `014a`/`014b` são mapas diferentes da mesma série | OK |
| Pastas com acento (`Univavá`, `Classificatória`) | — | Derive o torneio do **prefixo do arquivo**, não do nome da pasta (evita problema de encoding) |

### 5.5 O que está presente, o que é calculável e o que não dá

**Direto no JSON:** placar, vencedor e tipo de fim de cada round, lado vencedor, site e tempo do plant, abates (quem, quem, quando, com o quê, assistências), dano por alvo com HS/corpo/perna, economia de cada jogador no round, agente, K/D/A por partida, cerimônias (clutch, ace, flawless, thrifty).

**Calculável (validado nos scripts):**
- Win rate de round por lado/mapa; pistol; anti-eco; force/eco vs full.
- **Tipo de compra do time** (soma do `loadoutValue` dos 5): eco < 5 k · semi < 15 k · semi-full < 20 k · full ≥ 20 k. A matriz da liga é coerente e monotônica (eco vs full 11%; semi vs full 22,5%; semi-full vs full 41,5%; full vs full 50%) — evidência de que os limiares fazem sentido.
- **First blood:** quem o faz vence 71,3% dos rounds (n = 6.584).
- **Trade** (abatedor do primeiro morto é morto em ≤ 5 s): 20,2% na liga.
- Pós-plant 70,8% / retake 29,2% (somam 100%, checagem de consistência).
- Clutch 1vX (X ≥ 2): 6,4% na liga.
- **KAST** (71,1% médio), **ADR** (139,9), **HS%** (25,7%), first kills / first deaths por jogador.
- Composição por função (duelista/controlador/iniciador/sentinela) — requer tabela estática UUID → agente → função.

**Hipóteses (precisam do VOD para confirmar):** *por que* um time falha em chegar ao plant, qualidade de utilitária, comunicação, posicionamento.

**Não obtível com segurança:** heatmap sobre o minimapa (falta calibração de coordenadas → fora das regras), efeitos de habilidade (sempre nulos), ranking/elo (`competitiveTier = 0`, são partidas custom), qualquer dado fora destes torneios.

### 5.6 Metadados estáticos (nomes de agentes e armas)
Os JSON trazem só UUIDs. Os nomes **não são dados de partida**, são metadados do jogo; mesmo assim, **pergunte à monitoria (ticket no Discord, 9h–18h)** se uma tabela estática UUID → nome embutida no projeto é aceitável. Mapeamento que conheço com segurança (confira os marcados com `?`):

Agentes: `8e253930…`=Omen (588 aparições), `320b2a48…`=Sova, `eb93336a…`=Phoenix, `add6443a…`=Jett, `bb2a4828…`=Neon, `dade69b4…`=Fade, `117ed9e3…`=Cypher, `f94c3b30…`=Raze, `22697a3d…`=Chamber, `efba5359…`=Vyse, `1e58de9c…`=Killjoy, `707eab51…`=Viper, `6f2a04ca…`=Skye, `41fb69c1…`=Astra, `7f94d92c…`=Yoru, `5f8d3a7f…`=Breach, `569fdd95…`=Sage, `601dbbe7…`=KAY/O, `95b78ed7…`=Harbor, `1dbf2edd…`=Clove, `9f0d8ba9…`=Brimstone, `a3bfb853…`=Reyna, `b444168c…`=Tejo, `0e38b510…`=Iso, `cc8b64c8…`=Deadlock, `e370fa57…`=Gekko, `df1cb487…`=Waylay `?`, `92eeef5d…`=`?` (1 aparição).

Armas: `9C82E19D…`=Vandal (17.428 abates), `EE8E8D15…`=Phantom (11.259), `E336C6B8…`=Sheriff, `1BAA85B4…`=Ghost, `4ADE7FAA…`=Guardian, `29A0CFAB…`=Classic, `A03B24D3…`=Operator, `AE3DE142…`=Bulldog, `462080D1…`=Spectre, `5F0AAF7A…`=Outlaw, `F7E1B454…`=Stinger, `63E6C2B6…`=Odin, `C4883E50…`=Marshal, `EC845BF4…`=Judge, `910BE174…`=Bucky, `42DA8CCC…`=Shorty, `44D4E95C…`=Frenzy; `410B2E0B…` (949), `856D9A7E…` (481), `95336AE4…` (207) = **a identificar**. Os coletes (3 UUIDs) também: deduza pelo custo na economia ou mostre "leve/pesado" só se tiver certeza.

**A funcionalidade central (Pauta) não depende de nenhum nome de arma ou agente** — isso reduz o risco a zero se a monitoria disser não.

---

## 6. Arquitetura recomendada

### 6.1 Abordagem A × B

| | **A — MVP simples** (script/notebook + página estática) | **B — Aplicação estruturada** (Spring Boot + banco + front separado) |
|---|---|---|
| Vantagem | Rápido para explorar | Robusta, "cara de produto" |
| Desvantagem | Python/pandas não é seu forte; difícil testar e demonstrar como produto | Banco relacional e migrações não agregam nada aqui (dados são estáticos e cabem em memória) |
| Esforço | Baixo no início, alto para virar produto | Alto (schema, carga, ORM, dois deploys) |
| Risco | Médio (stack nova) | Alto (tempo) |

**Decisão: B enxuto ("monólito modular em memória").**
- **Spring Boot, um único serviço**, sem banco: na inicialização lê os 336 JSON com Jackson, ignora o que não é usado (posições etc.), monta um **modelo de domínio compacto em memória** e calcula tudo. 336 arquivos/~200 MB carregam em segundos.
- **Sem microsserviços, sem Kubernetes, sem autenticação, sem JPA.** Um banco só faria sentido se houvesse upload de partidas novas — não há.
- **Front: React + Vite + Tailwind** (não Next.js: não há SSR, rotas complexas ou SEO; Vite evita um segundo servidor). Em produção, o build do React vai para `src/main/resources/static` e **o próprio Spring serve tudo num único `.jar`** → um único deploy, um único link.
- Gráficos: **barras horizontais simples com Recharts** ou até CSS/Tailwind puro. A Pauta é essencialmente uma **tabela ranqueada** — não precisa de gráfico sofisticado.

**Plano B do front (se o React atrasar no dia 3):** Thymeleaf + Tailwind via CDN, renderizado pelo próprio Spring. Você perde interatividade, não perde a demo.

### 6.2 Diagrama
```
 data/**/*.json (336 arquivos, somente leitura)
        │
        ▼
┌──────────────────────── Spring Boot (um processo) ────────────────────────┐
│ ingest/   MatchFileParser (regex do nome) ─┐                               │
│           RawMatch (records Jackson)  ─────┼─► MatchAssembler ─► Match     │
│           DataQualityReport (logs)   ◄─────┘   (domínio compacto)          │
│                                                     │                      │
│ domain/   Match, Round, Kill, TeamSide, BuyType ◄───┘                      │
│                                                     ▼                      │
│ metrics/  Situation (interface)  ── FirstBloodConversion, Trade,           │
│           PostPlant, Retake, PistolRound, EcoVsFull, AttackRounds,         │
│           AttackWithoutPlant, Clutch, ...  →  SituationResult(w, n, refs)  │
│                                                     ▼                      │
│ insight/  Benchmark (liga | quartil superior), WilsonInterval,             │
│           OpportunityService → Pauta ranqueada + fila de VOD               │
│                                                     ▼                      │
│ api/      REST controllers + @RestControllerAdvice (erros padronizados)    │
│ static/   build do React (servido em "/")                                  │
└────────────────────────────────────────────────────────────────────────────┘
        ▲
        │ HTTP/JSON
 React (Vite): TeamPicker → PautaTable → SituationDetail → VodQueue
```

### 6.3 Responsabilidades
| Pacote | Responsabilidade | Paralelo com o Bradesco |
|---|---|---|
| `ingest` | Ler arquivos, validar, mapear Red/Blue → nome, filtrar observadores, autoabates e fogo amigo; emitir relatório de integridade | Camada de integração + validação de entrada |
| `domain` | Objetos imutáveis (records) só com o que o produto usa | Modelo de domínio |
| `metrics` | Uma classe por situação, cada uma com definição documentada e teste próprio | Regras de negócio isoladas e testáveis |
| `insight` | Comparar com referencial, IC, ranking, fila de VOD | Serviço de aplicação |
| `api` | Endpoints, DTOs, tratamento de erros | Controllers REST + handler global |

### 6.4 Endpoints REST essenciais
| Método | Rota | Retorno |
|---|---|---|
| GET | `/api/health/data` | Relatório de integridade (arquivos, rounds, kills, observadores filtrados, reconciliação 100%) |
| GET | `/api/teams` | Lista de times com nº de partidas, rounds, win rate, torneios |
| GET | `/api/teams/{team}/pauta?benchmark=league\|top25` | Pauta ranqueada (situação, taxa, referencial, n, IC, rounds em jogo, veredito) |
| GET | `/api/teams/{team}/situations/{id}` | Detalhe: quebra por mapa e lado + fila de VOD |
| GET | `/api/teams/{team}/players` | KAST, ADR, HS%, FK/FD por jogador (secundário) |

Erros: `404` time inexistente, `400` benchmark inválido, sempre no formato `{ "code", "message", "timestamp" }` via `@RestControllerAdvice`.

### 6.5 Estrutura de pastas
```
pauta/
├── backend/
│   ├── pom.xml
│   ├── data/                      ← copie aqui as 5 pastas dos zips (no .gitignore? ver nota)
│   └── src/
│       ├── main/java/br/pauta/
│       │   ├── PautaApplication.java
│       │   ├── ingest/  MatchFileName.java, RawMatch.java, MatchAssembler.java, MatchRepository.java, DataQualityReport.java
│       │   ├── domain/  Match.java, Round.java, Kill.java, Side.java, BuyType.java
│       │   ├── metrics/ Situation.java, SituationResult.java, FirstBloodConversion.java, ...
│       │   ├── insight/ Wilson.java, Benchmark.java, OpportunityService.java, PautaItem.java
│       │   └── api/     TeamController.java, HealthController.java, ApiErrorHandler.java
│       ├── main/resources/application.properties
│       └── test/java/br/pauta/...  (ver seção 9)
├── frontend/   (Vite + React + Tailwind)
└── analise-dados/  (scripts Python deste documento = "oráculo" de validação)
```
Nota: os JSON são da organização; **não publique a base num repositório público** sem confirmar com a monitoria. Para o deploy, empacote os dados dentro da imagem/servidor e deixe o repositório privado ou com `data/` no `.gitignore`.

### 6.6 Execução e demonstração
- Desenvolvimento: `./mvnw spring-boot:run` (porta 8080) + `npm run dev` no front (Vite com proxy de `/api` para 8080).
- Demo/protótipo: `npm run build` → copiar `dist/` para `backend/src/main/resources/static` → `./mvnw package` → `java -jar target/pauta.jar`.
- Link navegável: um serviço que rode um `Dockerfile` com JRE 21 (ex.: Render, Railway, Fly.io). **Teste o deploy no dia 4, não no dia 5.** Plano B do link: exportar as respostas da API para arquivos `.json` estáticos e publicar só o front (GitHub Pages/Vercel) — o vídeo continua sendo a entrega principal.

---

## 7. Escopo do MVP

**Obrigatório (sem isso não há produto):**
1. Ingestão + relatório de integridade (com a reconciliação 100% de kills).
2. 8 situações: pistol, anti-eco, eco/force vs full, converter first blood, reagir a first blood sofrido, trade do primeiro abatido, pós-plant, retake, **ataque sem plant**, + win rate de ataque/defesa por mapa.
3. Pauta ranqueada por rounds em jogo com IC de Wilson e veredito ("comprovado", "não comprovado", "amostra pequena").
4. Detalhe por mapa/lado + fila de VOD.
5. Uma tela: seleção de time → pauta → detalhe.

**Opcional (só se o obrigatório estiver estável até o dia 4 às 14h):**
- Referencial "quartil superior".
- Visão "onde perdemos rounds" (decomposição).
- Painel por jogador (KAST, ADR, FK/FD) — conecta individual ao coletivo.
- Nomes de agentes/funções (se a monitoria aprovar a tabela estática).
- Resumo em texto gerado **por template** a partir dos números ("O ataque em Ascent é o maior ponto de atenção: …"). Se quiser IA generativa, ela só pode **reescrever** o texto do template com os números já calculados, nunca calcular ou inventar — e é totalmente dispensável.

**Descartado:** mapas 2D/heatmaps, ML, login, banco de dados, upload de partidas, scouting de adversário, previsões de resultado.

---

## 8. Plano de 5 dias

Premissa: ~5–6 h úteis por dia, sozinha. Hoje (19/10) já está quase no fim e **a etapa do dia 1 foi feita neste documento** — você começa o dia 2 com folga.

### Dia 1 — seg 19/10 (noite) — Dados e escopo ✅ / ~1 h restante
- **Objetivo:** confirmar a ideia e as premissas.
- **Tarefas:** ler este documento; rodar um dos scripts de [tools/analise/](../tools/analise/) para ver os números com seus próprios olhos; abrir ticket na monitoria sobre (a) tabela estática UUID→nome e (b) publicar protótipo com a base.
- **Entregável:** escopo congelado (seção 7). **Concluído quando:** você consegue explicar em 2 frases o que é "rounds em jogo".

### Dia 2 — ter 20/10 — Ingestão e núcleo
| Bloco | Tarefa | Entregável verificável |
|---|---|---|
| 1 (1 h) | Criar projeto Spring (start.spring.io: Maven, Java 21, dependência *Spring Web*) | `./mvnw spring-boot:run` sobe |
| 2 (1,5 h) | `MatchFileName` + testes; records Jackson; `MatchRepository` carrega tudo | Log: `336 partidas, 6618 rounds carregados` |
| 3 (1,5 h) | `MatchAssembler`: Red/Blue→nome, lados por round, kills limpos, `BuyType` | Teste: reconciliação kills = 100% |
| 4 (1,5 h) | 4 primeiras situações + `Wilson` + testes com números de ouro | Testes verdes com os valores da seção 10 |
- **Riscos:** caminho longo/acentos no Windows (derive torneio do prefixo); records + Jackson (use `@JsonIgnoreProperties(ignoreUnknown = true)`).
- **Concluído quando:** `GET /api/health/data` retorna os números da seção 5.

### Dia 3 — qua 21/10 — Restante das métricas, API e tela
| Bloco | Tarefa | Entregável |
|---|---|---|
| 1 (2 h) | Demais situações + `OpportunityService` + endpoints | `curl /api/teams/octacore_python/pauta` mostra "ataque" no topo |
| 2 (0,5 h) | `@RestControllerAdvice`, logs | 404/400 padronizados |
| 3 (2,5 h) | Vite + React + Tailwind: TeamPicker + PautaTable | Tela lista a pauta de qualquer time |
- **Risco:** front atrasar → plano B Thymeleaf. **Concluído quando:** fluxo time → pauta funciona no navegador.

### Dia 4 — qui 22/10 — Validação e diferencial
| Bloco | Tarefa | Entregável |
|---|---|---|
| 1 (1,5 h) | Detalhe por mapa/lado + fila de VOD | Clique em "ataque" → Ascent 13/45 + lista de rounds |
| 2 (1,5 h) | Validação cruzada Java × Python (seção 10) para 3 times; checagens de sanidade | Planilha/tabela de conferência sem divergência |
| 3 (1 h) | **Deploy** do jar | Link público abre |
| 4 (1 h) | Opcional: quartil superior *ou* painel de jogadores (escolha um) | — |
- **Concluído quando:** link público funcionando e números conferidos.

### Dia 5 — sex 23/10 — Estabilizar, gravar, entregar
| Horário | Tarefa |
|---|---|
| manhã | Congelar código (só correções). Polir textos da tela, selos e legendas de limitação |
| tarde | Slides (6–7 telas), roteiro, 2 ensaios cronometrados |
| até 19h | Gravar (OBS), editar o mínimo, subir no YouTube como **não listado** |
| até 21h | Preencher o Formulário Google: vídeo + apresentação + protótipo; **abrir os 3 links em aba anônima** |
| 21h–23h59 | **Folga para imprevistos** — não planeje nada aqui |

**Se for em equipe:** (1) ingestão + integridade, (2) métricas + testes, (3) insight/API, (4) front, (5) pitch, slides, vídeo e validação manual. Sozinha, o plano acima já está ajustado.

---

## 9. Guia técnico (incremental)

### Etapa 1 — Projeto
```bash
# No start.spring.io: Project=Maven, Language=Java, Java=21, Group=br.pauta, Artifact=pauta, Dependencies=Spring Web
# Descompacte em pauta/backend e rode:
./mvnw spring-boot:run
```
Se o Spring Boot oferecido for **4.x**, o Jackson é a versão 3: o pacote do `ObjectMapper` passa a ser `tools.jackson.databind`; as anotações (`@JsonIgnoreProperties`) continuam em `com.fasterxml.jackson.annotation`. Na 3.x, tudo é `com.fasterxml.jackson.*`. Use o que o IntelliJ sugerir no import.

`application.properties`:
```properties
pauta.data-dir=data
logging.level.br.pauta=INFO
```

### Etapa 2 — Nome do arquivo → partida (o primeiro teste)
Por quê: é a **única fonte do nome do time**. Se isso falhar, tudo falha — então é a primeira coisa a ser testada.
```java
package br.pauta.ingest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record MatchFileName(String tournament, String number, String date,
                            String teamA, int scoreA, int scoreB, String teamB, String map) {

    private static final Pattern P = Pattern.compile(
        "^(?<prefix>.+?)_(?<num>\\d+[a-z]?)_(?<date>\\d\\d-\\d\\d-\\d\\d)_(?<a>.+)_(?<sa>\\d+)_x_(?<sb>\\d+)_(?<b>.+)_(?<map>[a-z]+)$");

    public static MatchFileName parse(String fileName) {
        String base = fileName.replaceFirst("\\.json$", "");
        Matcher m = P.matcher(base);
        if (!m.matches()) {
            throw new InvalidMatchFileException("Nome de arquivo fora do padrão: " + fileName);
        }
        return new MatchFileName(m.group("prefix"), m.group("num"), m.group("date"),
            m.group("a"), Integer.parseInt(m.group("sa")), Integer.parseInt(m.group("sb")),
            m.group("b"), m.group("map"));
    }
}
```
(`InvalidMatchFileException` é uma `RuntimeException` simples.) O `.+?` no prefixo é *preguiçoso*: ele para no primeiro `_número_data`, por isso `RUSH_INCLUSIVO_003_...` funciona.

Teste:
```java
@Test
void parseNomeComSufixoDeLetra() {
    var f = MatchFileName.parse("RUSH_INCLUSIVO_014a_08-08-26_caap_auroras_4_x_13_macklogic_rainbow_split.json");
    assertEquals("RUSH_INCLUSIVO", f.tournament());
    assertEquals("caap_auroras", f.teamA());
    assertEquals(4, f.scoreA());
    assertEquals("macklogic_rainbow", f.teamB());
    assertEquals("split", f.map());
}
```

### Etapa 3 — Records do JSON bruto (só o que usamos)
Por quê: com `ignoreUnknown`, o Jackson descarta posições e campos inúteis → memória baixa.
```java
package br.pauta.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawMatch(MatchInfo matchInfo, List<RawPlayer> players, List<RawTeam> teams, List<RawRound> roundResults) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MatchInfo(String matchId, String mapId, long gameStartMillis, long gameLengthMillis) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawPlayer(String puuid, String gameName, String tagLine, String teamId,
                            String characterId, boolean isObserver, RawStats stats) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawStats(int kills, int deaths, int assists, int roundsPlayed, int score) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawTeam(String teamId, boolean won, int roundsWon) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawRound(int roundNum, String roundResult, String winningTeam, String winningTeamRole,
                           String plantSite, String bombPlanter, String bombDefuser, String roundCeremony,
                           List<RawPlayerRound> playerStats) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawPlayerRound(String puuid, List<RawKill> kills, List<RawDamage> damage, RawEconomy economy) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawKill(long timeSinceRoundStartMillis, String killer, String victim,
                          List<String> assistants, FinishingDamage finishingDamage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinishingDamage(String damageType, String damageItem) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawDamage(String receiver, int damage, int headshots, int bodyshots, int legshots) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RawEconomy(int loadoutValue, String weapon, String armor, int spent, int remaining) {}
}
```

### Etapa 4 — Domínio limpo + montagem
```java
package br.pauta.domain;

public enum Side { RED, BLUE;
    public Side other() { return this == RED ? BLUE : RED; }
    public static Side of(String teamId) { return Side.valueOf(teamId.toUpperCase()); }
}

public enum BuyType { ECO, SEMI, SEMI_FULL, FULL;
    /** Soma do loadoutValue dos 5 jogadores no início do round. Limiares validados pela matriz da liga. */
    public static BuyType of(int teamLoadout) {
        if (teamLoadout < 5_000) return ECO;
        if (teamLoadout < 15_000) return SEMI;
        if (teamLoadout < 20_000) return SEMI_FULL;
        return FULL;
    }
    public boolean isLow()  { return this == ECO || this == SEMI; }
    public boolean isHigh() { return this == SEMI_FULL || this == FULL; }
}

public record Kill(long timeMs, String killer, Side killerSide, String victim, Side victimSide) {}

public record Round(int number, Side winner, Side attacker, String plantSite,
                    java.util.Map<Side, BuyType> buy, java.util.List<Kill> kills) {
    public boolean isPistol() { return number == 0 || number == 12; }
    public Side defender() { return attacker.other(); }
    public boolean planted() { return plantSite != null && !plantSite.isEmpty(); }
}

public record Match(String fileId, String tournament, String date, String map,
                    java.util.Map<Side, String> teamName, java.util.List<Round> rounds) {
    public Side sideOf(String team) {
        return teamName.entrySet().stream().filter(e -> e.getValue().equals(team))
            .map(java.util.Map.Entry::getKey).findFirst().orElse(null);
    }
}
```
Trecho central do `MatchAssembler` (as três regras de limpeza que a análise revelou):
```java
// 1) nome do time pelo placar do arquivo
Map<Side, String> names = new EnumMap<>(Side.class);
for (RawTeam t : raw.teams()) {
    names.put(Side.of(t.teamId()), t.roundsWon() == file.scoreA() ? file.teamA() : file.teamB());
}
if (new HashSet<>(names.values()).size() != 2) {
    throw new InvalidMatchFileException("Não foi possível mapear Red/Blue em " + fileId);
}

// 2) sem observadores
Map<String, Side> sideOf = raw.players().stream()
    .filter(p -> !p.isObserver() && p.stats() != null)
    .collect(Collectors.toMap(RawPlayer::puuid, p -> Side.of(p.teamId())));

// 3) por round: lado atacante, compra e kills limpos (sem autoabate nem fogo amigo), ordenados no tempo
Side winner = Side.of(r.winningTeam());
Side attacker = "Attacker".equals(r.winningTeamRole()) ? winner : winner.other();
List<Kill> kills = r.playerStats().stream()
    .flatMap(ps -> ps.kills().stream())
    .filter(k -> !k.killer().equals(k.victim()))
    .filter(k -> sideOf.containsKey(k.killer()) && sideOf.containsKey(k.victim()))
    .filter(k -> sideOf.get(k.killer()) != sideOf.get(k.victim()))
    .map(k -> new Kill(k.timeSinceRoundStartMillis(), k.killer(), sideOf.get(k.killer()), k.victim(), sideOf.get(k.victim())))
    .sorted(Comparator.comparingLong(Kill::timeMs))
    .toList();
```
O `MatchRepository` (um `@Component`) percorre `pauta.data-dir` com `Files.walk`, faz `objectMapper.readValue(path.toFile(), RawMatch.class)` num `try/catch` por arquivo (um arquivo ruim gera `log.warn` e entra no relatório, mas não derruba a aplicação) e guarda a `List<Match>` imutável.

**Como testar:** log de inicialização `Carregadas 336 partidas (6618 rounds, 0 rejeitadas, 22 observadores filtrados)`. **Erros prováveis:** `UnrecognizedPropertyException` (faltou `ignoreUnknown`), `NullPointerException` em `stats` (observador), `IllegalArgumentException` em `Side.of("")`.

### Etapa 5 — Uma situação = uma regra
```java
package br.pauta.metrics;

public interface Situation {
    String id();           // "attack-without-plant"
    String label();        // "Ataque sem conseguir plantar"
    String definition();   // texto exibido na tela — transparência
    /** Registra, para o time no round, se a situação ocorreu e se o round foi vencido. */
    void accept(Match m, Round r, Side side, SituationAccumulator acc);
}

public final class FirstBloodConversion implements Situation {
    public String id() { return "first-blood-conversion"; }
    public String label() { return "Converter o first blood"; }
    public String definition() { return "Rounds em que o time fez o primeiro abate (excluindo autoabates). Sucesso = venceu o round."; }
    public void accept(Match m, Round r, Side side, SituationAccumulator acc) {
        if (r.kills().isEmpty()) return;
        if (r.kills().get(0).killerSide() == side) acc.add(m, r, r.winner() == side);
    }
}

public final class AttackWithoutPlant implements Situation {
    public String id() { return "attack-without-plant"; }
    public String label() { return "Ataque que não chega ao plant"; }
    public String definition() { return "Rounds de ataque. Sucesso = plantou a spike."; }
    public void accept(Match m, Round r, Side side, SituationAccumulator acc) {
        if (r.attacker() == side) acc.add(m, r, r.planted());
    }
}
```
Atenção: em `AttackWithoutPlant` o "sucesso" é plantar, não vencer — então a unidade do ranking é **plants em jogo**, não rounds. Mostre isso na tela (ou converta multiplicando pela diferença de vitória com/sem plant). Para o MVP, basta rotular corretamente.

`SituationAccumulator` guarda `successes`, `total` e a lista de `RoundRef(fileId, map, roundNumber)` dos fracassos — a fila de VOD sai de graça.

### Etapa 6 — Intervalo de confiança (explicação)
Uma taxa de 41% em 99 rounds não é "exatamente 41%": com outra amostra poderia ser 35% ou 48%. O **intervalo de Wilson** dá a faixa plausível (95%) da taxa real. Regra do produto: se a média da liga **está fora** da faixa do time → "diferença comprovada"; se está dentro → "não comprovado"; se n < 20 → "amostra pequena".
```java
public record Interval(double low, double high) {
    public static Interval wilson(int successes, int n) {
        if (n == 0) return new Interval(0, 0);
        double z = 1.96, p = (double) successes / n, z2 = z * z;
        double den = 1 + z2 / n;
        double center = (p + z2 / (2 * n)) / den;
        double half = z * Math.sqrt(p * (1 - p) / n + z2 / (4.0 * n * n)) / den;
        return new Interval(center - half, center + half);
    }
}
```
Teste: `wilson(41, 99)` ≈ `[0,32 ; 0,51]` (bate com o script Python).

### Etapa 7 — Pauta
```java
double teamRate = (double) team.successes() / team.total();
double refRate  = (double) league.successes() / league.total();
double roundsAtStake = (refRate - teamRate) * team.total();   // > 0 = oportunidade
Interval ci = Interval.wilson(team.successes(), team.total());
Verdict v = team.total() < 20 ? Verdict.SMALL_SAMPLE
          : (ci.high() < refRate || ci.low() > refRate) ? Verdict.PROVEN
          : Verdict.NOT_PROVEN;
```
Ordene por `roundsAtStake` decrescente, mostrando primeiro os `PROVEN`. Atenção: em situações onde o "sucesso" é ruim (nenhuma das propostas, mas se criar), inverta o sinal.

### Etapa 8 — API e erros
```java
@RestController
@RequestMapping("/api/teams")
class TeamController {
    private final OpportunityService service;
    TeamController(OpportunityService service) { this.service = service; }

    @GetMapping("/{team}/pauta")
    List<PautaItem> pauta(@PathVariable String team, @RequestParam(defaultValue = "league") String benchmark) {
        return service.pauta(team, Benchmark.parse(benchmark));   // lança TeamNotFoundException / IllegalArgumentException
    }
}

@RestControllerAdvice
class ApiErrorHandler {
    record ApiError(String code, String message, Instant timestamp) {}
    @ExceptionHandler(TeamNotFoundException.class)
    ResponseEntity<ApiError> notFound(TeamNotFoundException e) {
        return ResponseEntity.status(404).body(new ApiError("TEAM_NOT_FOUND", e.getMessage(), Instant.now()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID_PARAMETER", e.getMessage(), Instant.now()));
    }
}
```
```bash
curl http://localhost:8080/api/teams/octacore_python/pauta
```

### Etapa 9 — Front mínimo
```bash
npm create vite@latest frontend -- --template react
```
Em `vite.config.js`, `server: { proxy: { '/api': 'http://localhost:8080' } }`. Instale o Tailwind seguindo o guia oficial para Vite (o comando muda entre versões — siga a página do Tailwind).
Componente central (tabela é melhor que gráfico aqui: o treinador lê de cima para baixo):
```jsx
export function PautaTable({ items, onSelect }) {
  return (
    <table className="w-full text-sm">
      <thead><tr className="text-left text-gray-500">
        <th>Situação</th><th>Time</th><th>Referência</th><th>Amostra</th><th>Rounds em jogo</th><th>Evidência</th>
      </tr></thead>
      <tbody>
        {items.map(i => (
          <tr key={i.id} onClick={() => onSelect(i.id)} className="cursor-pointer hover:bg-gray-50 border-t">
            <td className="py-2 font-medium">{i.label}</td>
            <td>{(i.teamRate * 100).toFixed(0)}%</td>
            <td>{(i.benchmarkRate * 100).toFixed(0)}%</td>
            <td>{i.n}</td>
            <td className={i.roundsAtStake > 0 ? 'text-red-600 font-semibold' : 'text-green-700'}>
              {i.roundsAtStake > 0 ? '+' : ''}{i.roundsAtStake.toFixed(1)}
            </td>
            <td><VerdictBadge verdict={i.verdict} /></td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

### Práticas do Bradesco aplicadas (sem burocracia)
| Prática | Aplicação enxuta |
|---|---|
| Separação de responsabilidades | 5 pacotes; nenhum controller calcula nada |
| Regras de negócio isoladas | Uma classe por situação, com `definition()` exibida ao usuário |
| Validação de entrada | Nome de arquivo, mapeamento Red/Blue, observadores, autoabates — tudo na ingestão |
| Testes | Unitários por regra + teste de integração com a base inteira e números de ouro |
| Exceções | Exceções de domínio + `@RestControllerAdvice` com contrato único de erro |
| Logs | Um resumo de carga na subida + `warn` por arquivo rejeitado; nada de log por round |
| Integridade/observabilidade | `/api/health/data` — vira slide de confiabilidade |
| Rastreabilidade | Cada número da pauta leva aos rounds de origem (fila de VOD) |

---

## 10. Estratégia de validação

1. **Reconciliação com o placar oficial** (teste de integração): para cada jogador-partida, kills limpos = `stats.kills` (esperado: 3.360/3.360). Para cada partida, rounds vencidos por lado = `teams[].roundsWon`.
2. **Identidades de soma:** converter FB + reagir a FB sofrido = 100% na liga (71,3 + 28,7); pós-plant + retake = 100% (70,8 + 29,2); ataque + defesa da liga somam 100% (51,8 + 48,2). Se não somar, há bug no lado do round.
3. **Números de ouro** (calculados pelos scripts Python, independentes do Java):

| Teste | Valor esperado |
|---|---|
| Partidas / rounds / kills brutos | 336 / 6.618 / 46.753 |
| Observadores filtrados | 22 |
| Liga: FB → vitória | 71,3% (n = 6.584) |
| Liga: trade ≤ 5 s | 20,2% |
| Liga: pós-plant / retake | 70,8% / 29,2% |
| octacore_python: ataque | 41/99 (41,4%) |
| octacore_python: ataque em Ascent | 13/45 |
| octacore_python: ataques sem plant | 40 (venceu 3) |
| ufu_saints: rounds | 362 jogados, 59,1% vencidos |
| ufu_saints: pós-plant | 124/148 (83,8%) |

   Pequenas diferenças (±1 round) podem vir de bordas (rounds sem kill, empate de tempo); se aparecerem, investigue o round específico antes de aceitar.
4. **Sanidade de domínio:** matriz de compra monotônica (eco < semi < semi-full < full); nenhum round com 0 ou 2 vencedores; pistols só nos rounds 1 e 13.
5. **Validação humana:** pegue 2 rounds da fila de VOD e confira manualmente no JSON (quem fez o first blood, se plantou). Mostre isso no pitch em 10 segundos.

Os scripts: [metrics.py](../tools/analise/metrics.py) (ranking), [drill.py](../tools/analise/drill.py) (mapa/lado), [killcheck.py](../tools/analise/killcheck.py) (reconciliação), [profile.py](../tools/analise/profile.py) (perfil da base), saída em [perfil-base.txt](../tools/analise/perfil-base.txt).

---

## 11. Demonstração do produto

**Fluxo (60–90 s no vídeo):**
1. Tela inicial: "336 partidas · 6.618 rounds · 46.753 abates · 100% reconciliados com o placar".
2. Escolhe **octacore_python** → a Pauta abre: no topo, "Rounds de ataque — +10 rounds em jogo — **comprovado**"; abaixo, itens "não comprovados" em cinza (o produto mostra que *não* conclui o que não pode).
3. Clica em "ataque" → quebra por mapa: **Ascent 29%**; "40% dos ataques sem plant, vitória em 7,5% deles".
4. **Fila de VOD:** a lista real de partidas e números de round perdidos naquela situação (gerada pelo sistema, não inventada para o vídeo) — o treinador sabe exatamente o que assistir.
5. (Opcional) Troca para **ufu_saints** → "acima da média em tudo; contra o quartil superior, o ponto de atenção é X" — mostra que a ferramenta funciona para times fortes também.

**Telas:** (1) seleção de time com resumo; (2) Pauta; (3) detalhe da situação + fila de VOD; (4) "Como calculamos" (definições + limitações).

---

## 12. Pitch (≤ 5 min)

**Slides (6):** Problema · Ideia · Como lemos os dados · Demo · Confiabilidade · Impacto e próximos passos.

| Tempo | Conteúdo | Tela |
|---|---|---|
| 0:00–0:35 | "Depois de uma série, um treinador universitário tem um placar e uma pilha de números: K/D, ADR, win rate. Nenhum deles diz **o que treinar na segunda-feira**, nem **quais rounds rever**." | Slide: placar + tabela de números soltos |
| 0:35–1:05 | "A Pauta de Treino transforma as partidas em uma lista priorizada de situações de jogo, medida em **rounds em jogo**, e entrega a lista exata de rounds para revisar." | Slide da ideia |
| 1:05–1:50 | Como usamos os dados: 336 partidas oficiais do UNIVAVÁ e Rush Series; cada round reconstruído a partir dos eventos de abate, economia e plant; 10 situações com definição explícita; comparação com a liga e intervalo de confiança | Slide com o fluxo JSON → round → situação → pauta |
| 1:50–3:30 | **Demo ao vivo** (fluxo da seção 11) | Produto |
| 3:30–4:10 | Confiabilidade: 46.753 abates reconciliados 100% com o placar oficial (após remover autoabates por bomba/queda e fogo amigo — achado da nossa auditoria da base); identidades de soma; testes automatizados; o produto marca "não comprovado" e "amostra pequena" em vez de inventar conclusões | Slide + tela `/health/data` |
| 4:10–4:50 | Impacto: do número à decisão (o que treinar) e à ação (quais rounds rever). Viabilidade: um único serviço Java, roda com qualquer nova leva de JSON no mesmo formato. Próximos passos: comparar antes/depois de um bloco de treinos, referenciais por divisão | Slide final |
| 4:50–5:00 | Frase de fechamento: "Não dizemos ao time que ele é ruim. Mostramos onde estão os rounds — e quais assistir." | Logo + links |

Cuidados: **não diga que o produto melhora o desempenho** — diga que ele **identifica onde os rounds se perdem e organiza a revisão**. Fale de times, não exponha jogadores individualmente de forma negativa (são estudantes reais e a banca é da organização do campeonato).

---

## 13. Próxima ação imediata

1. **Agora (10 min):** abra um ticket na monitoria do Discord perguntando: (a) "posso embutir uma tabela estática UUID → nome de agente/arma?" e (b) "posso publicar um protótipo que contém a base?".
2. **Amanhã, primeiro bloco:** gere o projeto no start.spring.io (Maven, Java 21, Spring Web), copie as 5 pastas de dados para `backend/data/` e implemente o `MatchFileName` com o teste da etapa 2. Quando o teste passar, siga para os records da etapa 3.
