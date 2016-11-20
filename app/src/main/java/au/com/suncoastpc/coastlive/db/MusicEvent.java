package au.com.suncoastpc.coastlive.db;

import android.util.Log;

import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import au.com.suncoastpc.coastlive.utils.DatabaseHelper;
import au.com.suncoastpc.coastlive.utils.StringUtilities;

@DatabaseTable
public class MusicEvent {
    //HACK:  server is parsing times based upon a U.S. TZ, so we correct them here; ideally server should parse using the correct timezone
    //should be:  Friday, 18 Nov @ 1800; displays as saturday @ 0900
    private static final long TZ_CORRECTION_FACTOR = 1000L * 60 * 60 * 15;
    private static final long ONE_DAY = 1000L * 60 * 60 * 24;

    public static final String PLACEHOLDER_ENTITY_ID = "-1";

    @DatabaseField(canBeNull = false, id = true, index = true, unique = true)
    protected String id;                    //__collationIdentifier

    @DatabaseField(canBeNull = false)
    protected double latitude;              //'latitude'

    @DatabaseField(canBeNull = false)
    protected double longitude;             //'longitude'

    @DatabaseField(canBeNull = true)
    protected String description;           //'description'

    @DatabaseField(canBeNull = false)       //XXX:  JSONArray of genre strings; called "categories" in the JSON data
    protected String genresJson;            //'categories

    @DatabaseField(canBeNull = false)
    protected long startDate;               //'startDate'

    @DatabaseField(canBeNull = false)
    protected long startTime;               //'startTime'

    @DatabaseField(canBeNull = true)
    protected String website;               //XXX:  may be sent under 'website' or 'detailsLink' in the JSON

    @DatabaseField(canBeNull = true)
    protected String ticketWebsite;         //XXX:  sent as 'ticketLink' in the JSON, may be redundant with 'detailsLink'

    @DatabaseField(canBeNull = true)
    protected String ageType;               //'ageType'; content is variable

    @DatabaseField(canBeNull = false)
    protected boolean freeConcert;          //'is_free'; false or true

    @DatabaseField(canBeNull = false)
    protected String originalJson;          //XXX:  the original JSON data that this event was created from; may be useful during migrations if we want to promote data up into new top-level fields

    @DatabaseField(canBeNull=false, foreign=true)
    protected Artist artist;                //'artistName'

    @DatabaseField(canBeNull=false, foreign=true)
    protected Venue venue;                  //'locationName'

    public MusicEvent() {
        this.freeConcert = false;
    }

