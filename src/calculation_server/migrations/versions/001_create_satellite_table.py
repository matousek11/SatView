"""create satellite table

Revision ID: 001
Revises: 
Create Date: 2024-04-16 19:30:00.000000

"""
from alembic import op
import sqlalchemy as sa


revision = '001'
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        'Satellite',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('satellite_id', sa.Integer(), nullable=False),
        sa.Column('name', sa.String(), nullable=False),
        sa.Column('tle', sa.String(), nullable=False),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('satellite_id')
    )

    # Create an index for faster lookups
    op.create_index(
        'ix_satellite_satellite_id',
        'Satellite',
        ['satellite_id']
    )


def downgrade() -> None:
    op.drop_index('ix_satellite_satellite_id')
    op.drop_table('Satellite') 