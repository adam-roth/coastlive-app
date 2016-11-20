package au.com.suncoastpc.coastlive.activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.suncoastpc.coastlive.R;
import au.com.suncoastpc.coastlive.db.Genre;
import au.com.suncoastpc.coastlive.db.MusicEvent;
import au.com.suncoastpc.coastlive.db.Venue;
import au.com.suncoastpc.coastlive.fragment.AsyncMapConsumer;
import au.com.suncoastpc.coastlive.fragment.BasicGmapV2Fragment;
import au.com.suncoastpc.coastlive.net.ApiMethod;
import au.com.suncoastpc.coastlive.net.ApiResponseDelegate;
import au.com.suncoastpc.coastlive.net.ServerApi;
import au.com.suncoastpc.coastlive.utils.Constants;
import au.com.suncoastpc.coastlive.utils.DatabaseHelper;
import au.com.suncoastpc.coastlive.utils.Environment;
import au.com.suncoastpc.coastlive.utils.StringUtilities;

public class EventMapActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, AsyncMapConsumer, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener, ApiResponseDelegate {
    private static final DateFormat DAY_FORMAT = new SimpleDateFormat("EEEE, dd MMM yyyy");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("h.mma");

    private static final String[] EXTRA_CATEGORIES = {"BBQ's", "Bus Stops", "Community Centres", "Dining", "Groceries", "Outdoor Activities", "Wifi Access Points"};
    private static final boolean[] CATEGORY_FLAGS = {false, false, false, false, false, false, false};
    private static final float[] CATEGORY_COLORS = {BitmapDescriptorFactory.HUE_RED, BitmapDescriptorFactory.HUE_BLUE, BitmapDescriptorFactory.HUE_YELLOW, BitmapDescriptorFactory.HUE_CYAN,
                                                    BitmapDescriptorFactory.HUE_ORANGE, BitmapDescriptorFactory.HUE_GREEN, BitmapDescriptorFactory.HUE_AZURE};
    private static final Map<String, String[]> CATEGORY_CONTEXTS = new HashMap<>();

    static {
        CATEGORY_CONTEXTS.put(EXTRA_CATEGORIES[0], new String[]{"Structure/Structure_SCRC/MapServer/0"});
        CATEGORY_CONTEXTS.put(EXTRA_CATEGORIES[1], new String[]{"Transportation/Transportation_SCRC/MapServer/0", "Transportation/Transportation_SCRC/MapServer/1"});
        CATEGORY_CONTEXTS.put(EXTRA_CATEGORIES[2], new String[]{"Society/Society_SCRC/MapServer/12"});
        CATEGORY_CONTEXTS.put(EXTRA_CATEGORIES[3], new String[]{"Society/Society_SCRC/MapServer/23","Society/Society_SCRC/MapServer/25","Society/Society_SCRC/MapServer/26","Society/Society_SCRC/MapServer/27"});
        CATEGORY_CONTEXTS.put(EXTRA_CATEGORIES[4], new String[]{"Society/Society_SCRC/MapServer/32","Society/Society_SCRC/MapServer/33","Society/Society_SCRC/MapServer/34","Society/Society_SCRC/MapServer/35"});
        CATEGORY_CONTEXTS.put(EXTRA_CATEGORIES[5], new String[]{"Society/Society_SCRC/MapServer/1","Society/Society_SCRC/MapServer/5","Society/Society_SCRC/MapServer/7","Society/Society_SCRC/MapServer/42","Society/Society_SCRC/MapServer/51",});
        CATEGORY_CONTEXTS.put(EXTRA_CATEGORIES[6], new String[]{"UtilitiesCommunication/Utilities_SCRC/MapServer/0"});
    }

    private TextView dateDisplay;
    private SeekBar dateSeeker;

    private BasicGmapV2Fragment mapFragment;
    private GoogleMap map;

    private Button datasetsButton;
    private boolean requestProcessing = false;
    private List<Marker> extraMarkers;

