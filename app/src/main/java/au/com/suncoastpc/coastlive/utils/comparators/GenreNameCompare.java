package au.com.suncoastpc.coastlive.utils.comparators;

import java.util.Comparator;

import au.com.suncoastpc.coastlive.db.Genre;

public class GenreNameCompare implements Comparator<Genre> {

    @Override
    public int compare(Genre left, Genre right) {
        return left.getName().compareToIgnoreCase(right.getName());
    }
}
