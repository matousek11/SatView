FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

COPY src/calculation_server/requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY src/calculation_server/ /app/src/

# Set Python path
ENV PYTHONPATH=/app

CMD ["python", "-u", "/app/src/main.py"]