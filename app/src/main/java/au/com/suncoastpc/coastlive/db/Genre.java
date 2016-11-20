package au.com.suncoastpc.coastlive.db;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class Genre {
    @DatabaseField(canBeNull = false, id = true, index = true, unique = true)
    protected String name;              //'categories'

    @DatabaseField(canBeNull = false)
    protected boolean enabled;

    public Genre() {
        this.name = MusicEvent.PLACEHOLDER_ENTITY_ID;
        this.enabled = false;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof Genre) || obj == null) {
            return false;
        }

        Genre entity = (Genre)obj;
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