    private List<MusicEvent> filteredEvents;
    private List<MusicEvent> allVisibleEvents;
    private long displayedEventLowerBound;
    private long displayedEventUpperbound;

    private List<Marker> currentMarkerSet;
    private Map<Marker, Venue> markerVenues;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_map);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //first, make sure we actually have some content to display
        filteredEvents = new ArrayList<>();
        allVisibleEvents = new ArrayList<>();
        currentMarkerSet = new ArrayList<>();
        extraMarkers = new ArrayList<>();
        markerVenues = new HashMap<>();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 1);
        cal.add(Calendar.MILLISECOND, -2);

        long earliestEventDate = cal.getTimeInMillis();
        displayedEventLowerBound = earliestEventDate;

        cal.add(Calendar.DAY_OF_YEAR, 1);
        displayedEventUpperbound = cal.getTimeInMillis();

        cal.add(Calendar.DAY_OF_YEAR, 30);
        long latestEventDate = cal.getTimeInMillis();

        List<Genre> enabledGenres = DatabaseHelper.getHelper().findEnabledGenres();
        for (MusicEvent event : DatabaseHelper.getHelper().findAll(MusicEvent.class)) {
            if (event.getStartDate() > earliestEventDate && event.getStartDate() <= latestEventDate) {
                boolean genreOkay = enabledGenres.isEmpty();
                if (! genreOkay) {
                    for (Genre genre : event.getGenres()) {
                        if (enabledGenres.contains(genre)) {
                            genreOkay = true;
                            break;
                        }
                    }
                }
                if (genreOkay) {
                    allVisibleEvents.add(event);
                }
            }
        }

        if (allVisibleEvents.isEmpty()) {
            //oops, no events loaded or selected genres are too restrictive; we have nothing to display
            Toast.makeText(this, "Oops, we couldn't find any events to display; try enabling more genres!", Toast.LENGTH_SHORT);
            finish();
            return;
        }

        //can display content, so start setting up
        dateDisplay = (TextView)findViewById(R.id.map_date_text);
        dateSeeker = (SeekBar)findViewById(R.id.map_date_slider);
        dateSeeker.setMax(31);
        dateSeeker.setOnSeekBarChangeListener(this);

        //additional SCC datasets; all default to 'off' initially
        datasetsButton = (Button)findViewById(R.id.map_extra_datasets_button);
        datasetsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (requestProcessing) {
                    Toast.makeText(EventMapActivity.this, "The previous request is still processing; please wait for it to complete and then try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                //display multi-select popup with available supplemental datasets
                new AlertDialog.Builder(EventMapActivity.this)
                        .setTitle("Select Datasets")
                        .setMultiChoiceItems(EXTRA_CATEGORIES, CATEGORY_FLAGS, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int item, boolean checked) {
                                CATEGORY_FLAGS[item] = checked;
                            }
                        })
                        .setPositiveButton("Load Data", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                                if (map == null) {
                                    return;
                                }

                                List<String> contexts = new ArrayList<>();
                                for (int index = 0; index < CATEGORY_FLAGS.length; index++) {
                                    String category = EXTRA_CATEGORIES[index];
                                    if (CATEGORY_FLAGS[index]) {
                                        contexts.addAll(Arrays.asList(CATEGORY_CONTEXTS.get(category)));
                                    }
                                }

                                for (Marker marker : extraMarkers) {
                                    marker.remove();
                                }
                                extraMarkers.clear();

                                if (! contexts.isEmpty()) {
                                    //minLon,minLat,maxLon,maxLat
                                    //return Math.min(lng1, lng2) + "," + Math.min(lat1, lat2) + "," + Math.max(lng1, lng2) + "," + Math.max(lat1, lat2);
                                    LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
                                    double lng1 = bounds.northeast.longitude;
                                    double lng2 = bounds.southwest.longitude;
                                    double lat1 = bounds.northeast.latitude;
                                    double lat2 = bounds.southwest.latitude;
                                    String boundsCsv = Math.min(lng1, lng2) + "," + Math.min(lat1, lat2) + "," + Math.max(lng1, lng2) + "," + Math.max(lat1, lat2);

                                    String params = "inSR=4326&geometryType=esriGeometryEnvelope&f=pjson";
                                    params += "&where=" + StringUtilities.encodeUriComponent("1=1");
                                    //params += "&orderByFields=" + StringUtilities.encodeUriComponent("D_Date_Rec DESC");      //XXX:  breaks the query
                                    params += "&geometry=" + StringUtilities.encodeUriComponent(boundsCsv);

                                    requestProcessing = true;

                                    ServerApi.loadAggregateData(contexts, params, EventMapActivity.this);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        //map
        Fragment currentFragment = getFragmentManager().findFragmentById(R.id.event_map_fragment);
        if (currentFragment != null) {
            mapFragment = (BasicGmapV2Fragment)currentFragment;
            mapFragment.registerMapConsumer(this);
        }
    }

    private List<MusicEvent> getFilteredEvents() {
        if (! filteredEvents.isEmpty()) {
            return  filteredEvents;
        }

        for (MusicEvent event : allVisibleEvents) {
            if (event.getStartDate() > displayedEventLowerBound && event.getStartDate() <= displayedEventUpperbound) {
                filteredEvents.add(event);
            }
        }

        return filteredEvents;
    }

    private void refreshMapData() {
        if (map != null) {
            filteredEvents.clear();
            //map.clear();
            for (Marker marker : currentMarkerSet) {
                marker.remove();
            }
            currentMarkerSet.clear();
            markerVenues.clear();

            populateMap();
        }
    }

    private void populateMap() {
        if (! Environment.isMainThread()) {
            Environment.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    populateMap();
                }
            });

            return;
        }

        if (map == null) {
            //XXX:  should never happen
            return;
        }

        //draw event data onto the map as markers
        List<MusicEvent> eventsToDraw = getFilteredEvents();
        Map<Venue, Integer> eventCountByVenue = new HashMap<>();
        for (MusicEvent event : eventsToDraw) {
            //first, confirm the number of events at each venue
            Venue venue = event.getVenue();
            if (! eventCountByVenue.containsKey(venue)) {
                eventCountByVenue.put(venue, 0);
            }
            eventCountByVenue.put(venue, eventCountByVenue.get(venue) + 1);
        }

        for (MusicEvent event : eventsToDraw) {
            Venue venue = event.getVenue();
            if (eventCountByVenue.containsKey(venue)) {
                Marker marker = null;
                int venueCount = eventCountByVenue.get(venue);
                if (venueCount == 1) {
                    marker = map.addMarker(new MarkerOptions().position(new LatLng(event.getLatitude(), event.getLongitude()))
                            .title(event.getArtist().getName() + " @ " + event.getVenue().getName())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                }
                else {
                    marker = map.addMarker(new MarkerOptions().position(new LatLng(event.getLatitude(), event.getLongitude()))
                            .title(event.getVenue().getName() + " - " + eventCountByVenue.remove(venue) + " Events for " + dateDisplay.getText())
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
                }

                currentMarkerSet.add(marker);
                markerVenues.put(marker, venue);

            }
        }
    }

    private void zoomToFit(boolean animated) {
        if (map == null || currentMarkerSet.isEmpty()) {
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker marker : currentMarkerSet) {
            builder.include(marker.getPosition());
        }

        LatLngBounds bounds = builder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 40);

        if (animated) {
            map.animateCamera(cu);
        }
        else {
            map.moveCamera(cu);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            int daysToAdd = progress;

            Calendar lBound = Calendar.getInstance();
            lBound.set(Calendar.HOUR_OF_DAY, 0);
            lBound.set(Calendar.MINUTE, 0);
            lBound.set(Calendar.SECOND, 0);
            lBound.set(Calendar.MILLISECOND, 1);
            lBound.add(Calendar.MILLISECOND, -2);
            //23:59:59.999 YESTERDAY

            Calendar uBound = Calendar.getInstance();
            uBound.set(Calendar.HOUR_OF_DAY, 0);
            uBound.set(Calendar.MINUTE, 0);
            uBound.set(Calendar.SECOND, 0);
            uBound.set(Calendar.MILLISECOND, 1);
            uBound.add(Calendar.MILLISECOND, -2);
            uBound.add(Calendar.DAY_OF_YEAR, 1);
            //23:59:59.999 TODAY

            lBound.add(Calendar.DAY_OF_YEAR, daysToAdd);
            uBound.add(Calendar.DAY_OF_YEAR, daysToAdd);

            displayedEventLowerBound = lBound.getTimeInMillis();
            displayedEventUpperbound = uBound.getTimeInMillis();

            dateDisplay.setText(displayStringForDate(new Date(displayedEventUpperbound)));

            refreshMapData();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //no-op
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //no-op
        if (getFilteredEvents().isEmpty()) {
            Toast.makeText(this, "There aren't any events to display on the currently selected date; please move the slider to select a different date.", Toast.LENGTH_SHORT).show();
        }
        else {
            int num = getFilteredEvents().size();
            if (num == 1) {
                Toast.makeText(this, "1 event found for " + dateDisplay.getText() + ".", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(this, num + " events found for " + dateDisplay.getText() + ".", Toast.LENGTH_SHORT).show();
            }

            zoomToFit(true);
        }
    }

    private String displayStringForDate(Date date) {
        String today = DAY_FORMAT.format(new Date());
        String tomorrow = DAY_FORMAT.format(System.currentTimeMillis() + (1000L * 60 * 60 * 24));
        String eventDay = DAY_FORMAT.format(new Date(date.getTime()));

        if (today.equals(eventDay)) {
            return "Today";
        }
        if (tomorrow.equals(eventDay)) {
            return "Tomorrow";
        }

        return eventDay;
    }

    private double measure(double lat1, double lon1, double lat2, double lon2){  // generally used geo measurement function
        double  R = 6378.137; // Radius of earth in KM
        double dLat = (lat2 - lat1) * Math.PI / 180;
        double dLon = (lon2 - lon1) * Math.PI / 180;
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        return d * 1000; // meters
    };

    private String boundingBoxCsv(double lat, double lng, double targetKm) {
        double kmPer1Lat = measure(lat, lng, lat + 1, lng) / 1000.0;
        double kmPer1Lng = measure(lat, lng, lat, lng + 1) / 1000.0;

        double dLatPerKm = 1 / kmPer1Lat;
        double dLngPerKm = 1 / kmPer1Lng;

        double dLat = dLatPerKm * (targetKm / 2);
        double dLng = dLngPerKm * (targetKm / 2);

        double lat1 = lat + dLat;
        double lat2 = lat - dLat;
        double lng1 = lng + dLng;
        double lng2 = lng - dLng;

        //minLon,minLat,maxLon,maxLat
        return Math.min(lng1, lng2) + "," + Math.min(lat1, lat2) + "," + Math.max(lng1, lng2) + "," + Math.max(lat1, lat2);
    };


    @Override
    public void consumeMap(GoogleMap googleMap) {
        map = googleMap;

        try {
            map.setMyLocationEnabled(true);
        }
        catch (SecurityException ignored) {}
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(true);
        googleMap.getUiSettings().setScrollGesturesEnabled(true);

        //map.setOnMarkerClickListener(this);
        map.setOnInfoWindowClickListener(this);

        refreshMapData();

        //since this is the first time we're loading anything, trigger a zoom
        Environment.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                zoomToFit(false);
            }
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        //XXX:  not used
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        //show popup list with events at the selected marker, allow the user to select one and view the event details (or go straight to details activity if only a single matching event)
        Venue venue = markerVenues.get(marker);
        if (venue == null) {
            return;
        }

        final List<MusicEvent> matchedEvents = new ArrayList<>();
        for (MusicEvent event : getFilteredEvents()) {
            if (event.getVenue().equals(venue)) {
                matchedEvents.add(event);
            }
        }

        if (matchedEvents.size() == 1) {
            //can go directly to the event details page
            MusicEvent event = matchedEvents.get(0);

            Bundle params = new Bundle();
            params.putString("eventId", event.getId());

            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtras(params);
            startActivity(intent);
        }
        else {
            //need to show the events in a list-view and let the user choose
            String[] opts = new String[matchedEvents.size()];
            for (int index = 0; index < matchedEvents.size(); index++) {
                MusicEvent event = matchedEvents.get(index);
                opts[index] = event.getArtist().getName();
                if (event.getStartTime() != event.getStartDate()) {
                    opts[index] += " @ " + TIME_FORMAT.format(new Date(event.getStartTime())).toLowerCase();
                }

            }

            new AlertDialog.Builder(this)
                    .setTitle(marker.getTitle())
                    .setSingleChoiceItems(opts, -1, null)
                    .setPositiveButton("View Details", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                            if (selectedPosition != -1) {
                                MusicEvent event = matchedEvents.get(selectedPosition);

                                Bundle params = new Bundle();
                                params.putString("eventId", event.getId());

                                Intent intent = new Intent(EventMapActivity.this, EventDetailsActivity.class);
                                intent.putExtras(params);
                                startActivity(intent);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    @Override
    public void handleResponse(long requestId, ApiMethod requestApi, JSONObject response) {
        processApiResponse(requestId, requestApi, response);
    }

    private void processApiResponse(final long requestId, final ApiMethod requestApi, final JSONObject response) {
        if (! Environment.isMainThread()) {
            Environment.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    processApiResponse(requestId, requestApi, response);
                }
            });

            return;
        }

        requestProcessing = false;
        if (map == null || response == null || ! response.containsKey("status")) {
            return;
        }

        if (Constants.SUCCESS_STATUS.equals(response.getAsString("status"))) {
            JSONArray items = (JSONArray)response.get("result");
            for (Object itemObj : items) {
                JSONObject item = (JSONObject)itemObj;
                LatLng itemPos = coordsFromJson(item);
                String itemTitle = titleFromJson(item);

                if (itemPos != null && ! StringUtilities.isEmpty(itemTitle)) {
                    Marker marker = map.addMarker(new MarkerOptions().position(itemPos)
                            .title(itemTitle)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    extraMarkers.add(marker);
                }
            }
        }
        else {
            Toast.makeText(this, "Failed to load supplemental data from the server; please check your Internet connection and try again.", Toast.LENGTH_SHORT).show();
        }
    }

    //public Bitmap resizeMapIcons(String iconName,int width, int height){
    //    Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName, "drawable", getPackageName()));
    //    Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
    //    return resizedBitmap;
    //}

    private LatLng coordsFromJson(JSONObject details) {
        LatLng result = null;

        try {
            if (details.containsKey("latitude") && details.containsKey("longitude")) {
                //it's a point
                result = new LatLng(details.getAsNumber("latitude").doubleValue(), details.getAsNumber("longitude").doubleValue());
            } else if (details.containsKey("geometry") && ((JSONObject) details.get("geometry")).containsKey("points") && ((JSONArray) ((JSONObject) details.get("geometry")).get("points")).size() == 1) {
                //it's (still) a point
                JSONArray pointContainer = (JSONArray) ((JSONArray) ((JSONObject) details.get("geometry")).get("points")).get(0);
                result = new LatLng(((Number) pointContainer.get(1)).doubleValue(), ((Number) pointContainer.get(0)).doubleValue());
            } else if (details.containsKey("geometry") && ((JSONObject) details.get("geometry")).containsKey("rings") && ((JSONArray) ((JSONObject) details.get("geometry")).get("rings")).size() == 1) {
                //it's a region; we can plot the entire thing or take the centroid
                JSONArray coords = (JSONArray) ((JSONArray) ((JSONObject) details.get("geometry")).get("rings")).get(0);
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (int index = 0; index < coords.size(); index++) {
                    JSONArray coord = (JSONArray) coords.get(index);
                    builder.include(new LatLng(((Number) coord.get(1)).doubleValue(), ((Number) coord.get(0)).doubleValue()));
                    ;
                }

                LatLngBounds bounds = builder.build();
                result = bounds.getCenter();
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
        }

        return result;
    }


    private String titleFromJson(JSONObject details) {
        String result = null;
        JSONObject item = details;

        try {
            boolean isDodgy = details.containsKey("geometry") && ((JSONObject) details.get("geometry")).containsKey("rings") && ((JSONArray) ((JSONObject) details.get("geometry")).get("rings")).size() == 1;

            if (item.containsKey("Owner") && item.containsKey("WiFiType")) {
                result = item.getAsString("WiFiType") + " - " + item.getAsString("Owner");
            }
            else if (item.containsKey("Address") && item.containsKey("Locality")) {
                result = item.getAsString("Address") + ", " + item.getAsString("Locality");
            }
            else if (item.containsKey("Facility")) {
                result = item.getAsString("Facility");
            }
            else if (item.containsKey("DESCRIPTION")) {
                result = item.getAsString("DESCRIPTION");
            }
            else if (item.containsKey("ReferenceID")) {
                result = item.getAsString("ReferenceID");
            }
            else if (item.containsKey("NAME")) {
                result = item.getAsString("NAME");
            }
            else if (item.containsKey("Name")) {
                result = item.getAsString("Name");
            }

            else if (item.containsKey("LocationDesc")) {
                if (item.containsKey("ConditionComments")) {
                    result = item.getAsString("LocationDesc") + " - " + item.getAsString("ConditionComments");
                }
                else {
                    result = item.getAsString("LocationDesc");
                }
            }
            else if (item.containsKey("ConditionComments")) {
                result = item.getAsString("ConditionComments");
            }

            else if (item.containsKey("Beach")) {
                if (item.containsKey("LocationDescription") && item.containsKey("DogInfo")) {
                    result = item.getAsString("Beach") + " - " + item.getAsString("LocationDescription") + " - " + item.getAsString("DogInfo");
                }
                else if (item.containsKey("LocationDescription")) {
                    result = item.getAsString("Beach") + " - " + item.getAsString("LocationDescription");
                }
                else if (item.containsKey("DogInfo")) {
                    result = item.getAsString("Beach") + " - " + item.getAsString("DogInfo");
                }
                else {
                    result = item.getAsString("Beach");
                }
            }

            else if (item.containsKey("StreetName")) {
                result = item.getAsString("StreetName");
            }

            else if (item.containsKey("StationName")) {
                //XXX:  note that 'PageURL' is also pretty interesting
                result = item.getAsString("StationName");
            }

            else if (item.containsKey("Hazard")) {
                if (item.containsKey("Location")) {
                    result = item.getAsString("Hazard") + " - " + item.getAsString("Location");
                }
                else {
                    result = item.getAsString("Hazard");
                }
            }

            else if (item.containsKey("Description")) {
                if (item.containsKey("Description2") && item.containsKey("ProjectDetailComments")) {
                    result = item.getAsString("Description") + " - " + item.getAsString("Description2") + " - " + item.getAsString("ProjectDetailComments");
                }
                else if (item.containsKey("Description2")) {
                    result = item.getAsString("Description") + " - " + item.getAsString("Description2");
                }
                else if (item.containsKey("ProjectDetailComments")) {
                    result = item.getAsString("Description") + " - " + item.getAsString("ProjectDetailComments");
                }
                else {
                    result = item.getAsString("Description");
                }
            }
            else if (item.containsKey("artistName")) {
                result = item.getAsString("artistName") + " - " + item.getAsString("locationName");
            }

            result = isDodgy && result != null ? result + " - Note that position is approximate" : result;
        }
        catch (Throwable e) {
            e.printStackTrace();
        }

        return result;

    }
}