    public String getId() {
        return StringUtilities.isEmpty(id) ? PLACEHOLDER_ENTITY_ID : id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getGenresJson() {
        return genresJson;
    }
    public void setGenresJson(String genresJson) {
        this.genresJson = genresJson;
    }

    public long getStartDate() {
        return startDate;
    }
    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getStartTime() {
        return startTime;
    }
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public String getWebsite() {
        return website;
    }
    public void setWebsite(String website) {
        this.website = website;
    }

    public String getTicketWebsite() {
        return ticketWebsite;
    }
    public void setTicketWebsite(String ticketWebsite) {
        this.ticketWebsite = ticketWebsite;
    }

    public String getAgeType() {
        return ageType;
    }
    public void setAgeType(String ageType) {
        this.ageType = ageType;
    }

    public boolean isFreeConcert() {
        return freeConcert;
    }
    public void setFreeConcert(boolean freeConcert) {
        this.freeConcert = freeConcert;
    }

    public String getOriginalJson() {
        return originalJson;
    }
    public void setOriginalJson(String originalJson) {
        this.originalJson = originalJson;
    }

    public Artist getArtist() {
        if (artist != null && PLACEHOLDER_ENTITY_ID.equals(artist.getName())) {
            artist = this.resolveDatabaseRelationship(Artist.class, "artist_id", artist);
        }
        return artist;
    }
    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public Collection<Genre> getGenres() {
        Set<Genre> result = new LinkedHashSet<>();

        JSONArray storedGenres = (JSONArray)JSONValue.parse(getGenresJson());
        RuntimeExceptionDao<Genre, String> genreDao = DatabaseHelper.getHelper().getRuntimeDao(Genre.class);
        for (Object nameObj : storedGenres) {
            String name = nameObj.toString();
            Genre genre = genreDao.queryForId(name);
            if (genre != null) {
                result.add(genre);
            }
        }

        return result;
    }

    public Venue getVenue() {
        if (venue != null && (PLACEHOLDER_ENTITY_ID.equals(venue.getName()) || StringUtilities.isEmpty(venue.getAddress()))) {
            venue = this.resolveDatabaseRelationship(Venue.class, "venue_id", venue);
        }
        return venue;
    }
    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    @SuppressWarnings("unchecked")
    protected <T> T resolveDatabaseRelationship(Class<T> foreignClass, String fkColumnName, T defaultValue) {
        T result = defaultValue;
        try {
            //first, query for the actual foreign-key value
            DatabaseHelper dbHelper = DatabaseHelper.getHelper();
            RuntimeExceptionDao<?, String> dao = dbHelper.getRuntimeDao(this.getClass());
            GenericRawResults<String[]> vals = dao.queryBuilder().selectRaw("id", fkColumnName).where().idEq(this.getId()).queryRaw();

            //now lookup the entity based on the foreign-key
            String[] keys = vals.getFirstResult();
            if (keys != null && keys.length > 1) {
                String foreignKey = keys[1];
                RuntimeExceptionDao<T, String> foreignDao = dbHelper.getRuntimeDao(foreignClass);

                T foreignEntity = foreignDao.queryForId(foreignKey);
                result =  foreignEntity != null ? foreignEntity : result;
            }
        }
        catch (Exception ignored) {
            ignored.printStackTrace();
        }

        return result;
    }


    public void loadFromJson(JSONObject data) {
        try {
            DatabaseHelper helper = DatabaseHelper.getHelper();
            RuntimeExceptionDao<Artist, String> artistDao = helper.getRuntimeDao(Artist.class);
            RuntimeExceptionDao<Genre, String> genreDao = helper.getRuntimeDao(Genre.class);
            RuntimeExceptionDao<Venue, String> venueDao = helper.getRuntimeDao(Venue.class);

            if (StringUtilities.isEmpty(data.getAsString("artistName"))
                    || StringUtilities.isEmpty(data.getAsString("locationName"))
                    || StringUtilities.isEmpty(data.getAsString("latitude"))
                    || StringUtilities.isEmpty(data.getAsString("longitude"))
                    || StringUtilities.isEmpty(data.getAsString("__collationIdentifier"))
                    || StringUtilities.isEmpty(data.getAsString("startDate"))
                    || StringUtilities.isEmpty(data.getAsString("startTime"))) {
                //missing required data; cannot proceed
                Log.w("db.sync", "Received an invalid event; json=" + data.toJSONString());
                return;
            }

            //first, ensure the required artist, venue, and genre(s) all exist
            String artistName = data.getAsString("artistName");

            //HACK:  clean up garbage chars and promotional text that shouldn't be there
            artistName = artistName.replaceAll("[^a-zA-Z0-9 '\"!@#$%\\^\\&\\*\\(\\)\\-_\\+\\=\\[\\]\\{\\}\\?/]", "").replace(" - NEW YEARS EVE", "");
            if (! artistName.startsWith("New Year's Eve")) {
                artistName = artistName.replaceAll(" @ [a-zA-Z0-9 ]+", "");
            }

            Artist artist = artistDao.queryForId(artistName);
            if (artist == null) {
                artist = new Artist();
                artist.setName(artistName);
                artist.setFavorite(false);

                Log.d("db.sync", "Creating new artist:  " + artistName);
                artistDao.create(artist);
            }

            String venueName = data.getAsString("locationName");
            double latitude = data.getAsNumber("latitude").doubleValue();
            double longitude = data.getAsNumber("longitude").doubleValue();
            String address = StringUtilities.isEmpty(data.getAsString("address")) ? latitude + ", " + longitude : data.getAsString("address");

            //HACK:  clean up some garbage in the address data
            address = address.replaceAll("QLD [0-9]+", "").replace("Noosa Heads, Noosa Heads", "Noosa Heads").replace(" Noosaville, Queensland, Noosaville", ", Noosaville")
                    .replace("Bokarina, Bokarina", "Bokarina").replace(", Sunshine Coast", "").replace("Noosa Heads , Noosa Heads", "Noosa Heads");


            Venue venue = venueDao.queryForId(venueName);
            if (venue == null) {
                venue = new Venue();
                venue.setName(venueName);
                venue.setAddress(address);

                //XXX:  we store latitude and longitude against both the event and the venue due to the possibility that with a sufficiently large venue you may get events occurring at particular, geographically distinct spots within the venue
                venue.setLatitude(latitude);
                venue.setLongitude(longitude);

                venue.setFavorite(false);

                Log.d("db.sync", "Creating new venue:  " + venueName + ", address=" + address);
                venueDao.create(venue);
            }

            JSONArray genreList = (JSONArray)data.get("categories");
            for (Object genreObj : genreList) {
                String genreName = genreObj.toString();
                Genre genre = genreDao.queryForId(genreName);
                if (genre == null) {
                    genre = new Genre();
                    genre.setName(genreName);
                    //genre.setEnabled(true);

                    genreDao.create(genre);
                }
            }

            this.setId(data.getAsString("__collationIdentifier"));
            this.setArtist(artist);
            this.setVenue(venue);
            this.setLatitude(latitude);
            this.setLongitude(longitude);
            this.setGenresJson(genreList.toJSONString());
            this.setDescription(data.getAsString("description"));
            if (data.containsKey("artists")) {
                try {
                    JSONObject fullDescription = (JSONObject)data.get("artists");
                    if (fullDescription.containsKey("artists")) {
                        JSONArray artistList = (JSONArray)fullDescription.get("artists");
                        fullDescription = (JSONObject)artistList.get(0);
                        if (! StringUtilities.isEmpty(fullDescription.getAsString("description"))) {
                            this.setDescription(fullDescription.getAsString("description"));
                        }
                    }
                }
                catch (Throwable ignored) {}
            }
            this.setStartDate(data.getAsNumber("startDate").longValue() - TZ_CORRECTION_FACTOR);
            this.setStartTime(data.getAsNumber("startTime").longValue() - TZ_CORRECTION_FACTOR);
            this.setWebsite(data.getAsString("website"));
            if (! StringUtilities.isEmpty(data.getAsString("detailsLink"))) {
                this.setWebsite(data.getAsString("detailsLink"));
            }
            this.setTicketWebsite(data.getAsString("ticketLink"));
            this.setAgeType(data.getAsString("ageType"));
            this.setFreeConcert("true".equals(data.getAsString("is_free")) || "1".equals(data.getAsString("is_free")));

            this.setOriginalJson(data.toJSONString());
        }
        catch (Throwable ignored) {
            //nothing we can do
        }
    }

    @Override
    public String toString() {
        return this.getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof MusicEvent) || obj == null) {
            return false;
        }

        MusicEvent entity = (MusicEvent)obj;
        return entity.getId() != null && entity.getId().equals(this.getId());
    }

    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }
        return super.hashCode();
    }
}


