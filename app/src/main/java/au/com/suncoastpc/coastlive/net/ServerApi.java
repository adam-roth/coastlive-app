package au.com.suncoastpc.coastlive.net;

import android.util.Log;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import au.com.suncoastpc.coastlive.utils.Constants;
import au.com.suncoastpc.coastlive.utils.Environment;
import au.com.suncoastpc.coastlive.utils.StringUtilities;

public class ServerApi {
	private static final String PRODUCTION_SERVER = "http://terra.suncoastpc.com.au:8181";
	private static final String DEV_SERVER = "http://192.168.1.35:8080";

	private static final String PRODUCTION_API_KEY = "YOUR_PROD_KEY";
	private static final String DEV_API_KEY = "YOUR_DEV_KEY";

	private static final int CONNECT_TIMEOUT = 1000 * 30;			//30 second connect timeout
	private static final int REQUEST_TIMEOUT = 1000 * 60 * 4;		//4 minutes request timeout

	//FIXME:  strings bundle
	private static final String NETWORK_DISALLOWED_ON_MAIN_THREAD = "Application Error:  Network requests cannot be performed on the main/UI thread!";
	private static final String NETWORK_UNAVAILABLE = "Network Error:  Internet access is currently unavailable, please try again later.";
	private static final String NETWORK_ERROR = "Network Error:  Received a failing status code from the server - ";
	private static final String INVALID_FCM_TOKEN = "Registration Error:  Cannot register an empty FCM token!";

	private static final CookieManager COOKIE_MANAGER = new CookieManager();
	static {
		//make sure that we keep any cookies that the server might send (such as JSESSIONID)
		COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(COOKIE_MANAGER);
	}
	
	private static long requestId = 0;
	
	//XXX:  executes synchronously on the calling thread; immediately returns an 'error' response if called from the main thread
	public static JSONObject loadData(String context, String queryParams) {
		//check for basic validity of the request
		JSONObject error = checkNetwork();
		if (error != null) {
			return error;
		}
		
		//okay to proceed
		try {
			////format=json&apiKey=YOUR_API_KEY&params=where=1%3D1&f=pjson&context=Society/Society_SCRC/MapServer/17
			String url = getServerAddress() + ApiMethod.LOAD_DATA.getPath();
			String params = "format=json&apiKey=" + getApiKey()
					+ "&context=" + StringUtilities.encodeUriComponent(context)
					+ "&params=" + StringUtilities.encodeUriComponent(queryParams);

			Log.d("net", "Requesting data from the server:  " + params);
			return postToServer(url, params);
		}
		catch (Exception e) {
			//something bad happened
			if (Environment.isDebugMode()) {
				e.printStackTrace();
			}
			return errorResponse("Unexpected network error:  " + e.getMessage());
		}
	}

	public static JSONObject loadData(ApiContext context, String queryParams) {
		return loadData(context.getName(), queryParams);
	}
	
	//XXX:  always executes asynchronously in a new thread; passes the result back via a callback on the nominated handler
	public static long loadData(final String context, final String queryParams, final ApiResponseDelegate handler) {
		final long result = nextRequestId();
		new Thread() {
			@Override
			public void run() {
				JSONObject response = loadData(context, queryParams);
				handler.handleResponse(result, ApiMethod.LOAD_DATA, response);
			}
		}.start();
		
		return result;		//return this to the caller in case they want to track individual requests
	}

	public static long loadData(final ApiContext context, final String queryParams, final ApiResponseDelegate handler) {
		return loadData(context.getName(), queryParams, handler);
	}

	//XXX:  executes synchronously on the calling thread; immediately returns an 'error' response if called from the main thread
	public static JSONObject loadAggregateData(List<String> contexts, String queryParams) {
		//check for basic validity of the request
		JSONObject error = checkNetwork();
		if (error != null) {
			return error;
		}

		//okay to proceed
		try {
			Collections.sort(contexts);
			JSONArray contextsJson = new JSONArray();
			contextsJson.addAll(contexts);

			JSONObject mergedParams = new JSONObject();
			mergedParams.put("datasets", contextsJson);
			mergedParams.put("args", queryParams);

			//format=json&apiKey=<key>&context=Aggregator&params={"datasets":[<SCC Dataset>,<SCC Dataset>, ...],"args":<query params for ArcGIS>}
			String url = getServerAddress() + ApiMethod.LOAD_DATA.getPath();
			String params = "format=json&apiKey=" + getApiKey()
					+ "&context=" + StringUtilities.encodeUriComponent("Aggregator")
					+ "&params=" + StringUtilities.encodeUriComponent(mergedParams.toJSONString());

			Log.d("net", "Requesting data from the server:  " + params);
			return postToServer(url, params);
		}
		catch (Exception e) {
			//something bad happened
			if (Environment.isDebugMode()) {
				e.printStackTrace();
			}
			return errorResponse("Unexpected network error:  " + e.getMessage());
		}
	}

	public static JSONObject loadAggregateData(Collection<ApiContext> contexts, String queryParams) {
		List<String> strings = new ArrayList<>();
		for (ApiContext context : contexts) {
			strings.add(context.getName());
		}

		return loadAggregateData(strings, queryParams);
	}

