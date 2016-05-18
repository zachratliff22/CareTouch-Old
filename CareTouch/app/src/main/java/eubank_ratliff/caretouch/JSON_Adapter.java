package eubank_ratliff.caretouch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class JSON_Adapter {
    public String directions = "https://maps.googleapis.com/maps/api/directions/json?origin=Toronto&destination=Montreal&key=AIzaSyAgWlQf-ja4uw1Vec-5hLVwNYn18QCFT-Q\n";
    public String geocode = "https://maps.googleapis.com/maps/api/geocode/json?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA&key=AIzaSyAgWlQf-ja4uw1Vec-5hLVwNYn18QCFT-Q\n";
    public JSONObject getJSON(String address) throws IOException, JSONException {

        URL data = new URL(address);
        final HttpURLConnection connection = (HttpURLConnection) data.openConnection();

        connection.setReadTimeout(4000);
        connection.setConnectTimeout(1000);
        connection.setRequestMethod("GET");
        connection.setAllowUserInteraction(false);
        connection.connect();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();

            JSONObject json_data = new JSONObject(sb.toString());
            return json_data;
        }

        return null;
    }

}