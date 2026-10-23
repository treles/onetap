# Tarefas: o que falta até a entrega

Prazo final: **sexta 23/10, 23h59**. Meta interna: **enviar até 21h**.

## Pronto (branch `feat/mvp-completo`)

- [x] Ingestão completa com auditoria (3.360/3.360 abates conferidos com o placar)
- [x] 12 situações, por time e por mapa, todas conferidas com a análise independente em Python
- [x] Referência ajustada aos mapas jogados, sem o próprio time; liga ou quartil superior
- [x] Intervalo de Wilson, indício, evidência forte (Benjamini-Hochberg), amostra pequena
- [x] Recomendação em frase, pontos de atenção, pontos fortes, detalhe por mapa, fila de VOD
- [x] "Onde os rounds foram perdidos" (categorias excludentes vs liga)
- [x] Página "Como calculamos"
- [x] 47 testes, CI, snapshot estático, Dockerfile
- [x] Roteiro do pitch e da demo com números reais: [PITCH.md](PITCH.md)

## Falta (depende de contas, decisões ou de você)

| Quando | Tarefa | Quem | Observação |
|---|---|---|---|
| Hoje | Revisar e fazer merge do PR `feat/mvp-completo` na `main` | Thales | CI precisa estar verde |
| Hoje | Ticket na monitoria (Discord): pode publicar um site com dados agregados da base? Pode usar nomes de agentes/armas por tabela estática? | Thales | Sem resposta até quinta à tarde: publique mesmo assim; o site só tem agregados |
| Qui 22 | Publicar o protótipo (README, "Publicar o protótipo", opção A) e testar o link numa aba anônima | Thales + Claude | Precisa de login numa hospedagem (Netlify, Vercel ou outra) |
| Qui 22 | Slides (6, conteúdo pronto em PITCH.md) | Thales + Claude | Claude pode montar o deck |
| Qui 22 | Conferir 1 round da fila de VOD no JSON (prova de confiabilidade do pitch) | Thales | Ver PITCH.md |
| Qui 22 à noite | Ensaio cronometrado (≤ 5 min) | Thales | |
| Sex 23 até 18h | Gravar a tela + narração, editar, subir no YouTube como **não listado** | Thales | OBS Studio grava tela e voz |
| Sex 23 até 21h | Formulário Google: nomes do time, vídeo, slides, link do protótipo. Abrir os 3 links numa aba anônima | Thales | |

## Opcional (só se sobrar tempo)

- Nomes de agentes na tela (depende da monitoria)
- Painel por jogador (KAST, ADR, HS%) para conectar individual e coletivo
- Comparar duas fases do mesmo time (ex.: Classificatória 1 contra Fase de Grupos) para mostrar evolução
