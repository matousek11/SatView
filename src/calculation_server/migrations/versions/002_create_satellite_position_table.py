"""create satellite position table

Revision ID: 002
Revises: 001
Create Date: 2024-04-16 20:00:00.000000

"""
from alembic import op
import sqlalchemy as sa


revision = '002'
down_revision = '001'
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        'SatellitePosition',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('satellite_id', sa.Integer(), nullable=False),
        sa.Column('time', sa.DateTime(), nullable=False),
        sa.Column('latitude', sa.Float(), nullable=False),
        sa.Column('longitude', sa.Float(), nullable=False),
        sa.Column('height', sa.Float(), nullable=False),
        sa.Column('is_orbit', sa.Boolean(), nullable=False),
        sa.PrimaryKeyConstraint('id', 'time'),
        sa.ForeignKeyConstraint(['satellite_id'], ['Satellite.id'], ondelete='CASCADE')
    )

    op.create_index(
        'ix_satellite_position_time',
        'SatellitePosition',
        ['time']
    )

    op.create_index(
        'ix_satellite_position_satellite_id',
        'SatellitePosition',
        ['satellite_id']
    )

    # Convert table to a TimescaleDB hypertable
    op.execute("""
        SELECT create_hypertable('"SatellitePosition"', 'time', 
            if_not_exists => TRUE,
            migrate_data => TRUE
        );
    """)


def downgrade() -> None:
    op.drop_index('ix_satellite_position_satellite_id')
    op.drop_index('ix_satellite_position_time')

    op.drop_table('SatellitePosition') 