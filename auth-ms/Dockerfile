# Dockerfile para auth-ms
FROM node:18-alpine

# Instalamos dependencias necesarias para compilar módulos de node si fuera necesario
RUN apk add --no-cache python3 make g++

WORKDIR /app

# Copiar archivos de dependencias y aprovechar el cache de capas de Docker
COPY package*.json ./
# Usamos 'npm ci' si tienes un package-lock.json, es más rápido y seguro en CI/CD
RUN npm install 

# Copiar el resto del código
COPY . .

# Variables de entorno por defecto
ENV NODE_ENV=production

EXPOSE 4000

# Usamos un script de entrada para el seed y el inicio
# Primero el seed, luego el servidor
CMD ["sh", "-c", "npm start"]