#!/usr/bin/env bash

set -e

echo "🚀 Iniciando Logiflow - Sistema de Logística Distribuido"
echo ""

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Verificar si Docker está instalado
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker no está instalado${NC}"
    echo "   Instala Docker desde: https://docs.docker.com/get-docker/"
    exit 1
fi

# Verificar si Docker Compose está instalado y determinar el comando a usar
DOCKER_COMPOSE="docker compose"
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
elif ! docker compose version &> /dev/null 2>&1; then
    echo -e "${RED}Error: Docker Compose no está instalado${NC}"
    echo "   Instala Docker Compose desde: https://docs.docker.com/compose/install/"
    exit 1
fi

# Verificar si necesita sudo para ejecutar docker
if ! docker ps &> /dev/null; then
    if sudo docker ps &> /dev/null 2>&1; then
        echo -e "${YELLOW}Requiere permisos de sudo para Docker${NC}"
        DOCKER_COMPOSE="sudo $DOCKER_COMPOSE"
    else
        echo -e "${RED}Error: No tienes permisos para usar Docker${NC}"
        echo "   Opción 1: Agregar tu usuario al grupo docker:"
        echo "   sudo usermod -aG docker \$USER"
        echo "   newgrp docker"
        echo ""
        echo "   Opción 2: Ejecutar con sudo:"
        echo "   sudo ./start.sh"
        exit 1
    fi
fi

# Verificar si las claves JWT existen
if [ ! -f "src/keys/private.pem" ] || [ ! -f "src/keys/public.pem" ]; then
    echo -e "${YELLOW}⚠️  Advertencia: No se encontraron las claves JWT en src/keys/${NC}"
    echo ""
    read -p "¿Deseas generar las claves automáticamente? (s/n): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Ss]$ ]]; then
        echo -e "${GREEN}🔑 Generando claves JWT...${NC}"
        mkdir -p src/keys
        openssl ecparam -genkey -name prime256v1 -noout -out src/keys/private.pem
        openssl ec -in src/keys/private.pem -pubout -out src/keys/public.pem
        chmod 600 src/keys/private.pem
        chmod 644 src/keys/public.pem
        echo -e "${GREEN}✅ Claves generadas exitosamente${NC}"
        echo ""
    else
        echo -e "${RED}❌ No se pueden iniciar los servicios sin las claves JWT${NC}"
        echo "   Genera las claves manualmente o ejecuta este script de nuevo"
        exit 1
    fi
fi

# Detener contenedores existentes
echo -e "${YELLOW}🔄 Deteniendo contenedores existentes...${NC}"
$DOCKER_COMPOSE down 2>/dev/null || true
echo ""

# Construir imágenes
echo -e "${GREEN}🏗️  Construyendo imágenes Docker...${NC}"
$DOCKER_COMPOSE build
echo ""

# Levantar servicios
echo -e "${GREEN}🚢 Levantando servicios...${NC}"
$DOCKER_COMPOSE up -d
echo ""

# Esperar a que los servicios estén listos
echo -e "${YELLOW}⏳ Esperando a que los servicios estén listos...${NC}"
sleep 10

# Verificar estado de los servicios
echo ""
echo -e "${GREEN}📊 Estado de los servicios:${NC}"
$DOCKER_COMPOSE ps
echo ""

# Verificar conectividad
echo -e "${GREEN}🔍 Verificando conectividad...${NC}"
echo ""

# Auth Service
if curl -s http://localhost:4000/api-docs.json > /dev/null; then
    echo -e "${GREEN}✅ Auth Service: OK${NC}"
else
    echo -e "${RED}❌ Auth Service: No responde${NC}"
fi

# Fleet Service
if curl -s http://localhost:5000/health > /dev/null 2>&1 || curl -s http://localhost:5000 > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Fleet Service: OK${NC}"
else
    echo -e "${YELLOW}⚠️  Fleet Service: No responde (puede estar iniciando)${NC}"
fi

# Kong Gateway
if curl -s http://localhost:8001/status > /dev/null; then
    echo -e "${GREEN}✅ Kong Gateway: OK${NC}"
else
    echo -e "${RED}❌ Kong Gateway: No responde${NC}"
fi

echo ""
echo -e "${GREEN}🎉 Logiflow iniciado exitosamente!${NC}"
echo ""
echo "📚 URLs de acceso:"
echo "   • Auth Service:      http://localhost:4000"
echo "   • Auth Swagger:      http://localhost:4000/api-docs"
echo "   • Fleet Service:     http://localhost:5000"
echo "   • Kong Gateway:      http://localhost:8000"
echo "   • Kong Admin:        http://localhost:8001"
echo ""
echo "🧪 Prueba los endpoints a través de Kong:"
echo "   • Auth API:          http://localhost:8000/api/auth"
echo "   • Fleet API:         http://localhost:8000/api/fleet"
echo ""
echo "📋 Ver logs en tiempo real:"
echo "   $DOCKER_COMPOSE logs -f"
echo ""
echo "🛑 Detener servicios:"
echo "   $DOCKER_COMPOSE down"
echo ""
