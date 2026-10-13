# Consumidor de notificacoes do lab (Desafio 4): simula o servico externo de notificacao.
FROM python:3.12-slim
WORKDIR /app
ENV PIP_DISABLE_PIP_VERSION_CHECK=1 PIP_NO_CACHE_DIR=1 PYTHONUNBUFFERED=1
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY consumidor.py .
EXPOSE 8085
CMD ["python", "consumidor.py"]
