/*
Copyright (c) 2015-2017 Divested Computing Group

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package info.spotcomms.wlanbackend;

import android.location.Location;
import android.os.Environment;
import android.util.Log;

import org.microg.nlp.api.HelperLocationBackendService;
import org.microg.nlp.api.LocationHelper;
import org.microg.nlp.api.WiFiBackendHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Set;

import gnu.trove.map.hash.THashMap;

public class BackendService extends HelperLocationBackendService implements WiFiBackendHelper.Listener {

    private static final String logingTag = "MergedWiFiNLP";
    private static BackendService instance;
    private static final THashMap<String, String> mergedDB = new THashMap<>();
    private static Location lastLocation;
    private static Location curLocation;
    private static final int minTimeout = 2000;
    private int curTimeout = minTimeout;

    @Override
    protected final synchronized void onOpen() {
        super.onOpen();
        addHelper(new WiFiBackendHelper(this, this));
        instance = this;
        fillDatabase();
        Log.d(logingTag, "Initialized");
    }

    @Override
    protected final synchronized void onClose() {
        Log.d(logingTag, "Exiting");
        super.onClose();
        if (instance == this) {
            instance = null;
        }
    }

    @Override
    public final void onWiFisChanged(Set<WiFiBackendHelper.WiFi> networks) {
        if (shouldUpdateLocation()) {
            if (networks.size() > 0) {
                Log.d(logingTag, "Networks available");
                int ct = 0;
                double lat = 0;
                double lon = 0;
                for (WiFiBackendHelper.WiFi curWifi : networks) {
                    String bssid = curWifi.getBssid().replaceAll(":", "").toUpperCase();
                    if (mergedDB.containsKey(bssid)) {
                        String result = mergedDB.get(bssid);
                        if (result != null) {
                            String[] curWifiN = result.split(":");
                            lat += Double.valueOf(curWifiN[0]);
                            lon += Double.valueOf(curWifiN[1]);
                            ct++;
                        }
                    }
                }
                if (lat != 0 && lon != 0) { //plz don't be at the center of the world
                    lat = lat / ct;
                    lon = lon / ct;
                    lastLocation = curLocation;
                    curLocation = LocationHelper.create("MergedWiFiBackend", lat, lon, 150);
                    curLocation.setTime(System.currentTimeMillis());
                    report(curLocation);
                    Log.d(logingTag, "Networks Found: " + networks.size() + ", Networks Known: " + ct + ", Current Location: " + lat + " " + lon);
                } else {
                    Log.d(logingTag, "No networks found in database");
                }
            } else {
                Log.d(logingTag, "No networks found");
            }
        } else {
            report(curLocation); //We probably didn't change location, report the old location to save some battery
        }
    }

    private boolean shouldUpdateLocation() {
        if (curLocation == null || System.currentTimeMillis() - curLocation.getTime() >= getTimeout()) {
            Log.d(logingTag, "Location update needed");
            return true;
        }
        Log.d(logingTag, "Location update not needed");
        return false;
    }

    private int getTimeout() {
        if (lastLocation != null && lastLocation.getLatitude() == curLocation.getLatitude() && lastLocation.getLongitude() == curLocation.getLongitude()) {
            int timeoutFactor = 2;
            int maxTimeout = 15000;
            if (curTimeout * timeoutFactor <= maxTimeout) {
                curTimeout *= timeoutFactor;
            } else {
                curTimeout = maxTimeout;
            }
        } else {
            curTimeout = minTimeout;
        }
        Log.d(logingTag, "Current timeout is " + curTimeout);
        return curTimeout;
    }

    private void fillDatabase() {
        Log.d(logingTag, "Attempting to fill database");
        try {
            BufferedReader wifidb = new BufferedReader(new FileReader(new File(getApplicationContext().getFilesDir(), "WPSDB.csv")));
            String line;
            mergedDB.clear();
            while ((line = wifidb.readLine()) != null) {
                String[] wifi = line.split(":");
                mergedDB.put(wifi[0], wifi[1] + ":" + wifi[2]);
            }
            wifidb.close();
            System.gc();
            Log.d(logingTag, "Loaded " + mergedDB.size() + " WiFi networks");
        } catch (Exception e) {
            Log.d(logingTag, "Failed to fill database", e);
        }
    }
}
