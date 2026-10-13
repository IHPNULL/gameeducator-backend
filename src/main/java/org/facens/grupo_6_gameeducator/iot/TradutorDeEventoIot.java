package org.facens.grupo_6_gameeducator.iot;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Camada anticorrupcao: converte o payload compacto do dispositivo + topico MQTT (routing key AMQP) no
 * modelo canonico. E o unico lugar que conhece o formato dos dispositivos; trocar o firmware muda so aqui.
 *
 * <p>Topico MQTT {@code gameeducator/lab/{lab}/dispositivo/{id}/evento} chega como routing key
 * {@code gameeducator.lab.{lab}.dispositivo.{id}.evento}. Payload: {"id":"uuid","t":"P|I|C","a":42,"atv":"...","ts":epochSeg}.
 */
public class TradutorDeEventoIot {

    private static final Pattern ROTA = Pattern.compile("^gameeducator\\.lab\\.([\\w-]+)\\.dispositivo\\.([\\w-]+)\\.evento$");

    private final JsonMapper json;

    public TradutorDeEventoIot(JsonMapper json) {
        this.json = json;
    }

    public EventoDeLaboratorio traduzir(String routingKey, byte[] corpo) {
        Matcher rota = ROTA.matcher(routingKey == null ? "" : routingKey);
        if (!rota.matches()) {
            throw new EventoIotInvalidoException("Topico fora do padrao: " + routingKey);
        }
        JsonNode no;
        try {
            no = json.readTree(corpo);
        } catch (JacksonException e) {
            throw new EventoIotInvalidoException("Payload nao e JSON valido");
        }
        String eventoId = texto(no, "id");
        if (eventoId.length() > 36) {
            throw new EventoIotInvalidoException("id do evento maior que 36 caracteres");
        }
        TipoEventoIot tipo = TipoEventoIot.doCodigo(texto(no, "t"));
        if (!no.path("a").canConvertToLong() || !no.path("ts").canConvertToLong()) {
            throw new EventoIotInvalidoException("Campos obrigatorios 'a' (aluno) e 'ts' (epoch) ausentes ou invalidos");
        }
        String atividade = no.hasNonNull("atv") ? no.get("atv").asString() : null;
        return new EventoDeLaboratorio(eventoId, rota.group(1), rota.group(2), tipo, no.get("a").asLong(),
                atividade, Instant.ofEpochSecond(no.get("ts").asLong()));
    }

    private static String texto(JsonNode no, String campo) {
        if (!no.hasNonNull(campo) || no.get(campo).asString().isBlank()) {
            throw new EventoIotInvalidoException("Campo obrigatorio ausente: " + campo);
        }
        return no.get(campo).asString();
    }
}
