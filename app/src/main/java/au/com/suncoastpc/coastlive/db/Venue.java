package au.com.suncoastpc.coastlive.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class Venue {
    @DatabaseField(canBeNull = false, id = true, index = true, unique = true)
    protected String name;          //'locationName'

    @DatabaseField(canBeNull = false)
    protected String address;       //'address', or fallback to lat/long if no address is provided

    @DatabaseField(canBeNull = false)
    protected double latitude;              //'latitude'

    @DatabaseField(canBeNull = false)
    protected double longitude;             //'longitude'

    @DatabaseField(canBeNull = false)
    protected boolean favorite;

    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public Venue() {
        this.name = MusicEvent.PLACEHOLDER_ENTITY_ID;
    }

    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isFavorite() {
        return favorite;
    }
    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof Venue) || obj == null) {
            return false;
        }

        Venue entity = (Venue)obj;
        return entity.getName() != null && entity.getName().equals(this.getName());
    }

    @Override
    public int hashCode() {
        if (this.getName() != null) {
            return this.getName().hashCode();
        }
        return super.hashCode();
    }
}
