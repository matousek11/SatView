import os
import sys
from typing import Dict
import json

import requests
from sqlalchemy import create_engine, Engine
from sqlalchemy.orm import sessionmaker, Session
import time
import logging
from datetime import datetime, timedelta
from skyfield.api import utc

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from models import Satellite
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

def load_satellite_ids_from_json(file_path: str) -> list[int]:
    """
    Load satellite IDs from a JSON file
    """
    try:
        with open(file_path, 'r') as f:
            data = json.load(f)
            return data.get('noradID', [])
    except Exception as e:
        logger.error(f"Error loading JSON file: {str(e)}")
        return [62483, 62484, 62485, 62486, 62487, 62488, 62489, 62490, 62491, 62492, 62493, 62494, 62495, 62496, 62497, 62498, 62499, 62500, 62501, 62502, 62503, 62504, 62505, 62506, 62507, 62508, 62509, 62510, 62511, 62512, 62513, 62514, 62515, 62516, 62517, 62518, 62519, 62520, 62521, 62522, 62523, 62524, 62525, 62526, 62527, 62528, 62529, 62530, 62531, 62532, 62533, 62534, 62535, 62536, 62537, 62538, 62539, 62540, 62541, 62542, 62543, 62544, 62545, 62546, 62547, 62548, 62549, 62550, 62551, 62552, 62553, 62554, 62555, 62556, 62557, 62558, 62559, 62560, 62561, 62562, 62563, 62564, 62565, 62566, 62567, 62568, 62569, 62570, 62571, 62572, 62573, 62574, 62575, 62576, 62577, 62578, 62579, 62580, 62581, 62582, 62583, 62584, 62585, 62586, 62587, 62588, 62589, 62590, 62591, 62592, 62593, 62594, 62595, 62596, 62597, 62598, 62599, 62600, 62601, 62602, 62603, 62604, 62605, 62606, 62607, 62609, 62610, 62611, 62612, 62613, 62614, 62615, 62616, 62617, 62618, 62619, 62620, 62621, 62622, 62623, 62624, 62625, 62626, 62627, 62628, 62629, 62630, 62631, 62632, 62633, 62634, 62635, 62636, 62637, 62638, 62639, 62640, 62641, 62642, 62643, 62644, 62645, 62646, 62647, 62648, 62649, 62650, 62651, 62652, 62653, 62654, 62655, 62656, 62657, 62658, 62659, 62660, 62661, 62662, 62663, 62664, 62665, 62666, 62667, 62668, 62669, 62670, 62671, 62672, 62673, 62674, 62675, 62676, 62677, 62678, 62679, 62680, 62681, 62682, 62683, 62684, 62685, 62686, 62687, 62688, 62689, 62690, 62691, 62692, 62693, 62694, 62695, 62696, 62697, 62698, 62699, 62700, 62701, 62702, 62703, 62704, 62705, 62706, 62707, 62708, 62709, 62710, 62711, 62712, 62713, 62714, 62715, 62716, 62717, 62718, 62719, 62720, 62725, 62726, 62727, 62728, 62729, 62730, 62731, 62732, 62733, 62735, 62736, 62737, 62738, 62739, 62740, 62741, 62742, 62743, 62744, 62745, 62746, 62747, 62748, 62749, 62750, 62751, 62752, 62753, 62754, 62755, 62756, 62757, 62758, 62759, 62760, 62761, 62762, 62763, 62764, 62765, 62766, 62767, 62768, 62769, 62770, 62771, 62772, 62773, 62774, 62775, 62776, 62777, 62778, 62779, 62780, 62781, 62782, 62783, 62784, 62785, 62786, 62787, 62788, 62789, 62790, 62791, 62792, 62793, 62794, 62795, 62796, 62797, 62798, 62799, 62800, 62801, 62802, 62803, 62804, 62805, 62806, 62807, 62808, 62809, 62810, 62811, 62812, 62813, 62814, 62815, 62816, 62817, 62818, 62819, 62820, 62821, 62822, 62823, 62824, 62825, 62826, 62827, 62828, 62829, 62830, 62831, 62832, 62833, 62834, 62835, 62836, 62837, 62838, 62839, 62840, 62841, 62842, 62843, 62844, 62845, 62846, 62847, 62848, 62849, 62850, 62851, 62852, 62853, 63002, 63003, 63004, 63153, 63488]

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

def calculate_and_save_positions(session: Session, is_first_load: bool) -> None:
    """Calculate and save positions for all satellites"""
    try:
        satellites = session.query(Satellite).all()
        total_satellites = len(satellites)
        
        if not satellites:
            logger.info("No satellites found in the database.")
            return

        logger.info(f"Starting position calculations for {total_satellites} satellites...")
        
        # Calculate positions for last and next 12 hours on first load
        # and for the next 24 hours on subsequent loads
        if (is_first_load):
            start_time = datetime.utcnow().replace(second=0, microsecond=0, tzinfo=utc) - timedelta(hours=12)
        else:
            start_time = datetime.utcnow().replace(second=0, microsecond=0, tzinfo=utc)
        end_time = start_time + timedelta(hours=24)
        
        for index, satellite in enumerate(satellites, 1):
            logger.info(f"Calculating positions for satellite {index}/{total_satellites}: {satellite.name}")
            
            try:
                positions = calculate_positions(
                    tle=str(satellite.tle),
                    start_time=start_time,
                    end_time=end_time,
                    interval_minutes=1
                )

                save_positions_to_db(session, satellite, positions)
                logger.info(f"Successfully saved {len(positions)} positions for {satellite.name} ({index}/{total_satellites})")
                
            except Exception as e:
                logger.error(f"Error processing satellite {satellite.name} ({index}/{total_satellites}): {str(e)}")
                continue
                
        logger.info(f"Completed position calculations for {total_satellites} satellites")
                
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
    
    # Load additional satellite IDs from JSON file
    satellite_ids2 = load_satellite_ids_from_json('../../res/january.json')
    print(satellite_ids2)
    # Merge the lists and remove duplicates
    all_satellite_ids = list(set(satellite_ids + satellite_ids2))
    logger.info(f"Total number of satellites to track: {len(all_satellite_ids)}")
    
    session = get_session()
    
    is_first_load = True
    while True:
        for norad_id in all_satellite_ids:
            logger.info(f"Fetching data for satellite {norad_id}")

            satellite_data = fetch_satellite_tle(norad_id)
            if satellite_data:
                update_or_create_satellite(session, satellite_data)
            time.sleep(1)

        logger.info("Starting position calculations...")
        calculate_and_save_positions(session, is_first_load)
        logger.info("Completed position calculations")
        is_first_load = False

        logger.info("Completed update cycle, waiting 12 hours...")
        time.sleep(12 * 60 * 60)

if __name__ == "__main__":
    logger.info('Starting satellites update')
    main()
