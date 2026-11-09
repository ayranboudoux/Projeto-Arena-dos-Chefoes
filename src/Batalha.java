import java.util.LinkedList;
import java.util.Random;

/**
 * Regras do jogo. TODA a lógica de combate fica aqui, no Java.
 * O JavaScript só envia a ação e desenha o JSON devolvido por estadoJson().
 */
public class Batalha {

    /**
     * Dificuldade escolhida na tela inicial (extra da equipe).
     * Muda SOMENTE o bônus de dano da fúria. Os danos-base da regra obrigatória
     * (18-25, 11-17, 32-42), vidas, poções, defesa e cura são iguais em todos os níveis.
     */
    public enum Dificuldade {
        FACIL("facil", "Fácil", 1),
        MEDIO("medio", "Médio", 2),
        DIFICIL("dificil", "Difícil", 3),
        IMPOSSIVEL("impossivel", "Impossível", 5);

        final String id;
        final String nome;
        final int bonusFuria;   // dano extra por golpe do chefão enquanto ele está em fúria

        Dificuldade(String id, String nome, int bonusFuria) {
            this.id = id;
            this.nome = nome;
            this.bonusFuria = bonusFuria;
        }

        /** Converte o texto recebido do navegador; devolve null se não existir. */
        static Dificuldade deId(String id) {
            for (Dificuldade d : values()) {
                if (d.id.equals(id)) return d;
            }
            return null;
        }
    }

    private static final int VIDA_MAX_JOGADOR = 100;
    private static final int VIDA_MAX_CHEFE = 150;
    private static final int LIMITE_FURIA = 75;     // chefão com 75 PV ou menos entra em fúria
    private static final int TAMANHO_HISTORICO = 6;

    private int vidaJogador = VIDA_MAX_JOGADOR;
    private int vidaChefe = VIDA_MAX_CHEFE;
    private int pocoes = 2;
    private Dificuldade dificuldade = Dificuldade.MEDIO;   // padrão: Médio
    private int turno = 1;                           // turno que está prestes a ser jogado
    private boolean encerrado = false;
    private String resultado = "em_andamento";
    private String mensagem = "A batalha começou! Escolha sua primeira ação.";

    private final Random sorteio = new Random();
    private final LinkedList<String> historico = new LinkedList<>();
    private boolean defendendo = false;              // vale somente para o turno atual
    private String relatoJogador = "";
    private String relatoChefe = "";

    /** Executa um turno completo. Ações inválidas ou após o fim não alteram o estado da batalha. */
    public synchronized String agir(String acao) {
        if (encerrado) return estadoJson();                     // depois do fim, nada muda
        if (!executarAcaoJogador(acao)) return estadoJson();    // ação inválida: não gasta turno
        int turnoJogado = turno;
        contraAtacar();
        fecharTurno();
        registrarHistorico(turnoJogado, mensagem);
        return estadoJson();
    }

    /** Reinicia mantendo a dificuldade atual. */
    public synchronized String reiniciar() {
        restaurar();
        return estadoJson();
    }

    /** Reinicia já com a dificuldade escolhida. Texto desconhecido mantém a dificuldade atual. */
    public synchronized String reiniciar(String idDificuldade) {
        Dificuldade escolhida = Dificuldade.deId(idDificuldade);
        if (escolhida != null) dificuldade = escolhida;
        restaurar();
        return estadoJson();
    }

    private void restaurar() {
        vidaJogador = VIDA_MAX_JOGADOR;
        vidaChefe = VIDA_MAX_CHEFE;
        pocoes = 2;
        turno = 1;
        encerrado = false;
        resultado = "em_andamento";
        mensagem = "Nova batalha! Boa sorte.";
        defendendo = false;
        relatoJogador = "";
        relatoChefe = "";
        historico.clear();
    }

    private int sortear(int min, int max) {
        return min + sorteio.nextInt(max - min + 1);
    }

