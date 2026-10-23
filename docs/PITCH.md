# Pitch: Pauta de Treino (vídeo de até 5 minutos)

Todos os números abaixo saem do produto (base oficial do hackathon). Confira na tela antes de gravar.

## Mensagem central

> Não dizemos ao time que ele é ruim. Mostramos **onde** os rounds se perdem, **com quanta certeza**, e **quais rounds assistir**.

## Roteiro

| Tempo | Fala (resumo) | Na tela |
|---|---|---|
| 0:00–0:30 | **Problema.** Depois de uma série, o treinador universitário tem placar, K/D e ADR. Nenhum desses números diz o que treinar na segunda-feira, nem quais dos mais de 100 rounds perdidos vale a pena rever. | Slide 1: uma tabela de números soltos |
| 0:30–0:55 | **Solução.** A Pauta de Treino lê as partidas oficiais do Univavá e da Rush Series, mede 12 situações de jogo em cada mapa, compara com a liga e entrega uma pauta: o ponto principal, com que evidência, e a lista exata de rounds para rever. | Slide 2: frase + print da tela |
| 0:55–1:40 | **Como lemos os dados.** 336 mapas, 6.618 rounds, 46.753 abates. Reconstruímos cada round pelos abates com tempo, economia de cada jogador e plant. Auditoria da base: removendo 373 mortes por bomba ou queda e 9 de fogo amigo, os abates batem com o placar oficial em 3.360 de 3.360 linhas. | Slide 3: fluxo JSON → round → situação → pauta, e o número 3.360/3.360 |
| 1:40–3:25 | **Demo ao vivo** (roteiro abaixo) | Produto |
| 3:25–4:10 | **Por que confiar.** Três escolhas de método: (1) a referência considera os mapas que o time jogou; em Ascent o ataque da liga vence só 47%, então um time que joga muito Ascent não é injustiçado. (2) Cada número mostra amostra e intervalo de confiança. (3) Como comparamos dezenas de situações, aplicamos correção para múltiplas comparações; só aí chamamos de "evidência forte". Resultado: dos 34 times com 8 ou mais mapas, 25 recebem ponto de atenção com evidência, 9 com evidência forte, e 9 nenhum. Não inventamos problema. | Slide 4 + página "Como calculamos" |
| 4:10–4:45 | **Impacto e viabilidade.** Do número à decisão (o que treinar) e à ação (quais rounds rever). Um serviço Java, 47 testes automatizados que conferem cada indicador com uma análise independente em Python, e o mesmo código roda com qualquer nova leva de partidas no mesmo formato. | Slide 5 |
| 4:45–5:00 | Fechamento com a frase central. | Slide 6: nome + link |

## Demo (1min45)

1. **Abrir com a Octacore Python** (link direto: `#time=octacore_python`).
   - Ler a frase do topo: depois de sofrer o primeiro abate em **Ascent**, o time venceu **5% dos rounds (2 de 38)** contra **29%** da referência. **Evidência forte.**
   - Mostrar os pontos de atenção logo abaixo, todos em Ascent: rounds de ataque (29% contra 48%) e chegar ao plant (49% contra 63%). "O problema não é o time inteiro; é Ascent."
2. **Clicar em "Ver os 36 rounds para rever"**: aparecem o mapa a mapa (Lotus e Split sem evidência, Ascent destacado) e a lista de rounds com adversário, torneio, data e número do round.
   - Fala: "Esta é a pauta da sessão de VOD: 36 rounds, não 113."
3. **Mostrar a tabela "Todas as situações"**: no geral, o ataque da Octacore parecia pior que a liga (41% contra 52%), mas, com a referência ajustada aos mapas, **não há evidência**. "Uma média simples teria apontado o problema errado."
4. **Trocar para a UFU Saints e depois para "Quartil superior"**: contra a liga, nenhum ponto de atenção (time forte). Contra os 8 melhores times, surge um: chegar ao plant em Haven (71% contra 83%). Pontos fortes: pós-plant de 84% contra 75%.
5. (Se sobrar tempo) **Azure Bears Black**: converte mal o first blood em Haven (57% contra 71%, evidência forte), mas é muito forte no ataque em Split.

## Prova de confiabilidade (10 s no vídeo)

Abrir um round da fila de VOD da Octacore e mostrar no JSON correspondente (o nome do arquivo aparece ao passar o mouse na linha) que o primeiro abate foi do adversário e que o round foi perdido. Use `tools/analise/deep.py` ou um visualizador de JSON.

## Slides (6)

1. **O problema:** "Muitos números, nenhuma pauta."
2. **A solução:** frase central + print da tela inicial.
3. **Os dados:** 336 mapas, 6.618 rounds, 46.753 abates; 3.360/3.360 conferidos.
4. **O método:** referência por mapa, intervalo de confiança, correção para múltiplas comparações; 25 de 34 times com pauta, 9 com evidência forte.
5. **Engenharia:** Spring Boot + React, 47 testes, números conferidos contra análise independente.
6. **Fechamento:** frase + link do protótipo.

## Cuidados

- Não dizer que o produto "melhora o desempenho". Ele **aponta onde os rounds se perdem e organiza a revisão**.
- Falar de times, não de jogadores individualmente. São estudantes reais e a banca organiza o campeonato.
- As sugestões de foco de treino são orientações gerais por situação, não conclusões dos dados. Se perguntarem, diga isso.
