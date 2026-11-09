/**
 * Front-end da Arena de Chefões.
 * O JavaScript só ENVIA a ação e DESENHA o JSON devolvido pelo Java.
 * Nenhuma regra de combate (dano, cura, turno, vitória) é calculada aqui.
 */
const botoes = document.querySelectorAll('[data-acao]');
const botaoReiniciar = document.querySelector('#reiniciar');
const elHeroi = document.querySelector('.heroi');
const elChefe = document.querySelector('.chefe');
const telaDificuldade = document.querySelector('#tela-dificuldade');
const telaBatalha = document.querySelector('#tela-batalha');
const botoesNivel = document.querySelectorAll('[data-dificuldade]');
const botaoTrocar = document.querySelector('#trocar-dificuldade');

let encerrado = false;   // espelha dados.encerrado
let ocupado = false;     // evita clique duplo enquanto o fetch não volta
let anterior = null;     // último estado desenhado (usado só para animações)

function mostrarTela(batalha) {
  telaDificuldade.hidden = batalha;
  telaBatalha.hidden = !batalha;
}

// Ao abrir a página: se já existe uma batalha em andamento no Java, continua nela;
// senão, mostra a escolha de dificuldade. Quem decide é o estado vindo do servidor.
async function carregarEstado() {
  const resposta = await fetch('/api/estado');
  const dados = await resposta.json();
  const emAndamento = dados.turno > 1 || dados.encerrado;
  if (emAndamento) renderizar(dados);
  mostrarTela(emAndamento);
}

// Escolha de dificuldade: o Java guarda o nível e reinicia a batalha com ele.
async function iniciarBatalha(dificuldade) {
  botoesNivel.forEach(b => b.disabled = true);
  try {
    const resposta = await fetch('/api/reiniciar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ dificuldade })
    });
    const dados = await resposta.json();
    if (!resposta.ok || dados.erro) {
      alert(dados.erro || 'Erro ao falar com o servidor.');
      return;
    }
    anterior = null;                            // sem animação ao iniciar
    renderizar(dados);
    mostrarTela(true);
  } catch (erro) {
    alert('Falha ao conectar ao Java: ' + erro.message);
  } finally {
    botoesNivel.forEach(b => b.disabled = false);
  }
}

// TODO 4: POST /api/jogar com a ação no corpo
async function enviarAcao(acao) {
  if (ocupado || encerrado) return;
  ocupado = true;
  botoes.forEach(b => b.disabled = true);
  try {
    const resposta = await fetch('/api/jogar', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ acao })
    });
    const dados = await resposta.json();
    if (!resposta.ok || dados.erro) {          // o servidor devolve 400 com {erro}
      document.querySelector('#mensagem').textContent = dados.erro || 'Erro ao falar com o servidor.';
      return;
    }
    renderizar(dados, acao);
  } catch (erro) {
    document.querySelector('#mensagem').textContent = 'Falha ao conectar ao Java: ' + erro.message;
  } finally {
    ocupado = false;
    botoes.forEach(b => b.disabled = encerrado);
  }
}

function renderizar(dados, acao) {
  document.querySelector('#vida-jogador-texto').textContent = `${dados.jogadorVida} / ${dados.jogadorMax} PV`;
  document.querySelector('#vida-chefe-texto').textContent = `${dados.chefeVida} / ${dados.chefeMax} PV`;
  document.querySelector('#pocoes').textContent = dados.pocoes;
  document.querySelector('#turno').textContent = `Turno ${dados.turno}`;
  document.querySelector('#mensagem').textContent = dados.mensagem;

  // TODO 5: barras de vida
  document.querySelector('#barra-jogador').style.width = (dados.jogadorVida / dados.jogadorMax * 100) + '%';
  document.querySelector('#barra-chefe').style.width = (dados.chefeVida / dados.chefeMax * 100) + '%';

  // TODO 6: bloquear os botões quando a batalha acabou
  encerrado = dados.encerrado;
  botoes.forEach(b => b.disabled = dados.encerrado);

  // TODO 7: vitória ou derrota
  const resultado = document.querySelector('#resultado');
  if (dados.resultado === 'vitoria') resultado.textContent = '🏆 VITÓRIA! O Bug Supremo foi derrotado.';
  else if (dados.resultado === 'derrota') resultado.textContent = '💀 DERROTA! Reinicie e tente de novo.';
  else resultado.textContent = '';

  // TODO 8: aviso do próximo golpe, vindo pronto do Java
  const aviso = document.querySelector('#aviso');
  aviso.textContent = dados.aviso;
  aviso.classList.toggle('especial', !!dados.proximoEspecial);

  // Extra: dificuldade (nome e código vêm do Java)
  const nivel = document.querySelector('#nivel-nome');
  nivel.textContent = dados.dificuldadeNome;
  nivel.dataset.nivel = dados.dificuldade;

  // Extras: modo fúria, histórico de combate e animações
  elChefe.classList.toggle('furia', !!dados.furia);
  document.querySelector('#selo-furia').hidden = !dados.furia;
  desenharHistorico(dados.historico);
  animar(dados, acao);
  anterior = dados;
}

function desenharHistorico(lista) {
  const ul = document.querySelector('#historico');
  ul.replaceChildren(...(lista || []).map(texto => {
    const li = document.createElement('li');
    li.textContent = texto;
    return li;
  }));
}

// Só compara valores que vieram do Java para escolher o efeito visual.
function animar(dados, acao) {
  if (!anterior) return;                        // primeira carga ou reinício
  if (dados.chefeVida < anterior.chefeVida) efeito(elChefe, 'dano');
  if (dados.jogadorVida < anterior.jogadorVida) {
    efeito(elHeroi, 'dano');
    if (anterior.proximoEspecial) efeito(document.body, 'tremor');  // golpe especial avisado
  }
  if (acao === 'curar') efeito(elHeroi, 'cura');
  if (acao === 'defender') efeito(elHeroi, 'defesa');
}

function efeito(elemento, classe) {
  elemento.classList.remove(classe);
  void elemento.offsetWidth;                    // reinicia a animação CSS
  elemento.classList.add(classe);
}

botoes.forEach(botao => {
  botao.addEventListener('click', () => enviarAcao(botao.dataset.acao));
});

botoesNivel.forEach(botao => {
  botao.addEventListener('click', () => iniciarBatalha(botao.dataset.dificuldade));
});

botaoTrocar.addEventListener('click', () => mostrarTela(false));

botaoReiniciar.addEventListener('click', async () => {
  try {
    const resposta = await fetch('/api/reiniciar', { method: 'POST' });
    const dados = await resposta.json();
    anterior = null;                            // sem animação ao reiniciar
    renderizar(dados);
  } catch (erro) {
    document.querySelector('#mensagem').textContent = 'Falha ao conectar ao Java: ' + erro.message;
  }
});

carregarEstado().catch(erro => {
  document.querySelector('#mensagem').textContent = 'Falha ao conectar ao Java: ' + erro.message;
});
