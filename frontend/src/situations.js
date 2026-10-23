// Textos de interface derivados das definições do backend (metrics/situations).
// O "motivo de revisão" é exatamente a falha da situação, ou seja, o oposto do que ela conta como sucesso.

export const REVIEW_REASON = {
  attack: 'Perdeu o round no ataque',
  defense: 'Perdeu o round na defesa',
  'first-blood-conversion': 'Fez o primeiro abate e perdeu o round',
  'first-blood-reaction': 'Sofreu o primeiro abate e perdeu o round',
  'first-death-traded': 'Primeiro abatido do time ficou sem trade em 5 s',
  pistol: 'Perdeu o round de pistol',
  'post-plant': 'Plantou a spike e perdeu o round',
  retake: 'Perdeu o retake com a spike plantada',
  'anti-eco': 'Com compra alta, perdeu para um time em eco',
  'low-buy-vs-full': 'Em compra baixa, perdeu para compra alta',
  'attack-plant-rate': 'Atacou e não conseguiu plantar',
  clutch: 'Ficou em 1 contra 2 ou mais e perdeu',
}

/** As três métricas de destaque da tela principal, na ordem de exibição. */
export const KEY_METRICS = [
  { id: 'first-blood-conversion', title: 'First Blood', subtitle: 'converter a vantagem do primeiro abate' },
  { id: 'post-plant', title: 'Pós-plant', subtitle: 'segurar o round depois de plantar' },
  { id: 'retake', title: 'Retake', subtitle: 'recuperar o site com a spike plantada' },
]
