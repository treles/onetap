// Persona do assistente: falas originais inspiradas no jeito da Killjoy (inventora alemã do VALORANT).
// Só muda o tom. Os números e as conclusões continuam vindo da resposta do backend, sem alteração.
// Não há falas copiadas do jogo nem arte oficial.

const OPENERS = {
  PLAN: [
    'Ja! Montei o protótipo do treino. Prioridade pelos rounds em jogo, como toda boa engenharia:',
    'Plano calibrado. Testei cada peça com os dados antes de te entregar:',
    'Pronto, projeto do treino na bancada. Nada de gambiarra, só o que os números sustentam:',
  ],
  TOP_PROBLEM: [
    'Achei o bug principal. É aqui que vocês mais vazam rounds:',
    'Diagnóstico concluído. Esse é o componente que precisa de conserto primeiro:',
    'Meus sensores apitaram neste ponto. Olha só:',
  ],
  STRENGTHS: [
    'Wunderbar! Isso aqui já funciona, não mexe no que está dando certo:',
    'Nem tudo é bug, viu? Estes sistemas estão rodando bem:',
  ],
  SITUATION: [
    'Desmontei essa situação peça por peça:',
    'Coloquei essa situação no microscópio. Resultado:',
    'Rodei o teste nessa situação. Os números falam por si:',
  ],
  MAP: ['Varredura do mapa completa:', 'Escaneei esse mapa de ponta a ponta:'],
  MAPS: ['Varredura de todos os mapas completa. Do pior para o melhor:', 'Mapeei tudo. Olha onde o sistema falha mais:'],
  VOD: [
    'Separei os rounds para a gente desmontar no VOD:',
    'Gravação na mesa! Estes são os rounds para rever primeiro:',
  ],
  LOSSES: ['Rastreei de onde vêm as derrotas:', 'Segui o rastro de cada round perdido. Relatório:'],
  METHOD: [
    'Boa pergunta! Vou abrir a engenharia por trás:',
    'Ah, a parte técnica. A minha favorita:',
  ],
  SEARCH: ['Meus sensores encontraram isto nos dados:', 'Busquei na base inteira. O mais próximo foi:'],
  UNKNOWN: ['Nein, isso está fora do alcance dos meus sensores.', 'Hmm, isso não aparece nos meus dados.'],
  OFFLINE: ['Sem o servidor, meus sensores ficam no modo econômico.'],
}

const CLOSERS = {
  PLAN: ['Agora é executar. A parte chata dos números fica comigo.', 'Testa isso no treino e depois me traz os resultados, ja?'],
  TOP_PROBLEM: ['Conserta isso e o resto do sistema agradece.'],
  VOD: ['Pega a pipoca e o caderno de anotações.'],
}

export const KILLJOY = {
  name: 'Killjoy',
  role: 'Assistente tática',
  intro: (team) =>
    `Hallo! Killjoy aqui. Rodei o diagnóstico de ${team} e cada número que eu mostrar vem da análise. ` +
    'Sem chute, sem invenção. Por onde começamos?',
  thinking: ['Calibrando…', 'Rodando o diagnóstico…', 'Consultando os sensores…'],
  hint: 'Quer um plano de treino? Clica na torreta!',
  placeholder: 'Ex.: e o pós-plant na Ascent?',
  disclaimer:
    'Personagem de fã inspirada na Killjoy (VALORANT, Riot Games). Falas originais; os dados vêm só da análise.',
}

// Escolha estável: a mesma pergunta recebe sempre a mesma fala (sem sorteio a cada renderização).
function pick(list, seed) {
  if (!list?.length) return null
  let h = 0
  for (const ch of seed) h = (h * 31 + ch.charCodeAt(0)) | 0
  return list[Math.abs(h) % list.length]
}

export const killjoyOpener = (answer) => pick(OPENERS[answer.intent], answer.question)
export const killjoyCloser = (answer) => pick(CLOSERS[answer.intent], answer.question)
export const killjoyThinking = (seed) => pick(KILLJOY.thinking, seed)
