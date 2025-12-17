.PHONY: help start stop restart logs build clean test keys status

# Colores para la terminal
GREEN  := \033[0;32m
YELLOW := \033[1;33m
NC     := \033[0m # No Color

# Detectar si necesita sudo
DOCKER_CMD := $(shell docker ps > /dev/null 2>&1 && echo "docker" || echo "sudo docker")
COMPOSE_CMD := $(shell $(DOCKER_CMD) compose version > /dev/null 2>&1 && echo "$(DOCKER_CMD) compose" || echo "$(DOCKER_CMD)-compose")

help: ## Muestra esta ayuda
	@echo "$(GREEN)Logiflow - Sistema de Logística Distribuido$(NC)"
	@echo ""
	@echo "Comandos disponibles:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-15s$(NC) %s\n", $$1, $$2}'

start: ## Inicia todos los servicios
	@echo "$(GREEN)🚀 Iniciando servicios...$(NC)"
	@./start.sh

stop: ## Detiene todos los servicios
	@echo "$(YELLOW)🛑 Deteniendo servicios...$(NC)"
	@$(COMPOSE_CMD) down

restart: stop start ## Reinicia todos los servicios

logs: ## Muestra logs de todos los servicios
	@$(COMPOSE_CMD) logs -f

logs-auth: ## Muestra logs del Auth Service
	@$(COMPOSE_CMD) logs -f auth-service

logs-fleet: ## Muestra logs del Fleet Service
	@$(COMPOSE_CMD) logs -f fleet-service

logs-kong: ## Muestra logs del Kong Gateway
	@$(COMPOSE_CMD) logs -f kong

build: ## Construye las imágenes Docker
	@echo "$(GREEN)🏗️  Construyendo imágenes...$(NC)"
	@$(COMPOSE_CMD) build

clean: ## Limpia contenedores, volúmenes e imágenes
	@echo "$(YELLOW)🧹 Limpiando contenedores y volúmenes...$(NC)"
	@$(COMPOSE_CMD) down -v
	@$(DOCKER_CMD) system prune -f

test: ## Ejecuta tests del Auth Service
	@echo "$(GREEN)🧪 Ejecutando tests...$(NC)"
	@cd src && npm test

test-watch: ## Ejecuta tests en modo watch
	@cd src && npm run test:watch

keys: ## Genera claves JWT
	@echo "$(GREEN)🔑 Generando claves JWT...$(NC)"
	@mkdir -p src/keys
	@openssl ecparam -genkey -name prime256v1 -noout -out src/keys/private.pem
	@openssl ec -in src/keys/private.pem -pubout -out src/keys/public.pem
	@chmod 600 src/keys/private.pem
	@chmod 644 src/keys/public.pem
	@echo "$(GREEN)✅ Claves generadas en src/keys/$(NC)"

status: ## Muestra el estado de los servicios
	@echo "$(GREEN)📊 Estado de los servicios:$(NC)"
	@$(COMPOSE_CMD) ps

check-kong: ## Verifica la configuración de Kong
	@echo "$(GREEN)🔍 Servicios configurados en Kong:$(NC)"
	@curl -s http://localhost:8001/services | jq '.data[] | {name: .name, url: .url}'
	@echo ""
	@echo "$(GREEN)🔍 Rutas configuradas en Kong:$(NC)"
	@curl -s http://localhost:8001/routes | jq '.data[] | {name: .name, paths: .paths}'

test-endpoints: ## Prueba los endpoints principales
	@echo "$(GREEN)🧪 Probando endpoints...$(NC)"
	@echo ""
	@echo "$(YELLOW)Auth Service (directo):$(NC)"
	@curl -s http://localhost:4000/api-docs.json > /dev/null && echo "✅ OK" || echo "❌ FAIL"
	@echo ""
	@echo "$(YELLOW)Kong Gateway:$(NC)"
	@curl -s http://localhost:8001/status > /dev/null && echo "✅ OK" || echo "❌ FAIL"
	@echo ""
	@echo "$(YELLOW)Auth a través de Kong:$(NC)"
	@curl -s http://localhost:8000/api/auth/health > /dev/null && echo "✅ OK" || echo "❌ FAIL"

db-auth: ## Accede a la base de datos del Auth Service
	@docker exec -it logiflow_auth_db psql -U postgres -d logiflow_auth

db-fleet: ## Accede a la base de datos del Fleet Service
	@docker exec -it logiflow_fleet_db psql -U fleetUser -d FleetDB

shell-auth: ## Abre shell en el contenedor del Auth Service
	@docker exec -it logiflow_auth_service sh

shell-fleet: ## Abre shell en el contenedor del Fleet Service
	@docker exec -it logiflow_fleet_service sh

shell-kong: ## Abre shell en el contenedor de Kong
	@docker exec -it logiflow_kong_gateway sh
