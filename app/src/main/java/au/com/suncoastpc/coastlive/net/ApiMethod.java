package au.com.suncoastpc.coastlive.net;

public enum ApiMethod {
	LOAD_DATA("/data-gateway/api/loadData");			        //format=json&apiKey=e4335a64660e40b1826ab61296bb0a26&params=where=1%3D1&f=pjson&context=Society/Society_SCRC/MapServer/17

	private String path;
	
	private ApiMethod(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
