"""Simulador de dispositivo de laboratorio com recursos limitados e conexao instavel (Desafio 5).

Publica eventos de presenca/inicio/conclusao em MQTT QoS 1 com payload compacto e sessao persistente.
Para testar a decisao arquitetural, o roteiro simula:
  1. uma QUEDA de conexao no meio do lote: eventos gerados offline vao para um buffer local;
  2. a RECONEXAO: o buffer e descarregado (nada se perde);
  3. DUPLICIDADE: reenvia eventos ja publicados com o mesmo id (como uma reentrega QoS 1);
  4. um payload INVALIDO (firmware com bug) - deve ir para a DLQ, nao para o dominio.
Imprime um resumo JSON (inclui bytes por evento MQTT x REST, evidencia de eficiencia de comunicacao).
"""
import argparse
import json
import random
import time
import uuid
from collections import deque

import paho.mqtt.client as mqtt


def args():
    p = argparse.ArgumentParser()
    p.add_argument("--host", default="localhost")
    p.add_argument("--porta", type=int, default=1883)
    p.add_argument("--usuario", default="dispositivo")
    p.add_argument("--senha", default="dispositivo")
    p.add_argument("--laboratorio", default="lab-01")
    p.add_argument("--dispositivo", default="porta-01")
    p.add_argument("--eventos", type=int, default=20, help="eventos gerados no total")
    p.add_argument("--queda-apos", type=int, default=8, help="cai a conexao depois de N eventos")
    p.add_argument("--eventos-offline", type=int, default=5, help="eventos gerados durante a queda")
    p.add_argument("--duplicados", type=int, default=2)
    p.add_argument("--invalidos", type=int, default=1)
    p.add_argument("--intervalo", type=float, default=0.05)
    return p.parse_args()


def conectar(a):
    cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=f"{a.laboratorio}-{a.dispositivo}",
                          clean_session=False, protocol=mqtt.MQTTv311)
    cliente.username_pw_set(a.usuario, a.senha)
    cliente.will_set(f"gameeducator/lab/{a.laboratorio}/dispositivo/{a.dispositivo}/status", "offline", qos=1)
    for _ in range(30):
        try:
            cliente.connect(a.host, a.porta, keepalive=30)
            break
        except OSError:
            time.sleep(2)
    else:
        raise SystemExit("broker MQTT inacessivel")
    cliente.loop_start()
    return cliente


def main():
    a = args()
    topico = f"gameeducator/lab/{a.laboratorio}/dispositivo/{a.dispositivo}/evento"
    cliente = conectar(a)
    publicados, buffer = [], deque()
    resumo = {"gerados": 0, "publicadosOnline": 0, "bufferizadosOffline": 0, "descarregadosAposReconexao": 0,
              "duplicadosEnviados": 0, "invalidosEnviados": 0}

    def publicar(payload: str):
        info = cliente.publish(topico, payload, qos=1)
        info.wait_for_publish(timeout=10)
        if not info.is_published():
            raise SystemExit("broker nao confirmou (PUBACK) a publicacao QoS 1")

    online, offline_restantes = True, 0
    for i in range(a.eventos):
        evento = {"id": str(uuid.uuid4()), "t": random.choice("PIC"), "a": 1000 + i % 25, "atv": "redes-1",
                  "ts": int(time.time())}
        payload = json.dumps(evento, separators=(",", ":"))
        resumo["gerados"] += 1
        if online:
            publicar(payload)
            publicados.append(payload)
            resumo["publicadosOnline"] += 1
        else:
            buffer.append(payload)
            resumo["bufferizadosOffline"] += 1
        if online and resumo["publicadosOnline"] == a.queda_apos and a.eventos_offline > 0:
            cliente.loop_stop()
            cliente.disconnect()
            online, offline_restantes = False, a.eventos_offline
            print(f"-- conexao perdida apos {a.queda_apos} eventos; gerando offline (buffer local)")
        elif not online:
            offline_restantes -= 1
            if offline_restantes == 0:
                cliente = conectar(a)
                online = True
                print(f"-- reconectado; descarregando {len(buffer)} eventos do buffer")
                while buffer:
                    payload = buffer.popleft()
                    publicar(payload)
                    publicados.append(payload)
                    resumo["descarregadosAposReconexao"] += 1
        time.sleep(a.intervalo)

    for payload in publicados[: a.duplicados]:
        publicar(payload)  # mesmo id: simula reentrega QoS 1 / reenvio apos timeout de PUBACK
        resumo["duplicadosEnviados"] += 1
    for _ in range(a.invalidos):
        publicar('{"t":"P"}')
        resumo["invalidosEnviados"] += 1

    cliente.loop_stop()
    cliente.disconnect()

    exemplo = publicados[0]
    # PUBLISH QoS1: header fixo (2) + tamanho do topico (2) + topico + packet id (2) + payload
    bytes_mqtt = 2 + 2 + len(topico) + 2 + len(exemplo)
    requisicao_rest = (f"POST /api/laboratorios/{a.laboratorio}/eventos HTTP/1.1\r\nHost: core:8080\r\n"
                       f"Content-Type: application/json\r\nAuthorization: Bearer <token-do-dispositivo>\r\n"
                       f"Content-Length: {len(exemplo)}\r\n\r\n{exemplo}")
    resumo.update({"eventosUnicosEsperadosNoCore": resumo["gerados"], "topico": topico, "payloadExemplo": exemplo,
                   "bytesPorEventoMqtt": bytes_mqtt, "bytesPorEventoRestMinimo": len(requisicao_rest)})
    print(json.dumps(resumo, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
