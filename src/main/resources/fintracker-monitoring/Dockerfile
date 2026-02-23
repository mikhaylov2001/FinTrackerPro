FROM prom/prometheus:latest

# копируем конфиг внутрь образа
COPY prometheus.yml /etc/prometheus/prometheus.yml

# Prometheus по умолчанию слушает порт 9090
EXPOSE 9090

CMD [ "--config.file=/etc/prometheus/prometheus.yml" ]

