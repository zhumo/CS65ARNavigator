package edu.dartmouth.com.arnavigation.directions;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class used to parse Directions JSON response from Google Maps Directions API
 */

public class DirectionsParser {
    /**
     * Returns a list of lists containing latitude and longitude from a JSONObject
     */
    public List<List<HashMap<String, String>>> parse(JSONObject jObject) {

        List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String, String>>>();
        JSONArray jRoutes = null;
        JSONArray jLegs = null;
        JSONArray jSteps = null;

        try {
            jRoutes = jObject.getJSONArray("routes");

            // Loop for all routes
            for (int i = 0; i < jRoutes.length(); i++) {
                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                List path = new ArrayList<HashMap<String, String>>();

                //Loop for all legs
                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    //Loop for all steps
                    for (int k = 0; k < jSteps.length(); k++) {
                        String polyline = "";
                        polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                        List list = decodePolyline(polyline);

                        //Loop for all points
                        for (int l = 0; l < list.size(); l++) {
                            HashMap<String, String> hm = new HashMap<String, String>();
                            hm.put("lat", Double.toString(((LatLng) list.get(l)).latitude));
                            hm.put("lon", Double.toString(((LatLng) list.get(l)).longitude));
                            path.add(hm);
                        }
                    }
                    routes.add(path);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }

        return routes;
    }

    /**
     * Method to decode polyline
     * Source : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     */
    private List decodePolyline(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
//
//    //async task to parse JSON response from request
//    private class ParseDirectionsTask extends AsyncTask<String, Void, List<List<HashMap<String, String>>>> {
//        @Override
//        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
//
//            JSONObject jsonObject = null;
//
//            List<List<HashMap<String, String>>> routes = null;
//
//            //use DirectionsParser class to parse JSONObject
//            try {
//                jsonObject = new JSONObject(strings[0]);
//                DirectionsParser directionsParser = new DirectionsParser();
//                routes = directionsParser.parse(jsonObject);
//
//            } catch (JSONException je) {
//                je.printStackTrace();
//            }
//
//            return routes;
//        }
//
//        @Override
//        protected void onPostExecute(List<List<HashMap<String, String>>> paths){
//
//            //draw paths
//            ArrayList waypoints = null;
//
//            polylineOptions = new PolylineOptions();
//
//            if (paths.size() > 0) {
//
//                for (List<HashMap<String, String>> path : paths) {
//                    waypoints = new ArrayList();
//
//                    for (HashMap<String, String> point : path) {
//
//                        //get values from json-created hashmap
//                        Double lat = Double.parseDouble(point.get("lat"));
//                        Double lng = Double.parseDouble(point.get("lon"));
//
//                        //add to waypoints
//                        waypoints.add(new LatLng(lat, lng));
//                    }
//
//                    addToPolyine(waypoints);
//
//                }
//
//
//                //get last position latlng
//                int lastPathIndex = paths.size() - 1;
//                List<HashMap<String, String>> lastPath = paths.get(lastPathIndex);
//                int lastPointIndex = lastPath.size() - 1;
//                HashMap<String, String> lastPointHashMap = lastPath.get(lastPointIndex);
//                LatLng lastLatLng = new LatLng(Double.parseDouble(lastPointHashMap.get("lat")),
//                        Double.parseDouble(lastPointHashMap.get("lon")));
//
//                setLastLocation(lastLatLng);
//            } else {
//                mDestination = mOrigin;
//                setLastLocation(mDestination);
//            }
//
//            //update receivers
//            updateListeners();
//        }
//    }


}