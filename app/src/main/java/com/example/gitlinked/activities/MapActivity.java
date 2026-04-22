package com.example.gitlinked.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gitlinked.R;
import com.example.gitlinked.database.UserDao;
import com.example.gitlinked.models.User;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Map screen showing developer locations and heatmap.
 * Uses Google Maps API with markers for nearby developers.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        userDao = new UserDao(this);

        // Back button
        findViewById(R.id.btn_map_back).setOnClickListener(v -> finish());

        // Load map
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;

        // Dark map style
        try {
            String darkStyle = "[" +
                    "{\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#212121\"}]}," +
                    "{\"elementType\":\"labels.text.fill\",\"stylers\":[{\"color\":\"#757575\"}]}," +
                    "{\"elementType\":\"labels.text.stroke\",\"stylers\":[{\"color\":\"#212121\"}]}," +
                    "{\"featureType\":\"road\",\"elementType\":\"geometry.fill\",\"stylers\":[{\"color\":\"#2c2c2c\"}]}," +
                    "{\"featureType\":\"water\",\"elementType\":\"geometry\",\"stylers\":[{\"color\":\"#0e1626\"}]}" +
                    "]";
            googleMap.setMapStyle(new MapStyleOptions(darkStyle));
        } catch (Exception e) {
            // Fallback to default style
        }

        // Add developer markers
        addDeveloperMarkers();

        // Center on Bangalore (default location for demo)
        LatLng bangalore = new LatLng(12.9716, 77.5946);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bangalore, 14f));

        // Enable zoom controls
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
    }

    private void addDeveloperMarkers() {
        List<User> users = userDao.getAllUsers();

        // Add mock heatmap points if no users
        if (users.isEmpty()) {
            addMockMarkers();
            return;
        }

        for (User user : users) {
            if (user.getLatitude() != 0 && user.getLongitude() != 0) {
                LatLng position = new LatLng(user.getLatitude(), user.getLongitude());

                float color = user.isOnline()
                        ? BitmapDescriptorFactory.HUE_CYAN
                        : BitmapDescriptorFactory.HUE_RED;

                googleMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(user.getUsername())
                        .snippet(user.getBio())
                        .icon(BitmapDescriptorFactory.defaultMarker(color)));
            }
        }

        // Add heatmap-like cluster markers
        addHeatmapPoints();
    }

    private void addMockMarkers() {
        // Mock developer positions around Bangalore
        LatLng[] positions = {
                new LatLng(12.9716, 77.5946),
                new LatLng(12.9720, 77.5950),
                new LatLng(12.9700, 77.5940),
                new LatLng(12.9710, 77.5960),
                new LatLng(12.9730, 77.5935),
                new LatLng(12.9695, 77.5955),
        };
        String[] names = {"priya_codes", "arjun_dev", "sneha_ml",
                "rahul_cloud", "ananya_web", "vikram_sys"};

        for (int i = 0; i < positions.length; i++) {
            googleMap.addMarker(new MarkerOptions()
                    .position(positions[i])
                    .title(names[i])
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
        }
    }

    /**
     * Add semi-transparent markers to simulate heatmap density.
     */
    private void addHeatmapPoints() {
        // Simulated dense areas
        double baseLat = 12.9716;
        double baseLng = 77.5946;

        for (int i = 0; i < 20; i++) {
            double lat = baseLat + (Math.random() - 0.5) * 0.01;
            double lng = baseLng + (Math.random() - 0.5) * 0.01;

            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(lat, lng))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .alpha(0.3f)
                    .title("Developer"));
        }
    }
}
