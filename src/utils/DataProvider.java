package utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataProvider {
    private ArrayList<Integer> satelliteIDs;

    /**
     * Key is satelliteID from DB and value is longitude;latitude;height
     */
    private Map<String, String> satellitePositions;

    public DataProvider(ArrayList<Integer> satelliteIDs) {
        this.satellitePositions = new HashMap<>();
        this.satelliteIDs = satelliteIDs;
        satelliteIDs.forEach(satelliteID -> loadFromDb(satelliteID, (int)(System.currentTimeMillis() / 1000)));
    }

    public void loadFromDb(int satelliteID, int timestamp) {
        String url = "jdbc:postgresql://localhost:5432/satview";
        String user = "postgres";
        String password = "postgres";

        String query = """
            SELECT *
            FROM "SatellitePosition"
            WHERE time >= ? AND time < ?
              AND satellite_id = ?
        """;

        try {
            Class.forName("org.postgresql.Driver");

            Connection connection = DriverManager.getConnection(url, user, password);

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setTimestamp(1, Timestamp.valueOf("2025-04-21 10:00:00"));
            statement.setTimestamp(2, Timestamp.valueOf("2025-04-22 23:00:00"));
            statement.setInt(3, satelliteID);

            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                int positionTimestamp = (int) (rs.getTimestamp("time").getTime() / 1000);
                double latitude = rs.getDouble("latitude");
                double longitude = rs.getDouble("longitude");
                double height = rs.getDouble("height");

                satellitePositions.put(id + String.valueOf(positionTimestamp), longitude + ";" + latitude + ";" + height);
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("error: " + e.getMessage());
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

        String[] satellitePosition = data.split(";");
        String[] nextSatellitePosition = nextData.split(";");
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
