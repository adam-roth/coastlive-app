package au.com.suncoastpc.coastlive.net;

import net.minidev.json.JSONObject;

public interface ApiResponseDelegate {
	public void handleResponse(final long requestId, final ApiMethod requestApi, final JSONObject response);
}
