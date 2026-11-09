import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Infraestrutura pronta: servidor HTTP simples, sem Maven ou frameworks. */
public class ServidorArena {
    private static final Batalha batalha = new Batalha();

    public static void main(String[] args) throws Exception {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        servidor.createContext("/", ServidorArena::tratarRequisicao);
        servidor.setExecutor(null);
        servidor.start();
        System.out.println("ARENA ONLINE: abra http://localhost:" + porta);
        System.out.println("Para encerrar, pressione Ctrl+C neste terminal.");
    }

    private static void tratarRequisicao(HttpExchange req) throws IOException {
        try {
            String metodo = req.getRequestMethod();
            String rota = req.getRequestURI().getPath();
            if (rota.equals("/api/estado") && metodo.equals("GET")) {
                responder(req, 200, "application/json", batalha.estadoJson());
            } else if (rota.equals("/api/jogar") && metodo.equals("POST")) {
                String corpo = new String(req.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String acao = extrairCampo(corpo, "acao");
                if (!acao.matches("atacar|defender|curar")) {
                    responder(req, 400, "application/json", "{\"erro\":\"Ação inválida\"}");
                    return;
                }
                responder(req, 200, "application/json", batalha.agir(acao));
            } else if (rota.equals("/api/reiniciar") && metodo.equals("POST")) {
                String corpo = new String(req.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String dificuldade = extrairCampo(corpo, "dificuldade");
                if (dificuldade.isEmpty()) {                         // sem campo: mantém a dificuldade atual
                    responder(req, 200, "application/json", batalha.reiniciar());
                } else if (Batalha.Dificuldade.deId(dificuldade) == null) {
                    responder(req, 400, "application/json", "{\"erro\":\"Dificuldade inválida\"}");
                } else {
                    responder(req, 200, "application/json", batalha.reiniciar(dificuldade));
                }
            } else if (metodo.equals("GET")) {
                servirPagina(req, rota);
            } else {
                responder(req, 405, "text/plain", "Método não permitido");
            }
        } catch (Exception erro) {
            erro.printStackTrace();
            responder(req, 500, "application/json", "{\"erro\":\"Erro interno do servidor\"}");
        } finally {
            req.close();
        }
    }

    /** Lê um campo do corpo "application/x-www-form-urlencoded" (ex.: acao=atacar). */
    private static String extrairCampo(String corpo, String nome) {
        for (String campo : corpo.split("&")) {
            String[] partes = campo.split("=", 2);
            if (partes.length == 2 && partes[0].equals(nome)) {
                return URLDecoder.decode(partes[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static void servirPagina(HttpExchange req, String rota) throws IOException {
        String arquivo = rota.equals("/") ? "index.html" : rota.substring(1);
        if (!arquivo.equals("index.html") && !arquivo.equals("style.css") && !arquivo.equals("app.js")) {
            responder(req, 404, "text/plain", "Página não encontrada");
            return;
        }
        Path caminho = Path.of("web", arquivo);
        if (!Files.isRegularFile(caminho)) {
            responder(req, 404, "text/plain", "Arquivo não encontrado: " + arquivo);
            return;
        }
        String mime = arquivo.endsWith(".css") ? "text/css" :
                      arquivo.endsWith(".js") ? "text/javascript" : "text/html";
        byte[] dados = Files.readAllBytes(caminho);
        req.getResponseHeaders().set("Content-Type", mime + "; charset=utf-8");
        req.sendResponseHeaders(200, dados.length);
        try (var saida = req.getResponseBody()) { saida.write(dados); }
    }

    private static void responder(HttpExchange req, int status, String mime, String conteudo) throws IOException {
        byte[] dados = conteudo.getBytes(StandardCharsets.UTF_8);
        req.getResponseHeaders().set("Content-Type", mime + "; charset=utf-8");
        req.sendResponseHeaders(status, dados.length);
        try (var saida = req.getResponseBody()) { saida.write(dados); }
    }
}