	//XXX:  always executes asynchronously in a new thread; passes the result back via a callback on the nominated handler
	public static long loadAggregateData(final List<String> contexts, final String queryParams, final ApiResponseDelegate handler) {
		final long result = nextRequestId();
		new Thread() {
			@Override
			public void run() {
				JSONObject response = loadAggregateData(contexts, queryParams);
				handler.handleResponse(result, ApiMethod.LOAD_DATA, response);
			}
		}.start();

		return result;		//return this to the caller in case they want to track individual requests
	}

	public static long loadAggregateData(final Collection<ApiContext> contexts, final String queryParams, final ApiResponseDelegate handler) {
		List<String> strings = new ArrayList<>();
		for (ApiContext context : contexts) {
			strings.add(context.getName());
		}

		return loadAggregateData(strings, queryParams, handler);
	}
	
	//utils
	public static String getServerAddress() {
		if (Constants.USE_DEV) {
			return DEV_SERVER;
		}
		return PRODUCTION_SERVER;
	}

	protected static String getApiKey() {
		if (Constants.USE_DEV) {
			return DEV_API_KEY;
		}
		return PRODUCTION_API_KEY;
	}
	
	public static String getServerHostName() {
		return getServerAddress().split("\\:\\/\\/")[1].split("\\:")[0];
	}
	
	public static int getServerPort() {
		if (Constants.USE_DEV) {
			//for dev servers we expect the port to be explicitly specified as part of the server address
			String nameAndPort = getServerAddress().split("\\:\\/\\/")[1];
			if (nameAndPort.contains(":")) {
				return Integer.parseInt(nameAndPort.split("\\:")[1]);
			}
		}
		
		//for QA and production, we use the default port associated with the protocol
		return getServerAddress().startsWith("https") ? 443 : 80;
	}

	private static JSONObject uploadMultipartToServer(String url, String params, File file) throws Exception {
		PostMethod post = null;
		try {
			post = new PostMethod(url + "?" + params);
			Part[] parts = new Part[]{ new FilePart(file.getName().replace(".png", ".annotation.png"), file)};      //XXX:  the server will convert PNG images to JPEG (losing transparency!) unless the file name includes ".annotation"

			HttpMethodParams config = new HttpMethodParams();
			config.setSoTimeout(REQUEST_TIMEOUT);

			MultipartRequestEntity entity = new MultipartRequestEntity(parts, config);
			post.setRequestEntity(entity);

			HttpClient client = new HttpClient();
			int status = client.executeMethod(post);
			if (status == HttpStatus.SC_OK) {
				//parse and return response JSON object
				return (JSONObject) JSONValue.parse(post.getResponseBodyAsString());
			}

			//return error response
			Log.e("net", "Received failing status code when uploading file; status=" + status + ", file=" + file.getAbsolutePath() + ", request=" + post);
			return errorResponse(NETWORK_ERROR + status);
		}
		finally {
			if (post != null) {
				post.releaseConnection();
			}
		}
	}

	private static JSONObject postToServer(String url, String params) throws Exception {
		InputStream in = null;
		HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
		try {
			byte[] paramBytes = params.getBytes();

			//send as a POST request as the parameter length may be quite long
			configureConnection(connection);

			connection.setDoOutput( true );
			connection.setRequestMethod( "POST" );
			connection.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty( "charset", "utf-8");
			connection.setRequestProperty( "Content-Length", Integer.toString( paramBytes.length ));

			//write the params to the request body
			try( DataOutputStream wr = new DataOutputStream(connection.getOutputStream()) ) {
				wr.write(paramBytes);
			}

			//read response
			in = connection.getInputStream();
			return (JSONObject) JSONValue.parse(in);
		}
		finally {
			connection.disconnect();
			if (in != null) {
				in.close();
			}
		}
	}
	
	private static void configureConnection(HttpURLConnection connection) {
		connection.setConnectTimeout(CONNECT_TIMEOUT);
		connection.setReadTimeout(REQUEST_TIMEOUT);
		connection.setUseCaches(false);
	}
	
	private static JSONObject errorResponse(String message) {
		JSONObject result = new JSONObject();
		result.put("status", Constants.ERROR_STATUS);
		result.put("message", message);
		
		return result;
	}
	
	private static synchronized long nextRequestId() {
		requestId++;
		return requestId;
	}
	
	private static JSONArray collectionToJson(Collection<Long> ids) {
		JSONArray result = new JSONArray();
		
		if (ids != null) {
			for (long id : ids) {
				result.add(id);
			}
		}
		
		return result;
	}
	
	private static JSONObject checkNetwork() {
		if (Environment.isMainThread()) {
			return errorResponse(NETWORK_DISALLOWED_ON_MAIN_THREAD);
		}
		if (! Environment.isNetworkAvailable()) {
			return errorResponse(NETWORK_UNAVAILABLE);
		}
		
		//XXX:  can't perform this check here, as it may take several seconds to complete if the server is unresponsive
		//if (! Environment.isServerAvailable()) {
		//	return errorResponse(SERVER_UNAVAILABLE);
		//}

		return null;
	}
}
