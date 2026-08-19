# Usamos una imagen liviana de Python 3.13
FROM python:3.13-slim

# Evita que Python escriba archivos .pyc en disco y fuerza stdout/stderr sin buffer
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

# Instalar dependencias primero (aprovecha el cache de capas de Docker)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copiar el resto del código de la aplicación
COPY . .

# Exponer el puerto de FastAPI
EXPOSE 8000

# Comando para arrancar Uvicorn en modo producción (0.0.0.0 para escuchar fuera del contenedor)
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]