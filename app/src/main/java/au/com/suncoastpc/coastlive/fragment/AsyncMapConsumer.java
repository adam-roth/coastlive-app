package au.com.suncoastpc.coastlive.fragment;

import com.google.android.gms.maps.GoogleMap;

/**
 * Created by aroth on 11/19/2016.
 */

public interface AsyncMapConsumer {
    public void consumeMap(GoogleMap googleMap);
}
