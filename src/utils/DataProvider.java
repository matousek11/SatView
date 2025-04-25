package utils;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataProvider {
    private final String SEPARATOR = ";";
    private ArrayList<Integer> satelliteIDs;
    private Map<Integer, String> satelliteNames;

    /**
     * Key is satelliteID from DB and value is longitude;latitude;height
     */
    private Map<String, String> satellitePositions;

    public DataProvider(ArrayList<Integer> satelliteIDs) {
        this.satellitePositions = new HashMap<>();
        this.satelliteNames = new HashMap<>();
        this.satelliteIDs = satelliteIDs;
        satelliteIDs.forEach(satelliteID -> {
            loadPositionsFromDb(satelliteID, (int)(System.currentTimeMillis() / 1000));
        });
    }

    /**
     * Fetches all satellites from the database and returns a map of satellite IDs to their names
     *
     * @return HashMap where key is satellite ID and value is satellite name
     */
    public Map<Integer, String> getSatelliteNames() {
        String query = "SELECT id, satellite_id, name FROM \"Satellite\"";

        if (!satelliteNames.isEmpty()) {
            return satelliteNames;
        }

        try {
            ResultSet rs = DatabaseConnection.executeQuery(query);
            while (rs.next()) {
                int satelliteID = rs.getInt("satellite_id");
                String name = rs.getString("name");
                satelliteNames.put(satelliteID, name + SEPARATOR + satelliteID);
            }
        } catch (SQLException e) {
            System.out.println("Error fetching satellite names: " + e.getMessage());
        }

        return satelliteNames;
    }

    private void loadPositionsFromDb(int satelliteID, int timestamp) {
        String query = """
            SELECT *
            FROM "SatellitePosition"
            WHERE time >= ? AND time < ?
              AND satellite_id = ?
        """;

        try {
            String startTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.ofEpochSecond((long) timestamp - 5 * 60 * 60));
            String endTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.ofEpochSecond((long) timestamp + 5 * 60 * 60));

            ResultSet rs = DatabaseConnection.executeQuery(query, 
                Timestamp.valueOf(startTime),
                Timestamp.valueOf(endTime),
                satelliteID
            );

            while (rs.next()) {
                int positionTimestamp = (int) (rs.getTimestamp("time").getTime() / 1000);
                double latitude = rs.getDouble("latitude");
                double longitude = rs.getDouble("longitude");
                double height = rs.getDouble("height");

                satellitePositions.put(satelliteID + String.valueOf(positionTimestamp),
                    longitude + SEPARATOR + latitude + SEPARATOR + height);
            }
        } catch (SQLException e) {
            System.out.println("Error loading positions from DB: " + e.getMessage());
        }
    }

    /**
     * returns closest position record for selected satellite in selected timestamp
     */
    public float[] getRecord(int satelliteID, int timestamp) {
        // TODO: implement load from db on demand

        // only position for each minute is calculated => round timestamp into the closest minute
        int closestMinuteTimestamp = (timestamp / 60);
        closestMinuteTimestamp *= 60;

        String data = satellitePositions.get(String.valueOf(satelliteID) + closestMinuteTimestamp);
        String nextData = satellitePositions.get(String.valueOf(satelliteID) + (closestMinuteTimestamp + 60));
        if (data == null || nextData == null) {
            return new float[0];
        }

        String[] satellitePosition = data.split(SEPARATOR);
        String[] nextSatellitePosition = nextData.split(SEPARATOR);
        float t = (float) (timestamp - closestMinuteTimestamp) / 60;

        return interpolatePosition(satellitePosition, nextSatellitePosition, t);
    }

    private float[] interpolatePosition(String[] satellitePosition, String[] nextSatellitePosition, float t) {
        float currentLongitude = Float.parseFloat(satellitePosition[0]);
        float nextLongitude = Float.parseFloat(nextSatellitePosition[0]);
        float currentLatitude = Float.parseFloat(satellitePosition[1]);
        float nextLatitude = Float.parseFloat(nextSatellitePosition[1]);
        float currentHeight = Float.parseFloat(satellitePosition[2]);
        float nextHeight = Float.parseFloat(nextSatellitePosition[2]);

        // Fix for longitude wraparound
        if (Math.abs(nextLongitude - currentLongitude) > 180) {
            if (currentLongitude > nextLongitude) {
                nextLongitude += 360;
            } else {
                currentLongitude += 360;
            }
        }

        float longitude = currentLongitude + (nextLongitude - currentLongitude) * t;
        longitude = ((longitude + 180) % 360 + 360) % 360 - 180; // Normalize to [-180, 180]

        float latitude = currentLatitude + (nextLatitude - currentLatitude) * t;
        float height = currentHeight + (nextHeight - currentHeight) * t;

        return new float[]{longitude, latitude, height};
    }
}
