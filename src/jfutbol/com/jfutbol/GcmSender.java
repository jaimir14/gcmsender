/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jfutbol.com.jfutbol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public class GcmSender {

	private static Logger log = LogManager.getRootLogger();
	public static final String API_KEY = "AIzaSyDdTSuVxzgo_5kMBILO96LDdxPjyRXT6cU";
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://nt71li6axbkq1q6a.cbetxkdyhwsb.us-east-1.rds.amazonaws.com:3306/dhcgqhdy14b0gvt3";
	// Database credentials
	static final String USER = "mb6u49ef19m2p9gl";
	static final String PASS = "xqbw0lfr1zkttj4d";

	public static void main(String[] args) {

		do {

			Connection conn = null;
			Statement stmt = null;

			try {
				// STEP 2: Register JDBC driver
				Class.forName(JDBC_DRIVER);
				// STEP 3: Open a connection
				// System.out.println("Connecting to database...");
				conn = DriverManager.getConnection(DB_URL, USER, PASS);
				// STEP 4: Execute a query
				// System.out.println("Creating statement...");
				stmt = conn.createStatement();
				String sql;
				sql = "SELECT userId, COUNT(id) notificationCounter FROM notifications WHERE sentByGCM=0 GROUP BY userId";
				ResultSet rs = stmt.executeQuery(sql);
				// STEP 5: Extract data from result set
				while (rs.next()) {
					log.info("Notification found");
					// Retrieve by column name
					int userId = rs.getInt("userId");
					int notificationCounter = rs.getInt("notificationCounter");
					// Display values
					// System.out.print("userId: " + userId);
					// System.out.print(", notificationCounter: " +
					// notificationCounter);
					SendNotification(userId, notificationCounter);
				}
				// STEP 6: Clean-up environment
				rs.close();
				stmt.close();
				conn.close();
			} catch (SQLException se) {
				// Handle errors for JDBC
				log.error(se.getMessage());
				se.printStackTrace();
			} catch (Exception e) {
				// Handle errors for Class.forName
				log.error(e.getMessage());
				e.printStackTrace();
			} finally {
				// finally block used to close resources
				try {
					if (stmt != null)
						stmt.close();
				} catch (SQLException se2) {
					log.error(se2.getMessage());
				}// nothing we can do
				try {
					if (conn != null)
						conn.close();
				} catch (SQLException se) {
					log.error(se.getMessage());
					se.printStackTrace();
				}// end finally try
			}// end try
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}
		} while (1 != 0);
	}

	public static void SendNotification(int userId, int notificationCounter) {
		String msg[] = new String[2];

		if (notificationCounter > 1)
			msg = new String[] {
					"Tienes " + notificationCounter + " nuevas notificaciones.",
					"/topics/" + userId };
		else
			msg = new String[] {
					"Tienes " + notificationCounter + " nueva notificacion.",
					"/topics/" + userId };

		if (msg.length < 1 || msg.length > 2 || msg[0] == null) {
			System.err
					.println("usage: ./gradlew run -Pmsg=\"MESSAGE\" [-Pto=\"DEVICE_TOKEN\"]");
			System.err.println("");
			System.err
					.println("Specify a test message to broadcast via GCM. If a device's GCM registration token is\n"
							+ "specified, the message will only be sent to that device. Otherwise, the message \n"
							+ "will be sent to all devices subscribed to the \"global\" topic.");
			System.err.println("");
			System.err
					.println("Example (Broadcast):\n"
							+ "On Windows:   .\\gradlew.bat run -Pmsg=\"<Your_Message>\"\n"
							+ "On Linux/Mac: ./gradlew run -Pmsg=\"<Your_Message>\"");
			System.err.println("");
			System.err
					.println("Example (Unicast):\n"
							+ "On Windows:   .\\gradlew.bat run -Pmsg=\"<Your_Message>\" -Pto=\"<Your_Token>\"\n"
							+ "On Linux/Mac: ./gradlew run -Pmsg=\"<Your_Message>\" -Pto=\"<Your_Token>\"");
			System.exit(1);
		}
		try {
			// Prepare JSON containing the GCM message content. What to send and
			// where to send.
			JSONObject jGcmData = new JSONObject();
			JSONObject jData = new JSONObject();
			jData.put("message", msg[0].trim());
			// Where to send GCM message.
			if (msg.length > 1 && msg[1] != null) {
				jGcmData.put("to", msg[1].trim());
			} else {
				jGcmData.put("to", "/topics/global");
			}
			// What to send in GCM message.
			jGcmData.put("data", jData);

			// Create connection to send GCM Message request.
			URL url = new URL("https://android.googleapis.com/gcm/send");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Authorization", "key=" + API_KEY);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);

			// Send GCM message content.
			OutputStream outputStream = conn.getOutputStream();
			outputStream.write(jGcmData.toString().getBytes());

			// Read GCM response.
			InputStream inputStream = conn.getInputStream();
			String resp = IOUtils.toString(inputStream);
			updateNotificationAsSent(userId);
			System.out.println(resp);
			System.out
					.println("Check your device/emulator for notification or logcat for "
							+ "confirmation of the receipt of the GCM message.");
		} catch (IOException e) {
			System.out.println("Unable to send GCM message.");
			System.out
					.println("Please ensure that API_KEY has been replaced by the server "
							+ "API key, and that the device's registration token is correct (if specified).");

			log.error(e.getMessage());
			e.printStackTrace();
		}

	}

	public static void updateNotificationAsSent(int userId) {
		Connection conn = null;
		Statement stmt = null;

		try {
			// STEP 2: Register JDBC driver
			Class.forName(JDBC_DRIVER);
			// STEP 3: Open a connection
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
			// STEP 4: Execute a query
			System.out.println("Creating statement...");
			stmt = conn.createStatement();
			String sql;
			sql = "UPDATE notifications SET sentByGCM=1 WHERE userId=" + userId;
			int rs = stmt.executeUpdate(sql);
			// STEP 5: Extract data from result set
			if (rs > 0) {
				// System.out.print("Notifications sent to userId: "+userId);
			}
			// STEP 6: Clean-up environment
			stmt.close();
			conn.close();
		} catch (SQLException se) {
			// Handle errors for JDBC
			log.error(se.getMessage());
			se.printStackTrace();
		} catch (Exception e) {
			// Handle errors for Class.forName
			log.error(e.getMessage());
			e.printStackTrace();
		} finally {
			// finally block used to close resources
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException se2) {
				log.error(se2.getMessage());
			}// nothing we can do
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				log.error(se.getMessage());
				se.printStackTrace();
			}// end finally try
		}// end try
	}

}
