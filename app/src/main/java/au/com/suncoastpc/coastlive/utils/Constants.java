package au.com.suncoastpc.coastlive.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Constants {
	public static final boolean USE_DEV = false;
	public static final boolean FORCE_RESYNC = true;
	
	public static final String SUCCESS_STATUS = "success";
	public static final String ERROR_STATUS = "error";
	
	public static DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
	public static DateFormat DEFAULT_DATE_FORMAT_WITH_TIME = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a");
}
