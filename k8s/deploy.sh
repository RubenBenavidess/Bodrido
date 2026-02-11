#!/bin/bash
set -e

echo "🔑 Creando secretos para las claves JWT..."
# Crear secret logiflow-keys desde los archivos locales en auth-ms/keys
# Usamos --dry-run=client -o yaml | kubectl apply -f - para idempotencia (no fallar si ya existe)
kubectl create secret generic logiflow-keys \
  --from-file=ec_private.pem=./auth-ms/keys/ec_private.pem \
  --from-file=ec_public.pem=./auth-ms/keys/ec_public.pem \
  --dry-run=client -o yaml | kubectl apply -f -

echo "💾 Desplegando bases de datos..."
kubectl apply -f k8s/01-databases.yaml

echo "🐰 Desplegando RabbitMQ..."
kubectl apply -f k8s/02-rabbitmq.yaml

echo "⏳ Esperando unos segundos para que RabbitMQ y DBs inicien (opcional, K8s reintenta)..."
sleep 5

echo "🚀 Desplegando microservicios Backend..."
kubectl apply -f k8s/03-backend-services.yaml

echo "🔗 Desplegando GraphQL BFF..."
kubectl apply -f k8s/04-graphql-bff.yaml

echo "🚪 Desplegando Kong Gateway..."
kubectl apply -f k8s/05-kong.yaml

echo "✅ Todo desplegado! Usa 'kubectl get pods' para ver el estado."
echo "🌍 Para acceder a Kong desde fuera, asegúrate de correr 'minikube tunnel' en otra terminal."
