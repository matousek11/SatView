from datetime import datetime, timedelta
from typing import List, Tuple, Type
from skyfield.api import load, utc
from skyfield.sgp4lib import EarthSatellite
from sqlalchemy.orm import Session
from models import Satellite, SatellitePosition

def parse_tle(tle: str) -> Tuple[str, str]:
    """Split TLE string into two lines."""
    lines = tle.strip().split('\n')
    if len(lines) != 2:
        raise ValueError("TLE must contain exactly two lines")
    return lines[0], lines[1]

def calculate_positions(
    tle: str,
    start_time: datetime,
    end_time: datetime,
    interval_minutes: int = 1
) -> List[Tuple[datetime, float, float, float]]:
    """
    Calculate satellite positions for a given time range.
    
    Args:
        tle: Two-line element set as a string
        start_time: Start time for calculations (must be in UTC)
        end_time: End time for calculations (must be in UTC)
        interval_minutes: Time interval between calculations in minutes
        
    Returns:
        List of tuples containing (timestamp, latitude, longitude, height_km)
    """
    if start_time.tzinfo is None:
        start_time = start_time.replace(tzinfo=utc)
    if end_time.tzinfo is None:
        end_time = end_time.replace(tzinfo=utc)
    
    line1, line2 = parse_tle(tle)
    satellite = EarthSatellite(line1, line2)

    current_time = start_time
    positions = []
    
    while current_time <= end_time:
        # Convert to Skyfield time
        ts = load.timescale()
        t = ts.from_datetime(current_time)

        geocentric = satellite.at(t)
        subpoint = geocentric.subpoint()

        latitude = subpoint.latitude.degrees
        longitude = subpoint.longitude.degrees
        height_km = subpoint.elevation.km
        
        positions.append((current_time, latitude, longitude, height_km))
        
        # Move to next time point
        current_time += timedelta(minutes=interval_minutes)
    
    return positions

def save_positions_to_db(
    db: Session,
    satellite: Type[Satellite],
    positions: List[Tuple[datetime, float, float, float]]
) -> None:
    """
    Save calculated positions to the database.
    
    Args:
        db: Database session
        satellite: Satellite model instance
        positions: List of position tuples (timestamp, latitude, longitude, height_km)
    """
    # Process positions in smaller batches to avoid memory issues
    batch_size = 100
    for i in range(0, len(positions), batch_size):
        batch = positions[i:i+batch_size]
        try:
            position_objects = []
            for timestamp, latitude, longitude, height in batch:
                position = SatellitePosition(
                    id=int(satellite.id),
                    satellite_id=int(satellite.satellite_id),
                    time=timestamp,
                    latitude=latitude,
                    longitude=longitude,
                    height=height
                )
                position_objects.append(position)

            db.add_all(position_objects)
            db.commit()
            
        except Exception as e:
            db.rollback()
            raise e 