    // ---------------------------------------------------------------- TODO 1
    /** Aplica a ação do jogador. Devolve true se a ação foi válida (gasta o turno). */
    private boolean executarAcaoJogador(String acao) {
        defendendo = false;
        relatoChefe = "";
        switch (acao) {
            case "atacar":
                vidaChefe = Math.max(0, vidaChefe - sortear(18, 25));
                relatoJogador = "Você atacou o chefão.";
                return true;
            case "defender":
                defendendo = true;
                relatoJogador = "Você levantou a guarda.";
                return true;
            case "curar":
                if (pocoes <= 0) {
                    mensagem = "Você não tem mais poções!";
                    return false;
                }
                if (vidaJogador >= VIDA_MAX_JOGADOR) {
                    mensagem = "Sua vida já está cheia!";
                    return false;
                }
                vidaJogador = Math.min(VIDA_MAX_JOGADOR, vidaJogador + sortear(26, 34));
                pocoes--;
                relatoJogador = "Você bebeu uma poção e recuperou energia.";
                return true;
            default:
                mensagem = "Ação desconhecida.";
                return false;
        }
    }

    // ---------------------------------------------------------------- TODO 2
    /** Contra-ataque do chefão: comum, ou especial nos turnos múltiplos de 3. */
    private void contraAtacar() {
        if (vidaChefe <= 0) return;                    // chefão morto não revida
        boolean especial = (turno % 3 == 0);           // turno ainda NÃO incrementado
        boolean furia = emFuria();
        int dano = especial ? sortear(32, 42) : sortear(11, 17);
        if (furia) dano += dificuldade.bonusFuria;     // extra: modo fúria (bônus depende da dificuldade)
        if (defendendo) dano = Math.max(1, dano / 4);  // um quarto, para baixo, mínimo 1
        vidaJogador = Math.max(0, vidaJogador - dano);
        relatoChefe = especial
                ? "O chefão desferiu o golpe ESPECIAL!"
                : "O chefão revidou com um ataque comum.";
        if (defendendo) relatoChefe += " Seu escudo amorteceu o impacto.";
        if (furia) relatoChefe += " O chefão está em FÚRIA!";
    }

    // ---------------------------------------------------------------- TODO 3
    /** Verifica vitória/derrota e avança o turno somente se a batalha continua. */
    private void fecharTurno() {
        if (vidaChefe <= 0) {
            encerrado = true;
            resultado = "vitoria";
            mensagem = relatoJogador + " O chefão foi derrotado!";
        } else if (vidaJogador <= 0) {
            encerrado = true;
            resultado = "derrota";
            mensagem = relatoJogador + " " + relatoChefe + " Você caiu em batalha...";
        } else {
            mensagem = relatoJogador + " " + relatoChefe;
            turno++;
        }
    }

    // ---------------------------------------------------------------- extras
    private boolean emFuria() {
        return vidaChefe > 0 && vidaChefe <= LIMITE_FURIA;
    }

    private void registrarHistorico(int turnoJogado, String texto) {
        historico.addFirst("Turno " + turnoJogado + ": " + texto);
        while (historico.size() > TAMANHO_HISTORICO) {
            historico.removeLast();
        }
    }

    private String historicoJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < historico.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append('"').append(escapar(historico.get(i))).append('"');
        }
        return sb.append("]").toString();
    }

    // Este aviso do PRÓXIMO contra-ataque usa a mesma regra de contraAtacar().
    private String avisoProximoTurno() {
        if (encerrado) return "Batalha encerrada.";
        if (turno % 3 == 0) {
            return "⚠️ ATENÇÃO! O próximo contra-ataque será ESPECIAL!";
        }
        return "Próximo contra-ataque: COMUM.";
    }

    /** Contrato da API: não altere os nomes das propriedades sem atualizar o JS. */
    public synchronized String estadoJson() {
        return "{" +
            "\"jogadorVida\":" + vidaJogador + "," +
            "\"jogadorMax\":100," +
            "\"chefeVida\":" + vidaChefe + "," +
            "\"chefeMax\":150," +
            "\"furia\":" + emFuria() + "," +
            "\"dificuldade\":\"" + dificuldade.id + "\"," +
            "\"dificuldadeNome\":\"" + escapar(dificuldade.nome) + "\"," +
            "\"pocoes\":" + pocoes + "," +
            "\"turno\":" + turno + "," +
            "\"encerrado\":" + encerrado + "," +
            "\"resultado\":\"" + escapar(resultado) + "\"," +
            "\"mensagem\":\"" + escapar(mensagem) + "\"," +
            "\"aviso\":\"" + escapar(avisoProximoTurno()) + "\"," +
            "\"proximoEspecial\":" + (!encerrado && turno % 3 == 0) + "," +
            "\"historico\":" + historicoJson() +
            "}";
    }

    private String escapar(String texto) {
        return texto.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
