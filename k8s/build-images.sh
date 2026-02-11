#!/bin/bash
set -e

echo "🔌 Conectando al entorno Docker de Minikube..."
eval $(minikube docker-env)

echo "🏗️  Construyendo imágenes (esto puede tardar unos minutos)..."

echo "   - auth-service..."
docker build -t auth-service:latest ./auth-ms

echo "   - fleet-service..."
docker build -t fleet-service:latest ./FleetService

echo "   - order-service..."
docker build -t order-service:latest ./order-ms

echo "   - billing-service..."
docker build -t billing-service:latest ./billing-ms

echo "   - notification-service..."
docker build -t notification-service:latest ./notification-ms

echo "   - customer-service..."
docker build -t customer-service:latest ./customer-ms

echo "   - graphql-bff..."
docker build -t graphql-bff:latest ./graphql-bff

echo "✅ Todas las imágenes han sido construidas en Minikube."
