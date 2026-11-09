/**
 * Testes automáticos das regras. Execute:
 *   javac -encoding UTF-8 -d out src/*.java
 *   java -cp out TesteBatalha
 */
public class TesteBatalha {
    static int falhas = 0;

    static int num(String json, String campo) {
        String chave = '"' + campo + '"' + ":";
        int i = json.indexOf(chave) + chave.length();
        int f = i;
        while (f < json.length() && (Character.isDigit(json.charAt(f)) || json.charAt(f) == '-')) f++;
        return Integer.parseInt(json.substring(i, f));
    }

    static boolean verdadeiro(String json, String campo) {
        return json.contains('"' + campo + '"' + ":true");
    }

    static void checar(boolean ok, String descricao) {
        if (!ok) {
            falhas++;
            System.out.println("FALHOU: " + descricao);
        }
    }

    // Só defendendo, o chefão nunca perde vida: a perda do jogador revela o tipo de golpe.
    // Comum (11 a 17) vira 2 a 4; especial (32 a 42) vira 8 a 10.
    static void testeEspecialNoTurnoCerto() {
        for (int rep = 0; rep < 300; rep++) {
            Batalha b = new Batalha();
            int vidaAntes = 100;
            for (int turno = 1; turno <= 40; turno++) {
                String json = b.agir("defender");
                if (verdadeiro(json, "encerrado")) break;
                int vidaDepois = num(json, "jogadorVida");
                int perda = vidaAntes - vidaDepois;
                if (turno % 3 == 0) checar(perda >= 8 && perda <= 10, "turno " + turno + " deveria ser especial (perda " + perda + ")");
                else checar(perda >= 2 && perda <= 4, "turno " + turno + " deveria ser comum (perda " + perda + ")");
                checar(num(json, "turno") == turno + 1, "o turno deve avançar a cada ação válida");
                checar(verdadeiro(json, "proximoEspecial") == ((turno + 1) % 3 == 0), "aviso errado após o turno " + turno);
                vidaAntes = vidaDepois;
            }
        }
    }

    static void testeCuraInvalida() {
        Batalha b = new Batalha();
        String json = b.agir("curar");
        checar(num(json, "turno") == 1, "cura com vida cheia não pode gastar turno");
        checar(num(json, "pocoes") == 2, "cura com vida cheia não pode gastar poção");
        checar(num(json, "jogadorVida") == 100, "a vida não pode mudar");
    }

    static void testeInvariantes() {
        java.util.Random r = new java.util.Random();
        String[] acoes = {"atacar", "defender", "curar"};
        for (int jogo = 0; jogo < 2000; jogo++) {
            Batalha b = new Batalha();
            for (int i = 0; i < 100; i++) {
                String antes = b.estadoJson();
                String json = b.agir(acoes[r.nextInt(3)]);
                int jv = num(json, "jogadorVida");
                int cv = num(json, "chefeVida");
                int p = num(json, "pocoes");
                checar(jv >= 0 && jv <= 100, "vida do jogador fora de 0..100");
                checar(cv >= 0 && cv <= 150, "vida do chefão fora de 0..150");
                checar(p >= 0 && p <= 2, "poções fora de 0..2");
                if (verdadeiro(antes, "encerrado")) checar(json.equals(antes), "o estado mudou depois do fim");
                if (cv == 0) checar(verdadeiro(json, "encerrado") && json.contains("vitoria"), "chefão a 0 deve dar vitória");
                if (jv == 0) checar(verdadeiro(json, "encerrado") && json.contains("derrota"), "jogador a 0 deve dar derrota");
            }
        }
    }

    static void testeReiniciar() {
        Batalha b = new Batalha();
        b.agir("atacar");
        b.agir("defender");
        String json = b.reiniciar();
        checar(num(json, "jogadorVida") == 100 && num(json, "chefeVida") == 150, "reiniciar deve restaurar a vida");
        checar(num(json, "pocoes") == 2 && num(json, "turno") == 1, "reiniciar deve restaurar poções e turno");
        checar(json.contains("\"historico\":[]"), "reiniciar deve limpar o histórico");
    }

    static String texto(String json, String campo) {
        String chave = '"' + campo + '"' + ":\"";
        int i = json.indexOf(chave) + chave.length();
        return json.substring(i, json.indexOf('"', i));
    }

    // Dificuldade: só o bônus da fúria muda. Sem fúria, os danos são os da regra obrigatória.
    // Só atacando, a perda de vida do jogador é exatamente o dano do chefão (sem defesa nem cura).
    static void testeDificuldades() {
        String[] ids = {"facil", "medio", "dificil", "impossivel"};
        int[] bonus = {1, 2, 3, 5};
        for (int n = 0; n < ids.length; n++) {
            for (int rep = 0; rep < 500; rep++) {
                Batalha b = new Batalha();
                String json = b.reiniciar(ids[n]);
                checar(texto(json, "dificuldade").equals(ids[n]), "dificuldade deve ser " + ids[n]);
                int vidaAntes = 100;
                for (int i = 0; i < 20; i++) {
                    int turnoJogado = num(json, "turno");
                    json = b.agir("atacar");
                    if (verdadeiro(json, "encerrado")) break;
                    int perda = vidaAntes - num(json, "jogadorVida");
                    boolean furia = num(json, "chefeVida") <= 75;
                    boolean especial = turnoJogado % 3 == 0;
                    int b0 = furia ? bonus[n] : 0;
                    int min = (especial ? 32 : 11) + b0;
                    int max = (especial ? 42 : 17) + b0;
                    checar(perda >= min && perda <= max, ids[n] + ": turno " + turnoJogado + " perda " + perda + " fora de " + min + ".." + max);
                    checar(verdadeiro(json, "furia") == furia, "campo furia incoerente");
                    vidaAntes = num(json, "jogadorVida");
                }
            }
        }
    }

    static void testeEscolhaDeDificuldade() {
        Batalha b = new Batalha();
        checar(texto(b.estadoJson(), "dificuldade").equals("medio"), "o padrão deve ser medio");
        b.reiniciar("impossivel");
        checar(texto(b.estadoJson(), "dificuldade").equals("impossivel"), "reiniciar(\"impossivel\") deve guardar o nível");
        b.reiniciar("nivel-inexistente");
        checar(texto(b.estadoJson(), "dificuldade").equals("impossivel"), "nível inválido mantém o atual");
        b.agir("atacar");
        String json = b.reiniciar();
        checar(texto(json, "dificuldade").equals("impossivel"), "reiniciar() sem nível mantém o atual");
        checar(num(json, "turno") == 1 && num(json, "chefeVida") == 150, "reiniciar com nível restaura os valores");
    }

    public static void main(String[] args) {
        testeEspecialNoTurnoCerto();
        testeCuraInvalida();
        testeInvariantes();
        testeReiniciar();
        testeDificuldades();
        testeEscolhaDeDificuldade();
        System.out.println(falhas == 0 ? "TODOS OS TESTES PASSARAM" : falhas + " falha(s)");
        if (falhas > 0) System.exit(1);
    }
}
