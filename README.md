# Desafio prático — Arena de Chefões ⚔️

**Disciplina:** Desenvolvimento de Sistemas  
**Equipe:** 2 ou 3 estudantes  
**Tecnologias:** Java (back-end), HTML/CSS/JavaScript (front-end)

## Missão

Transforme o projeto inicial em um **jogo de batalha por turnos** que funcione no navegador. Os botões da página devem enviar ações ao servidor Java. **O Java decide o que acontece na batalha**; o JavaScript apenas envia as ações e atualiza a interface com os dados recebidos.

O desafio não é simplesmente atacar até a vida do chefão acabar: ele realiza **golpes especiais previsíveis**, e o jogador precisa combinar ataque, defesa e cura para sobreviver.

## Regras obrigatórias

1. **Condições iniciais:** jogador com **100 PV**, chefão com **150 PV** e apenas **2 poções**. A batalha começa no turno 1.
2. **Atacar:** o jogador causa um dano aleatório de **18 a 25 PV** no chefão.
3. **Defender:** o jogador não causa dano nesse turno, mas reduz o dano recebido para **um quarto do valor original**, arredondado para baixo (mínimo de 1 PV).
4. **Curar:** recupera **26 a 34 PV**, sem ultrapassar 100. Consome uma poção; não pode ser usada se não restarem poções ou se a vida estiver cheia. Uma tentativa inválida não gasta turno.
5. **Contra-ataque comum:** depois de cada ação válida, **se o chefão estiver vivo**, ele causa **11 a 17 PV** de dano.
6. **Golpe especial:** em todo **3º turno válido** (turnos 3, 6, 9, 12...), o chefão usa o golpe especial **no lugar do ataque comum**, causando **32 a 42 PV** antes da redução por defesa.
7. **Aviso antecipado:** antes da ação do jogador, a interface deve indicar se o **próximo contra-ataque** será comum ou especial. Essa informação vem do servidor Java no campo `aviso` do JSON. O jogador não pode ser surpreendido por um golpe especial sem aviso.
8. **Fim de jogo:** vida do chefão em 0 significa vitória; vida do jogador em 0 significa derrota. Quando um deles chega a 0, a batalha termina. Se o chefão morrer no ataque do jogador, **não contra-ataca**.
9. **Integração:** enviar as ações usando `fetch()` com `POST /api/jogar` e `acao=atacar`, `acao=defender` ou `acao=curar`.
10. **Interface:** atualizar barras e números de vida (PV), poções, turno, aviso do próximo golpe, mensagem e resultado com o JSON devolvido pelo Java. **Não exibir a quantidade de dano causado ou recebido, nem as faixas de dano nos avisos ou mensagens da tela**; o efeito aparece na mudança das barras de vida. Bloquear os botões após o fim da partida.
11. **Identidade própria:** a equipe deve alterar **pelo menos dois elementos visuais** do HTML/CSS (personagens, nomes, cenário, tema, layout, efeitos etc.).

**Regras de consistência:** a vida nunca fica negativa; não se ultrapassa 100 PV ao curar; defender não machuca o chefão; uma tentativa inválida de cura não consome poção nem turno; depois do fim, ações não modificam o estado; reiniciar restaura todos os valores.

### Exemplo da estratégia de combate

- **Turno 1:** o servidor anuncia ataque comum; o jogador pode atacar, defender ou curar.
- **Turno 2:** o servidor anuncia ataque comum novamente.
- **Turno 3:** o servidor **avisa antes** que o chefão preparou um golpe especial. Atacar pode ser arriscado; defender reduz bastante o dano.
- **Turno 4:** se a batalha continuar, o chefão volta ao ataque comum.

*O dano ainda é aleatório dentro das faixas informadas. Entender o padrão do adversário não garante vitória, mas ajuda a tomar decisões melhores.*

## Desafios extras — escolha pelo menos um

