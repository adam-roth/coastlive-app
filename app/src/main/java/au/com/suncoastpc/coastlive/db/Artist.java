package au.com.suncoastpc.coastlive.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class Artist {
    @DatabaseField(canBeNull = false, id = true, index = true, unique = true)
    protected String name;              //'artistName'

    @DatabaseField(canBeNull = false)
    protected boolean favorite;

    public Artist() {
        this.name = MusicEvent.PLACEHOLDER_ENTITY_ID;
        this.favorite = false;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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
        if (! (obj instanceof Artist) || obj == null) {
            return false;
        }

        Artist entity = (Artist)obj;
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
