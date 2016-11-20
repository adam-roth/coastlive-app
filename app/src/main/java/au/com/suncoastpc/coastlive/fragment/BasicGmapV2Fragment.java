package au.com.suncoastpc.coastlive.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashSet;
import java.util.Set;

import au.com.suncoastpc.coastlive.R;
import au.com.suncoastpc.coastlive.db.MusicEvent;

public class BasicGmapV2Fragment extends Fragment {
    private MapView mapView;
    private GoogleMap map;

    private MusicEvent event;

    private final Set<AsyncMapConsumer> mapConsumers = new HashSet<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_gmap_basic, container, false);

        Bundle args = this.getArguments();

        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) v.findViewById(R.id.event_map);
        mapView.onCreate(savedInstanceState);

        // Gets to GoogleMap from the MapView and does initialization stuff
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                //map.getUiSettings().setMyLocationButtonEnabled(false);
                map.getUiSettings().setMapToolbarEnabled(true);
                googleMap.getUiSettings().setScrollGesturesEnabled(false);
                try {
                    map.setMyLocationEnabled(true);
                }
                catch (SecurityException ignored) {}

                //needs to call MapsInitializer before doing any CameraUpdateFactory calls
                MapsInitializer.initialize(BasicGmapV2Fragment.this.getActivity());

                //updates the location and zoom of the MapView
                if (event != null) {
                    map.clear();

                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(event.getLatitude(), event.getLongitude()), 14);
                    map.moveCamera(cameraUpdate);
                    //map.animateCamera(cameraUpdate);

                    map.addMarker(new MarkerOptions().position(new LatLng(event.getLatitude(), event.getLongitude()))
                            .title(event.getArtist().getName() + " @ " + event.getVenue().getName())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))).showInfoWindow();
                }

                for (AsyncMapConsumer consumer : mapConsumers) {
                    consumer.consumeMap(map);
                }
                mapConsumers.clear();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    public MusicEvent getEvent() {
        return event;
    }
    public void setEvent(MusicEvent event) {
        this.event = event;
        if (event != null && map != null) {
            map.clear();

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(event.getLatitude(), event.getLongitude()), 14);
            map.moveCamera(cameraUpdate);
            //map.animateCamera(cameraUpdate);

            map.addMarker(new MarkerOptions().position(new LatLng(event.getLatitude(), event.getLongitude()))
                    .title(event.getArtist().getName() + " @ " + event.getVenue().getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))).showInfoWindow();
        }
    }

    public MapView getMapView() {
        return mapView;
    }

    public GoogleMap getMap() {
        return map;
    }

    public void registerMapConsumer(AsyncMapConsumer consumer) {
        if (map != null) {
            consumer.consumeMap(map);
        }
        else {
            mapConsumers.add(consumer);
        }
    }
}
