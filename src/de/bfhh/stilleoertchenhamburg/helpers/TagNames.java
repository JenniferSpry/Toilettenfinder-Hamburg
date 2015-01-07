package de.bfhh.stilleoertchenhamburg.helpers;

public class TagNames {
	
	// Broadcast Intent Actions
	public static final String BROADC_LOCATION_NEW = "com.bfhh.stilleoertchenhamburg.userLocationNew";
	public static final String BROADC_LOCATION_UPDATED = "com.bfhh.stilleoertchenhamburg.userLocationUpdated";
	public static final String BROADC_POIS = "com.bfhh.stilleoertchenhamburg.POIListAvaliable";
	
	// Intent Actions
	public static final String ACTION_GET_POIS = "com.bfhh.stilleoertchenhamburg.getPois";
	public static final String ACTION_SHOW_SLIDER = "com.bfhh.stilleoertchenhamburg.showSlider";
	
	// Intent Extras
	public static final String EXTRA_LAT = "com.bfhh.stilleoertchenhamburg.latitude";
	public static final String EXTRA_LONG = "com.bfhh.stilleoertchenhamburg.longitude";
	public static final String EXTRA_RADIUS = "com.bfhh.stilleoertchenhamburg.radius";
	
	public static final String EXTRA_PROVIDER = "com.bfhh.stilleoertchenhamburg.provider";
	public static final String EXTRA_POI_LIST = "com.bfhh.stilleoertchenhamburg.poiList";
	public static final String EXTRA_LOCATION_RESULT = "com.bfhh.stilleoertchenhamburg.LocationResult";
	public static final String EXTRA_POI = "com.bfhh.stilleoertchenhamburg.POI";
	
	public static final String EXTRA_E_MAIL = "com.bfhh.stilleoertchenhamburg.E-Mail";
	public static final String EXTRA_NAME = "com.bfhh.stilleoertchenhamburg.Name";
	public static final String EXTRA_COMMENT = "com.bfhh.stilleoertchenhamburg.Comment";
	
	public static final String EXTRA_CAMERA_POS = "com.bfhh.stilleoertchenhamburg.CameraPosition";
	public static final String EXTRA_SHOW_ROUTE = "com.bfhh.stilleoertchenhamburg.showRoute";
	
	//Internet Connection Check
	public static final String SETTINGS_WIFI = "com.bfhh.stilleoertchenhamburg.settingsWifi";
	public static final String SETTINGS_MOBILE = "com.bfhh.stilleoertchenhamburg.settingsMobile";
	public static final String SETTINGS_NETWORK_GENERAL = "com.bfhh.stilleoertchenhamburg.settingsNetworkGeneral";
	
	public static final int TYPE_WIFI = 1;
    public static final int TYPE_MOBILE = 2;
    public static final int TYPE_NOT_CONNECTED = 0;

}
