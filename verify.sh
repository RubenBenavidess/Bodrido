#!/usr/bin/env bash

set -e

echo "🔍 Verificando configuración del proyecto Logiflow"
echo ""

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

ERRORS=0
WARNINGS=0

# Función para verificar archivos
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✅ $1${NC}"
    else
        echo -e "${RED}❌ $1 - NO ENCONTRADO${NC}"
        ((ERRORS++))
    fi
}

# Función para verificar directorios
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✅ $1/${NC}"
    else
        echo -e "${YELLOW}⚠️  $1/ - NO ENCONTRADO${NC}"
        ((WARNINGS++))
    fi
}

echo "📁 Verificando estructura de archivos:"
echo ""

# Archivos raíz
check_file "docker-compose.yaml"
check_file "Makefile"
check_file "start.sh"
check_file "README.md"
check_file ".dockerignore"
check_file ".env.example"

echo ""

# Gateway
echo "🌐 Gateway (Kong):"
check_dir "gateway"
check_file "gateway/kong.yml"

echo ""

# Auth Service
echo "🔐 Auth Service (Node.js):"
check_dir "src"
check_file "src/Dockerfile"
check_file "src/package.json"
check_file "src/server.js"

# Verificar claves JWT
if [ -f "src/keys/private.pem" ] && [ -f "src/keys/public.pem" ]; then
    echo -e "${GREEN}✅ src/keys/private.pem${NC}"
    echo -e "${GREEN}✅ src/keys/public.pem${NC}"
    
    # Verificar permisos
    PERMS=$(stat -c "%a" src/keys/private.pem 2>/dev/null || stat -f "%OLp" src/keys/private.pem 2>/dev/null)
    if [ "$PERMS" = "600" ] || [ "$PERMS" = "400" ]; then
        echo -e "${GREEN}✅ Permisos de private.pem correctos ($PERMS)${NC}"
    else
        echo -e "${YELLOW}⚠️  Permisos de private.pem: $PERMS (recomendado: 600)${NC}"
        ((WARNINGS++))
    fi
else
    echo -e "${RED}❌ src/keys/private.pem - NO ENCONTRADO${NC}"
    echo -e "${RED}❌ src/keys/public.pem - NO ENCONTRADO${NC}"
    echo -e "${YELLOW}   Ejecuta: make keys${NC}"
    ((ERRORS++))
fi

echo ""

# Fleet Service
echo "🚛 Fleet Service (.NET):"
check_dir "FleetService"
check_file "FleetService/Dockerfile"
check_file "FleetService/FleetService.csproj"

echo ""

# Verificar Docker
echo "🐋 Verificando Docker:"
if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version)
    echo -e "${GREEN}✅ Docker instalado: $DOCKER_VERSION${NC}"
else
    echo -e "${RED}❌ Docker no está instalado${NC}"
    ((ERRORS++))
fi

if command -v docker-compose &> /dev/null || docker compose version &> /dev/null 2>&1; then
    if command -v docker-compose &> /dev/null; then
        COMPOSE_VERSION=$(docker-compose --version)
    else
        COMPOSE_VERSION=$(docker compose version)
    fi
    echo -e "${GREEN}✅ Docker Compose instalado: $COMPOSE_VERSION${NC}"
else
    echo -e "${RED}❌ Docker Compose no está instalado${NC}"
    ((ERRORS++))
fi

echo ""

# Verificar puertos disponibles
echo "🔌 Verificando puertos disponibles:"
PORTS=(4000 5000 5432 5433 8000 8001)
for PORT in "${PORTS[@]}"; do
    if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  Puerto $PORT está en uso${NC}"
        ((WARNINGS++))
    else
        echo -e "${GREEN}✅ Puerto $PORT disponible${NC}"
    fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Resumen
if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}🎉 Todo está correcto! Puedes iniciar el proyecto con:${NC}"
    echo -e "   ${YELLOW}./start.sh${NC} o ${YELLOW}make start${NC}"
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  Hay $WARNINGS advertencia(s), pero puedes continuar${NC}"
    echo -e "   ${YELLOW}./start.sh${NC} o ${YELLOW}make start${NC}"
else
    echo -e "${RED}❌ Se encontraron $ERRORS error(es) críticos${NC}"
    echo -e "   Corrige los errores antes de continuar"
    exit 1
fi

echo ""
