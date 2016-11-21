package au.com.suncoastpc.coastlive.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.dao.RuntimeExceptionDao;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.com.suncoastpc.coastlive.R;
import au.com.suncoastpc.coastlive.db.Artist;
import au.com.suncoastpc.coastlive.db.Genre;
import au.com.suncoastpc.coastlive.db.MusicEvent;
import au.com.suncoastpc.coastlive.db.Venue;
import au.com.suncoastpc.coastlive.net.ApiContext;
import au.com.suncoastpc.coastlive.net.ApiMethod;
import au.com.suncoastpc.coastlive.net.ApiResponseDelegate;
import au.com.suncoastpc.coastlive.net.ServerApi;
import au.com.suncoastpc.coastlive.types.EventFilter;
import au.com.suncoastpc.coastlive.utils.Constants;
import au.com.suncoastpc.coastlive.utils.DatabaseHelper;
import au.com.suncoastpc.coastlive.utils.Environment;
import au.com.suncoastpc.coastlive.utils.StringUtilities;

public class NavigationActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ApiResponseDelegate, ListAdapter, AdapterView.OnItemClickListener {
    private static final DateFormat DAY_FORMAT = new SimpleDateFormat("EEEE, dd MMM yyyy");
    private static final DateFormat DAY_FORMAT_SHORT = new SimpleDateFormat("EEEE, dd MMM");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("h.mma");

    private final List<Object> listContentItems = new ArrayList<>();
    protected final Set<DataSetObserver> observers = new LinkedHashSet<>();

    private ListView displayList;
    private EventFilter currentFilter;
    private Genre globalGenre = null;

    private ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Environment.setApplicationContext(getApplicationContext());     //bootstrap Environment