- **Modo fúria:** quando o chefão chega a 75 PV ou menos, seus ataques ficam mais fortes. Indique isso na interface.
- **Novo chefão:** adicione um inimigo com vida, aparência ou padrão de ataque diferente (sem quebrar as regras obrigatórias do chefe original).
- **Habilidade especial do jogador:** crie um quarto botão e implemente uma ação com custo ou limitação, incluindo o novo valor de ação na validação do servidor.
- **Histórico de combate:** exiba as últimas ações e seus resultados, em vez de mostrar somente a mensagem atual.
- **Animações:** indique visualmente dano, defesa, cura e golpe especial, sem alterar as regras no JavaScript.

## Orientações de desenvolvimento

**Back-end — regras do jogo**  
Abra `src/Batalha.java` e complete os TODOs 1 a 3. Preste atenção à contagem de turnos: o golpe especial ocorre nos turnos múltiplos de 3, contando apenas as ações válidas. O aviso do **próximo** golpe já tem um campo reservado no JSON.

**Integração JavaScript ↔ Java**  
Abra `web/app.js` e conclua os TODOs 4 a 8. A página está em `web/index.html`, e o estilo em `web/style.css`.

**Personalização e evolução**  
Altere a identidade visual e implemente pelo menos um desafio extra. A interface precisa continuar mostrando o aviso dos golpes especiais corretamente.

**Testes e demonstração**  
Teste uma batalha completa. Tente usar cura sem poções, atacar depois do encerramento e reiniciar. Confira se os golpes especiais acontecem exatamente nos turnos anunciados.

## Como executar

Requisito: JDK 17 ou superior. **Não precisa de Maven, PHP, Spring Boot ou banco de dados.**

No Windows, execute `INICIAR_WINDOWS.bat`.  
No Linux/macOS, execute `bash INICIAR_LINUX_MAC.sh`.

Alternativamente, na pasta principal do projeto:

```text
javac -encoding UTF-8 -d out src/*.java
java -cp out ServidorArena
```

No **PowerShell**, use `src\*.java` em vez de `src/*.java`, se necessário.

Abra **http://localhost:8080** no navegador. Não abra `web/index.html` diretamente pelo explorador de arquivos, pois o jogo depende do servidor Java.

## Entendendo a API

| Método | Rota | O que faz |
| --- | --- | --- |
| GET | `/api/estado` | Devolve os PV e o estado da batalha, incluindo o aviso do próximo golpe |
| POST | `/api/jogar` | Recebe `acao` e executa um turno no Java |
| POST | `/api/reiniciar` | Reinicia o combate |

Exemplo de resposta de `/api/estado`, antes do 3º turno:

```json
{
  "jogadorVida": 75,
  "jogadorMax": 100,
  "chefeVida": 107,
  "chefeMax": 150,
  "pocoes": 2,
  "turno": 3,
  "encerrado": false,
  "resultado": "em_andamento",
  "mensagem": "Você atacou. O chefão revidou com um ataque comum.",
  "aviso": "⚠️ ATENÇÃO! O próximo contra-ataque será ESPECIAL!"
}
```

Exemplo de `fetch()` para adaptar:

```javascript
const resposta = await fetch('/api/jogar', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: new URLSearchParams({ acao: 'atacar' })
});
const dados = await resposta.json();
```

## Entrega e avaliação (10 pontos)

- **4 pontos:** regras de combate, golpe especial, defesa, cura e condições de vitória/derrota corretas.
- **2 pontos:** integração navegador ↔ Java e atualização das informações, incluindo o aviso de ataque especial.
- **2 pontos:** organização, nomes compreensíveis e regras de combate no Java, não no JavaScript.
- **1 ponto:** personalização visual e pelo menos um desafio extra implementado.
- **1 ponto:** demonstração, testes e justificativa das decisões técnicas.

**Na apresentação, a equipe deverá conseguir explicar como o navegador se comunica com o servidor e justificar a divisão de responsabilidades entre Java e JavaScript.** Outras perguntas poderão ser feitas sobre o código desenvolvido.
