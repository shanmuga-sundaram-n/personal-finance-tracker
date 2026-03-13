.PHONY: start stop build logs clean ps

start: ## Build images and start all services
	docker compose up --build -d
	@echo ""
	@echo "Services started:"
	@echo "  Frontend:  http://localhost:3000"
	@echo "  Backend:   http://localhost:8080/swagger-ui.html"
	@echo ""
	docker compose ps

stop: ## Stop all services
	docker compose down

build: ## Build images without starting
	docker compose build

logs: ## Follow logs for all services (Ctrl+C to exit)
	docker compose logs -f

clean: ## Stop services and remove volumes (deletes all DB data)
	docker compose down -v

ps: ## Show status of all services
	docker compose ps