        setContentView(R.layout.activity_navigation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //FIXME:  bootstrap sync, etc.; display modal 'Loading' spinner if no events are present in the database
        currentFilter = EventFilter.BY_ARTIST;
        getSupportActionBar().setTitle("Coast Live! - Artists");
        displayList = (ListView)findViewById(R.id.list_display);
        displayList.setOnItemClickListener(this);
        List<MusicEvent> allEvents = DatabaseHelper.getHelper().findUpcomingEvents();
        if (allEvents.isEmpty() || Environment.shouldForceResync()) {
            //display modal loading dialog, perform initial sync
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Corralling Artists");
            progressDialog.setMessage("Please wait...");
            progressDialog.setCancelable(false); // disable dismiss by tapping outside of the dialog
            progressDialog.show();

            ServerApi.loadData(ApiContext.LIVE_MUSIC, ApiContext.STANDARD_MUSIC_SEARCH_PARAMS, this);
        }
        else {
            //can go straight to displaying events
            displayList.setAdapter(this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (EnumSet.of(EventFilter.BY_ARTIST, EventFilter.BY_GENRE, EventFilter.BY_VENUE, EventFilter.BY_DATE, EventFilter.FAV_ARTISTS, EventFilter.FAV_VENUES).contains(currentFilter)) {
            //see if the user has tapped on an event, and if they have, move to the event-details activity
            Object tappedItem = getItem(position);
            if (tappedItem != null && tappedItem instanceof MusicEvent) {
                MusicEvent event = (MusicEvent)tappedItem;

                Bundle params = new Bundle();
                params.putString("eventId", event.getId());

                Intent intent = new Intent(this, EventDetailsActivity.class);
                intent.putExtras(params);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            new AlertDialog.Builder(this)
                    .setTitle("Coast Live! v0.1")
                    .setMessage("Developed for HackFest 2016 by Team DataFlytt.")
                    .setPositiveButton("More Info", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();

                            String url = "http://terra.suncoastpc.com.au:8181/data-gateway/examples/coastLive";
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            NavigationActivity.this.startActivity(browserIntent);
                        }
                    })
                    .setNegativeButton("Dismiss", null)
                    .show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        String newTitle = "Coast Live!";
        if (id == R.id.nav_artist) {
            //show events grouped by artist
            currentFilter = EventFilter.BY_ARTIST;
            refreshListData();
            newTitle += " - Artists";
        }
        else if (id == R.id.nav_calendar) {
            //show events grouped by date (calendar view?)
            //FIXME:  fix timezone settings in data-gateway [for now we've just added a hack in the app to apply the correct TZ offset]

            currentFilter = EventFilter.BY_DATE;
            refreshListData();
            newTitle += " - Calendar";
        }
        else if (id == R.id.nav_genre) {
            //show events grouped by genre (filter displayed genres by settings/prefs)
            currentFilter = EventFilter.BY_GENRE;
            refreshListData();
            newTitle += " - Genres";
        }
        else if (id == R.id.nav_venue) {
            //show events grouped by venue
            currentFilter = EventFilter.BY_VENUE;
            refreshListData();
            newTitle += " - Venues";
        }
        else if (id == R.id.nav_map) {
            //show events on a map, with slider to select date/distance into the future
            Intent intent = new Intent(this, EventMapActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.nav_settings_bands) {
            //view only favorite bands
            currentFilter = EventFilter.FAV_ARTISTS;
            refreshListData();
            newTitle += " - Fave Artists";
        }
        else if (id == R.id.nav_settings_genre) {
            //manage displayed genres (default option is 'all genres')
            currentFilter = EventFilter.SETTINGS_GENRE;
            refreshListData();
            newTitle += " - Genre Settings";
        }
        else if (id == R.id.nav_settings_venues) {
            //view only favorite venues
            currentFilter = EventFilter.FAV_VENUES;
            refreshListData();
            newTitle += " - Fave Venues";
        }

        getSupportActionBar().setTitle(newTitle);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void handleResponse(long requestId, ApiMethod requestApi, JSONObject response) {
        if (progressDialog != null) {
            Environment.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            });
        }

        if (requestApi == ApiMethod.LOAD_DATA) {
            if (Constants.SUCCESS_STATUS.equals(response.getAsString("status"))) {
                //have new data to parse
                RuntimeExceptionDao<MusicEvent, String> eventDao = DatabaseHelper.getHelper().getRuntimeDao(MusicEvent.class);
                JSONArray allEvents = (JSONArray)response.get("result");
                for (Object eventObj : allEvents) {
                    JSONObject eventData = (JSONObject)eventObj;
                    if (StringUtilities.isEmpty(eventData.getAsString("artistName")) || StringUtilities.isEmpty(eventData.getAsString("__collationIdentifier"))) {
                        Log.w("db.sync", "Received an event that is not even superficially valid:  json=" + eventData.toJSONString());
                        continue;
                    }

                    //identify each artist associated with this event; we will create a distinct event for each one
                    List<String> eventArtists = new ArrayList<>();
                    eventArtists.add(eventData.getAsString("artistName").replaceAll("[^a-zA-Z0-9 '\"!@#$%\\^\\&\\*\\(\\)\\-_\\+\\=\\[\\]\\{\\}\\?/]", ""));
                    if (eventData.containsKey("otherArtists")) {
                        JSONArray otherArtists = (JSONArray)eventData.get("otherArtists");
                        for (Object artistObj : otherArtists) {
                            String artist = artistObj.toString().replaceAll("[^a-zA-Z0-9 '\"!@#$%\\^\\&\\*\\(\\)\\-_\\+\\=\\[\\]\\{\\}\\?/]", "");
                            if (! eventArtists.contains(artist)) {
                                eventArtists.add(artist);
                            }
                        }
                    }

                    String primaryArtist = eventData.getAsString("artistName");
                    String originalIdentifier = eventData.getAsString("__collationIdentifier");
                    for (String artist : eventArtists) {
                        String identifier = originalIdentifier.replace(saneString(primaryArtist), saneString(artist));
                        eventData.put("artistName", artist);
                        eventData.put("__collationIdentifier", identifier);

                        MusicEvent event = eventDao.queryForId(identifier);
                        if (event == null) {
                            Log.d("db.sync", "Saving a new event for id=" + identifier);
                            event = new MusicEvent();
                            event.loadFromJson(eventData);
                            if (! StringUtilities.isEmpty(event.getOriginalJson())) {
                                eventDao.create(event);
                            }
                        }
                        else {
                            Log.d("db.sync", "Already have an event for id=" + identifier);
                            //refresh the event we found with the JSON data, just in case anything has changed
                            event.loadFromJson(eventData);
                            eventDao.update(event);
                        }
                    }
                }

                //draw the content now
                Environment.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        listContentItems.clear();
                        displayList.setAdapter(NavigationActivity.this);
                        refreshListData();
                    }
                });
            }
            else {
                //API request failed; message the user if this means we can't display anything useful
                if (DatabaseHelper.getHelper().findUpcomingEvents().isEmpty()) {
                    Environment.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(NavigationActivity.this, "Unable to load event data from the server; please check your Internet connectivity and try again!", Toast.LENGTH_LONG).show();
                        }
                    });

                }
            }
        }
    }

    private String saneString(String str) {
        return str.toLowerCase().replaceAll("[^a-z]", "");
    }

    private Genre getGlobalGenre() {
        if (globalGenre != null) {
            return globalGenre;
        }

        globalGenre = new Genre();
        globalGenre.setName("Display Events for All Genres");
        globalGenre.setEnabled(DatabaseHelper.getHelper().findEnabledGenres().isEmpty());

        return globalGenre;
    }

    private List<Object> getListContent() {
        if (listContentItems.isEmpty() && currentFilter == EventFilter.SETTINGS_GENRE) {
            //XXX:  settings view; we can reuse the list view for this
            List<Genre> allGenres = DatabaseHelper.getHelper().findAll(Genre.class);
            if (allGenres.isEmpty()) {
                //no data loaded yet; just go back to the 'Artists' tab and wait
                listContentItems.clear();
                currentFilter = EventFilter.BY_ARTIST;

                Environment.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        getSupportActionBar().setTitle("Coast Live! - Artists");
                        Toast.makeText(NavigationActivity.this, "Well this is embarrassing, there doesn't seem to be any data available yet!  Please wait a bit and try again later.", Toast.LENGTH_LONG).show();
                    }
                });
            }
            else {
                //can display genre settings
                listContentItems.add("Show All");
                listContentItems.add(getGlobalGenre());
                listContentItems.add("Filter Specific Genres");
                listContentItems.addAll(allGenres);
            }
        }

        if (listContentItems.isEmpty() && EnumSet.of(EventFilter.BY_ARTIST, EventFilter.BY_GENRE, EventFilter.BY_VENUE, EventFilter.BY_DATE, EventFilter.FAV_ARTISTS, EventFilter.FAV_VENUES).contains(currentFilter)) {     //XXX:  ideally 'BY_DATE' should render on a calendar view
            List<String> sectionTitles = new ArrayList<>();
            Map<String, List<MusicEvent>> sectionContents = new HashMap<>();
            List<MusicEvent> allEvents = DatabaseHelper.getHelper().findUpcomingEvents();

            //pull the lists of favorite artists and favorite venues in case we need to filter
            List<Artist> faveArtists = DatabaseHelper.getHelper().findFaveArtists();
            List<Venue> faveVenues = DatabaseHelper.getHelper().findFaveVenues();
            List<Genre> enabledGenres = DatabaseHelper.getHelper().findEnabledGenres();

            if (currentFilter == EventFilter.FAV_ARTISTS && faveArtists.isEmpty()) {
                //invalid search filter
                currentFilter = EventFilter.BY_ARTIST;
                Environment.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        getSupportActionBar().setTitle("Coast Live! - Artists");
                        Toast.makeText(NavigationActivity.this, "You haven't selected any favorite artists yet!", Toast.LENGTH_LONG).show();
                    }
                });
            }

            if (currentFilter == EventFilter.FAV_VENUES && faveVenues.isEmpty()) {
                //invalid search filter
                currentFilter = EventFilter.BY_VENUE;
                Environment.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        getSupportActionBar().setTitle("Coast Live! - Venues");
                        Toast.makeText(NavigationActivity.this, "You haven't selected any favorite venues yet!", Toast.LENGTH_LONG).show();
                    }
                });
            }

            for (MusicEvent event : allEvents) {
                boolean genreOkay = enabledGenres.isEmpty();
                if (! genreOkay) {
                    for (Genre genre : event.getGenres()) {
                        //XXX:  permissive mode; if any genre matches, we admit the result
                        if (enabledGenres.contains(genre)) {
                            genreOkay = true;
                            break;
                        }
                    }
                }
                if (! genreOkay) {
                    //XXX:  we shouldn't display this event, because it doesn't match any enabled genre
                    continue;
                }

                Set<String> eventSections = new HashSet<>();
                switch (currentFilter) {
                    case FAV_ARTISTS:
                        if (! faveArtists.contains(event.getArtist())) {
                            continue;
                        }
                    case BY_ARTIST:
                        eventSections.add(event.getArtist().getName());
                        break;
                    case FAV_VENUES:
                        if (! faveVenues.contains(event.getVenue())) {
                            continue;
                        }
                    case BY_VENUE:
                        eventSections.add(event.getVenue().getName());
                        break;
                    case BY_DATE:
                        eventSections.add(sectionNameForEventDate(event));
                        break;
                    case BY_GENRE:
                        for (Genre genre : event.getGenres()) {
                            if (enabledGenres.isEmpty() || enabledGenres.contains(genre)) {
                                eventSections.add(genre.getName());
                            }
                        }
                        break;
                    default:
                        Log.w("display", "Unrecognized filter setting:  " + currentFilter);
                }

                for (String title : eventSections) {
                    if (! sectionContents.containsKey(title)) {
                        sectionTitles.add(title);
                        sectionContents.put(title, new ArrayList<MusicEvent>());
                    }

                    sectionContents.get(title).add(event);
                }
            }

            if (currentFilter != EventFilter.BY_DATE) {
                //XXX:  don't need to sort by title if we're displaying by date; the events come out of the database ordered by date
                Collections.sort(sectionTitles);
            }
            for (String title : sectionTitles) {
                listContentItems.add(title);
                listContentItems.addAll(sectionContents.get(title));
            }

            if (listContentItems.isEmpty() && EnumSet.of(EventFilter.FAV_ARTISTS, EventFilter.FAV_VENUES).contains(currentFilter)) {
                Environment.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(NavigationActivity.this, "No upcoming events were found for any of your favorites.  Bummer!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        else if (currentFilter == EventFilter.ON_MAP) {
            //XXX:  no list content; need to switch to map view
        }
        else {
            //XXX:  not currently used
        }

        return listContentItems;
    }

    private String sectionNameForEventDate(MusicEvent event) {
        String today = DAY_FORMAT.format(new Date());
        String tomorrow = DAY_FORMAT.format(System.currentTimeMillis() + (1000L * 60 * 60 * 24));
        String eventDay = DAY_FORMAT.format(new Date(event.getStartTime()));

        if (today.equals(eventDay)) {
            return "Today";
        }
        if (tomorrow.equals(eventDay)) {
            return "Tomorrow";
        }

        return eventDay;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int i) {
        return true;
    }

    @Override
    public void registerDataSetObserver(final DataSetObserver observer) {
        synchronized(observers) {
            observers.add(observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        synchronized(observers) {
            observers.remove(observer);
        }
    }

    @Override
    public int getCount() {
        return getListContent().size();
    }

    @Override
    public Object getItem(int index) {
        List<Object> allItems = getListContent();
        if (index < allItems.size()) {
            return allItems.get(index);
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        Object itemObj = getItem(position);
        return itemObj == null ? -1 : itemObj.toString().hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (currentFilter == EventFilter.SETTINGS_GENRE) {
            final Object genreOrText = getItem(position);
            if (genreOrText instanceof String) {
                //section title
                convertView = inflater.inflate(R.layout.list_section_header_interactive, parent, false);
                TextView textView = (TextView) convertView.findViewById(R.id.list_header_text);
                textView.setText(genreOrText.toString());

                //hide the imagebutton
                final ImageButton favButton = (ImageButton) convertView.findViewById(R.id.list_header_favicon);
                favButton.setVisibility(View.GONE);

                convertView.setId((int) textView.getText().hashCode());
            }
            else {
                final Genre genre = (Genre)genreOrText;

                convertView = inflater.inflate(R.layout.list_section_item_genre, parent, false);
                TextView genreName = (TextView)convertView.findViewById(R.id.list_genre_text);
                final CheckBox genreControl = (CheckBox)convertView.findViewById(R.id.list_genre_checkbox);

                genreName.setText(genre.getName());
                genreControl.setChecked(genre.isEnabled());
                genreControl.setEnabled(! genre.equals(getGlobalGenre()) || ! genreControl.isChecked());
                genreControl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (! genreControl.isEnabled()) {
                            return;
                        }

                        RuntimeExceptionDao<Genre, String> dao = DatabaseHelper.getHelper().getRuntimeDao(Genre.class);

                        genre.setEnabled(! genre.isEnabled());
                        genreControl.setChecked(genre.isEnabled());
                        genreControl.setEnabled(! genre.equals(getGlobalGenre()) || ! genreControl.isChecked());

                        if (genre.equals(getGlobalGenre())) {
                            //the global genre has been toggled back to 'on'; need to disable everything else
                            for (Genre dbGenre : DatabaseHelper.getHelper().findAll(Genre.class)) {
                                dbGenre.setEnabled(false);
                                dao.update(dbGenre);
                            }

                            //we probably just changed a lot of things, so reload the list
                            refreshListData();
                        }
                        else {
                            dao.update(genre);      //save the item that was changed

                            boolean prevState = globalGenre.isEnabled();
                            globalGenre.setEnabled(DatabaseHelper.getHelper().findEnabledGenres().isEmpty());
                            if (globalGenre.isEnabled() != prevState) {
                                //we made a change that impacts another UI element in the list, refresh
                                refreshListData();
                            }
                        }
                    }
                });

                convertView.setId((int) genre.getName().hashCode());
            }

            //return from here; do not proceed to event handling
            return convertView;
        }

        final Object eventOrText = getItem(position);
        if (eventOrText instanceof String) {
            //section title/header
            if (! EnumSet.of(EventFilter.BY_ARTIST, EventFilter.BY_VENUE, EventFilter.FAV_ARTISTS, EventFilter.FAV_VENUES).contains(currentFilter)) {
                convertView = inflater.inflate(R.layout.list_section_header, parent, false);
                TextView textView = (TextView) convertView.findViewById(R.id.list_header_text);

                textView.setText(eventOrText.toString());
                convertView.setId((int) textView.getText().hashCode());
            }
            else {
                convertView = inflater.inflate(R.layout.list_section_header_interactive, parent, false);
                TextView textView = (TextView) convertView.findViewById(R.id.list_header_text);
                textView.setText(eventOrText.toString());


                final ImageButton favButton = (ImageButton) convertView.findViewById(R.id.list_header_favicon);       //FIXME:  bind click handler to button and header row to toggle favorite status
                favButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (currentFilter == EventFilter.BY_ARTIST || currentFilter == EventFilter.FAV_ARTISTS) {
                            Artist artist = DatabaseHelper.getHelper().findById(Artist.class, eventOrText.toString());
                            artist.setFavorite(! artist.isFavorite());
                            DatabaseHelper.getHelper().getRuntimeDao(Artist.class).update(artist);

                            if (artist.isFavorite()) {
                                favButton.setImageResource(android.R.drawable.btn_star_big_on);
                            }
                            else {
                                favButton.setImageResource(android.R.drawable.btn_star_big_off);
                            }

                            if (currentFilter == EventFilter.FAV_ARTISTS) {
                                refreshListData();
                            }
                        }
                        else {
                            Venue venue = DatabaseHelper.getHelper().findById(Venue.class, eventOrText.toString());
                            venue.setFavorite(! venue.isFavorite());
                            DatabaseHelper.getHelper().getRuntimeDao(Venue.class).update(venue);

                            if (venue.isFavorite()) {
                                favButton.setImageResource(android.R.drawable.btn_star_big_on);
                            }
                            else {
                                favButton.setImageResource(android.R.drawable.btn_star_big_off);
                            }

                            if (currentFilter == EventFilter.FAV_VENUES) {
                                refreshListData();
                            }
                        }
                    }
                });

                if (currentFilter == EventFilter.BY_ARTIST || currentFilter == EventFilter.FAV_ARTISTS) {
                    Artist artist = DatabaseHelper.getHelper().findById(Artist.class, eventOrText.toString());
                    if (artist.isFavorite()) {
                        favButton.setImageResource(android.R.drawable.btn_star_big_on);
                    }
                }
                else {
                    Venue venue = DatabaseHelper.getHelper().findById(Venue.class, eventOrText.toString());
                    if (venue.isFavorite()) {
                        //FIXME:  set button state
                        favButton.setImageResource(android.R.drawable.btn_star_big_on);
                    }
                }
            }
        }
        else {
            MusicEvent event = (MusicEvent) eventOrText;

            convertView = inflater.inflate(R.layout.list_section_item_event, parent, false);
            TextView topLeft = (TextView)convertView.findViewById(R.id.list_event_date);
            TextView topRight = (TextView)convertView.findViewById(R.id.list_event_venue);
            TextView bottomLeft = (TextView)convertView.findViewById(R.id.list_event_time);
            TextView bottomRight = (TextView)convertView.findViewById(R.id.list_event_address);

            if (currentFilter == EventFilter.BY_ARTIST || currentFilter == EventFilter.FAV_ARTISTS) {
                topLeft.setText(DAY_FORMAT_SHORT.format(new Date(event.getStartDate())));
                bottomLeft.setText(event.getStartTime() == event.getStartDate() ? "" : TIME_FORMAT.format(new Date(event.getStartTime())).toLowerCase());
                topRight.setText(event.getVenue().getName());
                bottomRight.setText(event.getVenue().getAddress());
            }
            else if (currentFilter == EventFilter.BY_DATE) {
                topLeft.setText(event.getArtist().getName());
                bottomLeft.setText(event.getStartTime() == event.getStartDate() ? "" : TIME_FORMAT.format(new Date(event.getStartTime())).toLowerCase());
                topRight.setText(event.getVenue().getName());
                bottomRight.setText(event.getVenue().getAddress());
            }
            else if (currentFilter == EventFilter.BY_GENRE) {
                topLeft.setText(event.getArtist().getName());
                bottomLeft.setText(DAY_FORMAT_SHORT.format(new Date(event.getStartDate())));
                topRight.setText(event.getVenue().getName());
                bottomRight.setText(event.getVenue().getAddress());
                //if (event.getStartTime() != event.getStartDate()) {
                //    topRight.setText(topRight.getText() + " @ " + TIME_FORMAT.format(new Date(event.getStartTime())).toLowerCase());
                //}
            }
            else if (currentFilter == EventFilter.BY_VENUE || currentFilter == EventFilter.FAV_VENUES) {
                topLeft.setText(event.getArtist().getName());
                bottomLeft.setText(event.getStartTime() == event.getStartDate() ? "" : TIME_FORMAT.format(new Date(event.getStartTime())).toLowerCase());
                topRight.setText(DAY_FORMAT_SHORT.format(new Date(event.getStartDate())));
                bottomRight.setText(event.getVenue().getAddress());
            }

            if (event.isFreeConcert()) {
                topLeft.setText(topLeft.getText() + " - Free!");
                topLeft.setTextColor(Color.argb(255, 0, 96, 0));
            }

            if ("R18".equals(event.getAgeType()) || "18+".equals(event.getAgeType())) {
                topRight.setText(topRight.getText() + " (18+)");
                topRight.setTextColor(Color.argb(255, 128, 0, 0));
            }

            convertView.setId((int)event.getId().hashCode());
            //textView.setOnClickListener(JOB_CLICK_HANDLER);
        }

        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        //FIXME:  will likely have to revise
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        //FIXME:  will likely have to revise
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.getCount() == 0;
    }

    public void refreshListData() {
        listContentItems.clear();
        Environment.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                synchronized(observers) {
                    for (DataSetObserver observer : observers) {
                        observer.onChanged();
                    }
                }
            }
        });
    }
}
