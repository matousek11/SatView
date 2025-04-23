import os
from logging.config import fileConfig
from pathlib import Path
from dotenv import load_dotenv

from sqlalchemy import engine_from_config
from sqlalchemy import pool

from alembic import context

import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from models import Base

# Load environment variables from .env file
# Look for .env in the project root (3 levels up from migrations/env.py)
env_path = Path(__file__).parents[3] / '.env'
load_dotenv(env_path)

# Print the path and whether the file exists for debugging
print(f"Looking for .env file at: {env_path}")
print(f"File exists: {env_path.exists()}")

# Alembic Config object, which provides
# access to the values within the .ini file in use
config = context.config

# Interpret the config file for Python logging
# This line sets up loggers
if config.config_file_name is not None:
    fileConfig(config.config_file_name)

target_metadata = Base.metadata

def get_url():
    # Try DATABASE_URL first, then fall back to DB_DSN
    url = os.getenv("DATABASE_URL") or os.getenv("DB_DSN")
    if not url:
        raise ValueError("Neither DATABASE_URL nor DB_DSN environment variable is set")
    
    # If using DB_DSN, replace timescaledb with localhost for local development
    if "timescaledb" in url:
        url = url.replace("timescaledb", "localhost")
    
    return url

def run_migrations_offline() -> None:
    """
    Run migrations in 'offline' mode.

    This configures the context with just a URL
    and not an Engine, though an Engine is acceptable
    here as well.  By skipping the Engine creation
    we don't even need a DBAPI to be available.

    Calls to context.execute() here emit the given string to the
    script output.

    """
    url = get_url()
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )

    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    """
    Run migrations in 'online' mode.

    Create an Engine and associate a connection with the context.
    """
    configuration = config.get_section(config.config_ini_section)
    configuration["sqlalchemy.url"] = get_url()
    connectable = engine_from_config(
        configuration,
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )

    with connectable.connect() as connection:
        context.configure(
            connection=connection, target_metadata=target_metadata
        )

        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online() 