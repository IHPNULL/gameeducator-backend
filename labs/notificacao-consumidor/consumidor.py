"""Servico de notificacao SIMULADO (lab do Desafio 4). Representa o consumidor externo do case.

Recebe eventos por dois canais, para comparar as alternativas do ADR-B1-004:
  - HTTP POST /notificacoes  -> usado pelo Core no modo SINCRONO (alternativa descartada);
  - fila AMQP notificacao.eventos -> usada no modo ASSINCRONO (outbox + RabbitMQ, decisao).
Deduplica por eventoId (entrega "pelo menos uma vez"). Payload invalido -> nack sem requeue -> DLQ.

GET /notificacoes  -> resumo do que foi recebido (evidencia do lab)
GET /metrics       -> metricas Prometheus (notificacoes_recebidas_total{canal})
"""
import json
import logging
import os
import threading
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import pika
from prometheus_client import CONTENT_TYPE_LATEST, Counter, generate_latest

logging.basicConfig(level=logging.INFO, format="%(asctime)s [notificacao] %(message)s")
log = logging.getLogger()

RABBITMQ_URL = os.getenv("RABBITMQ_URL", "amqp://gameeducator:gameeducator@localhost:5672/%2F")
FILA = os.getenv("FILA", "notificacao.eventos")
PORTA = int(os.getenv("PORTA", "8085"))

RECEBIDAS = Counter("notificacoes_recebidas", "Notificacoes processadas", ["canal"])
DUPLICADAS = Counter("notificacoes_duplicadas", "Entregas repetidas ignoradas", ["canal"])
INVALIDAS = Counter("notificacoes_invalidas", "Mensagens rejeitadas para a DLQ", ["canal"])

trava = threading.Lock()
vistos: set[str] = set()
ultimas: list[dict] = []
iniciado_em = datetime.now(timezone.utc).isoformat()


def registrar(canal: str, evento: dict, tipo: str | None) -> bool:
    evento_id = evento.get("eventoId")
    with trava:
        if evento_id in vistos:
            DUPLICADAS.labels(canal).inc()
            return False
        vistos.add(evento_id)
        ultimas.append({"canal": canal, "tipo": tipo, "eventoId": evento_id,
                        "alunoId": evento.get("alunoId"), "recebidoEm": datetime.now(timezone.utc).isoformat()})
        del ultimas[:-50]
    RECEBIDAS.labels(canal).inc()
    log.info("notificacao enviada (%s) %s aluno=%s evento=%s", canal, tipo, evento.get("alunoId"), evento_id)
    return True


class Http(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def _responder(self, status, corpo, tipo="application/json"):
        dados = corpo if isinstance(corpo, bytes) else json.dumps(corpo).encode()
        self.send_response(status)
        self.send_header("Content-Type", tipo)
        self.send_header("Content-Length", str(len(dados)))
        self.end_headers()
        self.wfile.write(dados)

    def do_POST(self):
        if self.path != "/notificacoes":
            return self._responder(404, {"erro": "nao encontrado"})
        corpo = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        try:
            registrar("http", json.loads(corpo), self.headers.get("X-Evento-Tipo"))
        except ValueError:
            INVALIDAS.labels("http").inc()
            return self._responder(400, {"erro": "json invalido"})
        self._responder(202, {"status": "aceito"})

    def do_GET(self):
        if self.path == "/metrics":
            return self._responder(200, generate_latest(), CONTENT_TYPE_LATEST)
        if self.path == "/saude":
            return self._responder(200, {"status": "UP"})
        if self.path == "/notificacoes":
            with trava:
                por_canal = {}
                for item in ultimas:
                    por_canal[item["canal"]] = por_canal.get(item["canal"], 0) + 1
                return self._responder(200, {"iniciadoEm": iniciado_em, "totalUnicas": len(vistos),
                                             "porCanalUltimas": por_canal, "ultimas": ultimas[-10:]})
        self._responder(404, {"erro": "nao encontrado"})


def consumir_amqp():
    while True:
        try:
            conexao = pika.BlockingConnection(pika.URLParameters(RABBITMQ_URL))
            canal = conexao.channel()
            canal.basic_qos(prefetch_count=10)

            def ao_receber(ch, metodo, propriedades, corpo):
                try:
                    registrar("amqp", json.loads(corpo), propriedades.type)
                    ch.basic_ack(metodo.delivery_tag)
                except ValueError:
                    INVALIDAS.labels("amqp").inc()
                    ch.basic_nack(metodo.delivery_tag, requeue=False)  # -> notificacao.eventos.dlq

            canal.basic_consume(queue=FILA, on_message_callback=ao_receber)
            log.info("consumindo a fila %s", FILA)
            canal.start_consuming()
        except pika.exceptions.AMQPError as erro:
            log.warning("broker indisponivel (%s); nova tentativa em 3s", erro.__class__.__name__)
            time.sleep(3)


if __name__ == "__main__":
    threading.Thread(target=consumir_amqp, daemon=True).start()
    log.info("HTTP em :%d", PORTA)
    ThreadingHTTPServer(("0.0.0.0", PORTA), Http).serve_forever()
