package au.com.suncoastpc.coastlive.net;

public enum ApiMethod {
	LOAD_DATA("/data-gateway/api/loadData");			        //format=json&apiKey=...&params=where=1%3D1&f=pjson&context=Society/Society_SCRC/MapServer/17

	private String path;
	
	private ApiMethod(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
