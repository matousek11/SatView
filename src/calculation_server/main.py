import os
import sys
from typing import Dict

import requests
from sqlalchemy import create_engine, Engine
from sqlalchemy.orm import sessionmaker, Session
import time
import logging
from datetime import datetime, timedelta
from skyfield.api import utc

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from models import Base, Satellite
from satellite_utils import calculate_positions, save_positions_to_db

# Setup logging per module
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

API_KEY = os.getenv('N2YO_API_KEY')
BASE_URL = 'https://api.n2yo.com/rest/v1/satellite'

DATABASE_URL = os.getenv('DATABASE_URL')

def get_engine() -> Engine:
    """
    create connection to the database
    """
    return create_engine(DATABASE_URL)

def get_session() -> Session:
    """
    return active session to query the database
    """
    engine = get_engine()
    session = sessionmaker(bind=engine)
    return session()

def fetch_satellite_tle(norad_id: int) -> Dict[str, str]|None:
    """Fetch TLE data with satellite info from N2YO API"""
    url = f"{BASE_URL}/tle/{norad_id}&apiKey={API_KEY}"
    
    try:
        response = requests.get(url)
        response.raise_for_status()
        data = response.json()
        
        return {
            'satellite_id': str(data['info']['satid']),
            'name': data['info']['satname'],
            'tle': data['tle']
        }
    except requests.exceptions.RequestException as e:
        logger.error(f"Error fetching data for satellite {norad_id}: {str(e)}")
        return None

def update_or_create_satellite(session: Session, satellite_data: Dict[str, str]) -> Satellite|None:
    """Update or create a satellite record in the database"""
    try:
        satellite = session.query(Satellite).filter_by(
            satellite_id=satellite_data['satellite_id']
        ).first()
        
        if satellite:
            # Update existing satellite
            satellite.name = satellite_data['name']
            satellite.tle = satellite_data['tle']
            logger.info(f"Updated satellite: {satellite.name}")
        else:
            # Create new satellite
            satellite = Satellite(**satellite_data)
            session.add(satellite)
            logger.info(f"Created new satellite: {satellite_data['name']}")
        
        session.commit()
        return satellite
    except Exception as e:
        session.rollback()
        logger.error(f"Database error: {str(e)}")
        return None

def calculate_and_save_positions(session: Session) -> None:
    """Calculate and save positions for all satellites"""
    try:
        satellites = session.query(Satellite).all()
        
        if not satellites:
            logger.info("No satellites found in the database.")
            return

        # Calculate positions for last and next 24 hours
        start_time = datetime.utcnow().replace(second=0, microsecond=0, tzinfo=utc) - timedelta(hours=24)
        end_time = start_time + timedelta(hours=48)
        
        for satellite in satellites:
            logger.info(f"Calculating positions for satellite: {satellite.name}")
            
            try:
                positions = calculate_positions(
                    tle=str(satellite.tle),
                    start_time=start_time,
                    end_time=end_time,
                    interval_minutes=1
                )

                save_positions_to_db(session, satellite, positions)
                logger.info(f"Successfully saved {len(positions)} positions for {satellite.name}")
                
            except Exception as e:
                logger.error(f"Error processing satellite {satellite.name}: {str(e)}")
                continue
                
    except Exception as e:
        logger.error(f"Error in position calculation: {str(e)}")

def main():
    # List of NORAD IDs to track
    satellite_ids = [
        25544,  # ISS (International Space Station)
        27607,  # XM-3 (radio)
        33591,  # NOAA-19
        40019,  # TDRS 12
        43013,  # TESS (Transiting Exoplanet Survey Satellite)
        62391,  # Lasarsat
    ]
    
    session = get_session()
    
    #while True:
    for norad_id in satellite_ids:
        logger.info(f"Fetching data for satellite {norad_id}")
            
        satellite_data = fetch_satellite_tle(norad_id)
        if satellite_data:
            update_or_create_satellite(session, satellite_data)
        time.sleep(1)

    logger.info("Starting position calculations...")
    calculate_and_save_positions(session)
    logger.info("Completed position calculations")

    logger.info("Completed update cycle, waiting 15 minutes...")
    time.sleep(900)

if __name__ == "__main__":
    logger.info('Starting satellites update')
    main()
