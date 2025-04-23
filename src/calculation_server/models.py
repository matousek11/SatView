from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey, Sequence, Double
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship

Base = declarative_base()

class Satellite(Base):
    __tablename__ = 'Satellite'

    id = Column(Integer, primary_key=True)
    satellite_id = Column(String, nullable=False, unique=True)
    name = Column(String, nullable=False)
    tle = Column(String, nullable=False)

    positions = relationship("SatellitePosition", back_populates="satellite")

    def __repr__(self):
        return f"<Satellite(satellite_id='{self.satellite_id}', name='{self.name}')>"

class SatellitePosition(Base):
    __tablename__ = 'SatellitePosition'

    id = Column(Integer, Sequence('satellite_position_id_seq'), primary_key=True)
    satellite_id = Column(Integer, ForeignKey('Satellite.id'), primary_key=True)
    time = Column(DateTime, primary_key=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    height = Column(Float, nullable=False)
    
    # Relationship to satellite
    satellite = relationship("Satellite", back_populates="positions")

    def __repr__(self):
        return f"<SatellitePosition(satellite_id={self.satellite_id}, time='{self.time}', lat={self.latitude}, lon={self.longitude}, height={self.height})>" 