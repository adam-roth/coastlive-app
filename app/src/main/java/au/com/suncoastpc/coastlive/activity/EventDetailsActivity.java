package au.com.suncoastpc.coastlive.activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import au.com.suncoastpc.coastlive.R;
import au.com.suncoastpc.coastlive.db.Genre;
import au.com.suncoastpc.coastlive.db.MusicEvent;
import au.com.suncoastpc.coastlive.fragment.BasicGmapV2Fragment;
import au.com.suncoastpc.coastlive.utils.DatabaseHelper;
import au.com.suncoastpc.coastlive.utils.StringUtilities;
import au.com.suncoastpc.coastlive.utils.comparators.GenreNameCompare;

public class EventDetailsActivity extends AppCompatActivity {
    private static final DateFormat DAY_FORMAT = new SimpleDateFormat("dd MMMM yyyy");
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("h.mma");

    private MusicEvent selectedEvent;

    private TextView venueName;
    private TextView venueAddress;
    private TextView date;
    private TextView time;
    private TextView descriptionView;
    private TextView freeEvent;
    private TextView genreDisplay;

    private Button ticketPurchaseButton;
    private Button ticketEnquireButton;

    private BasicGmapV2Fragment mapFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle params = getIntent().getExtras();
        String eventId = params.getString("eventId");
        MusicEvent event = DatabaseHelper.getHelper().findById(MusicEvent.class, eventId);

        if (event == null) {
            //invalid call, go back to previous activity
            finish();
            return;
        }

        selectedEvent = event;
        getSupportActionBar().setTitle(event.getArtist().getName());

        //basic event details
        venueName = (TextView)findViewById(R.id.list_event_venue);
        venueAddress = (TextView)findViewById(R.id.list_event_address);
        date = (TextView)findViewById(R.id.list_event_date);
        time = (TextView)findViewById(R.id.list_event_time);

        date.setText(DAY_FORMAT.format(new Date(event.getStartDate())));
        time.setText(event.getStartTime() == event.getStartDate() ? "" : TIME_FORMAT.format(new Date(event.getStartTime())).toLowerCase());
        venueName.setText(event.getVenue().getName());
        venueAddress.setText(event.getVenue().getAddress());

        if ("R18".equals(event.getAgeType()) || "18+".equals(event.getAgeType())) {
            venueName.setText(venueName.getText() + " (18+)");
            venueName.setTextColor(Color.argb(255, 128, 0, 0));
        }

        //map
        Fragment currentFragment = getFragmentManager().findFragmentById(R.id.event_map_fragment);
        if (currentFragment != null) {
            mapFragment = (BasicGmapV2Fragment)currentFragment;
            mapFragment.setEvent(selectedEvent);
        }

        //genre details
        genreDisplay = (TextView)findViewById(R.id.genre_list);
        List<Genre> genres = new ArrayList<>(event.getGenres());
        Collections.sort(genres, new GenreNameCompare());
        if (! genres.isEmpty()) {
            String genreList = "";
            for (Genre genre : genres) {
                if (! "".equals(genreList)) {
                    genreList += ", ";
                }
                genreList += genre.getName();
                genreDisplay.setText(genreList);
            }
        }
        else {
            //no genre info to show
            findViewById(R.id.event_genres_container).setVisibility(View.GONE);
        }

        //ticket details
        freeEvent = (TextView)findViewById(R.id.tickets_not_required);
        ticketPurchaseButton = (Button)findViewById(R.id.ticket_purchase_button);
        ticketEnquireButton = (Button)findViewById(R.id.ticket_enquire_button);

        if (event.isFreeConcert()) {
            ticketPurchaseButton.setVisibility(View.GONE);
            ticketEnquireButton.setVisibility(View.GONE);
        }
        else {
            freeEvent.setVisibility(View.GONE);
            if (! StringUtilities.isEmpty(event.getTicketWebsite())) {
                ticketEnquireButton.setVisibility(View.GONE);
                ticketPurchaseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(selectedEvent.getTicketWebsite()));
                        EventDetailsActivity.this.startActivity(browserIntent);
                    }
                });
            }
            else if (! StringUtilities.isEmpty(event.getWebsite())) {
                ticketPurchaseButton.setVisibility(View.GONE);
                ticketEnquireButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(selectedEvent.getWebsite()));
                        EventDetailsActivity.this.startActivity(browserIntent);
                    }
                });
            }
            else {
                //nothing we can do; discard the entire section
                findViewById(R.id.event_tickets_container).setVisibility(View.GONE);
            }
        }

        //event description
        descriptionView = (TextView)findViewById(R.id.event_text_description);
        if (! StringUtilities.isEmpty(event.getDescription())) {
            descriptionView.setText(event.getDescription());
        }
        else {
            //XXX:  there are actually quite a few events with no description available
            //descriptionView.setVisibility(View.GONE);
            findViewById(R.id.event_description_container).setVisibility(View.GONE);
        }

        FloatingActionButton socialFab = (FloatingActionButton)findViewById(R.id.fab_facebook);
        socialFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //FIXME:  implement
                Toast.makeText(EventDetailsActivity.this, "Feature not implemented.  Bummer!", Toast.LENGTH_SHORT).show();
            }
        });

        FloatingActionButton mediaFab = (FloatingActionButton)findViewById(R.id.fab_music);
        mediaFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(EventDetailsActivity.this)
                        .setTitle("Find Artist On...")
                        .setSingleChoiceItems(new String[] {"Youtube", "Triple J Unearthed"}, -1, null)
                        .setPositiveButton("Listen", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();

                                String url = null;
                                int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                                if (selectedPosition == 0) {
                                    //youtube
                                    url = "https://www.youtube.com/results?search_query=" + StringUtilities.encodeUriComponent(selectedEvent.getArtist().getName());
                                }
                                else if (selectedPosition == 1) {
                                    url = "https://www.triplejunearthed.com/search/site/" + StringUtilities.encodeUriComponent(selectedEvent.getArtist().getName());
                                }

                                if (! StringUtilities.isEmpty(url)) {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    EventDetailsActivity.this.startActivity(browserIntent);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        //used for details link
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (! StringUtilities.isEmpty(event.getWebsite())) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(selectedEvent.getWebsite()));
                    EventDetailsActivity.this.startActivity(browserIntent);
                }
            });
        }
        else {
            fab.setVisibility(View.GONE);
        }
    }
}
