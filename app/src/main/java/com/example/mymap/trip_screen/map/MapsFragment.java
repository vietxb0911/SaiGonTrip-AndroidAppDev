package com.example.mymap.trip_screen.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.mymap.FinishTrip;
import com.example.mymap.database.MyDatabase;
import com.example.mymap.database.MyLocation;
import com.example.mymap.R;
import com.example.mymap.database.TripLocation;
import com.example.mymap.trip_screen.map.scroll_view.Data;
import com.example.mymap.trip_screen.map.scroll_view.Item;
import com.example.mymap.trip_screen.map.scroll_view.ScrollViewAdapter;
import com.gigamole.infinitecycleviewpager.HorizontalInfiniteCycleViewPager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.example.mymap.home_screen.ChooseLocationActivity;

import static android.app.Activity.RESULT_OK;
import static com.example.mymap.home_screen.HomeActivity.mLocationsArrayList;

public class MapsFragment extends Fragment
        implements OnMapReadyCallback, RoutingListener{
    private static final String TAG = "MapsFragment";
    private static final double epsilon = 0.001;//check to at destination
    private int zoomDefault = 17;
    //google map object
    static GoogleMap mMap;
    static ArrayList<Marker> markersItems;
    private ArrayList<Polyline> polylinesRoute;

    private MyDatabase database;

    public static FusedLocationProviderClient mFusedLocationClient;
    PermissionLocation permissionLocation;

    public static int PERMISSION_ID = 44;

    private Button btnGotoRouting;
    private EditText editTextSearch;

    List<TripLocation> tripLocationList;
    static ArrayList<Route> routeMaps = null;
    static LatLng curLocation=null;
    ArrayList<LatLng> latLngArrayList = null;
    static int sizeLatLngList;
    static int roundIntent;
    static boolean startARouteInt;
    int tripId;
    private List<Item> dataItemScroll;

    Integer[] distanceList=null;
    int index_minDistance, round;
    ArrayList<Route> routeMapsRound = null;

    private Spinner spinner;
    private static final String[] items = {"","restaurant", "coffee", "gasstation"};
    private static final String[] itemsDisplay = {"Tìm địa điểm gần đây", "Nhà hàng", "Quán Coffee", "Trạm xăng"};
    public static final int[] iconItems = {0,R.mipmap.ic_restaurant,R.mipmap.ic_drink,R.mipmap.ic_gas};
    private HorizontalInfiniteCycleViewPager horizontalInfiniteCycleViewPager;

    public MapsFragment(){}

    public static MapsFragment newInstance(int param){
        MapsFragment fragment = new MapsFragment();
        Bundle args = new Bundle();
        args.putInt("param1", param);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: createview");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.activity_maps, container, false);
        btnGotoRouting = (Button) view.findViewById(R.id.btnGoToRouting);

        editTextSearch = (EditText)view.findViewById(R.id.edit_text_search);
        spinner = (Spinner)view.findViewById(R.id.spinnerNearby);

        SupportMapFragment mapFragment = (SupportMapFragment)getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        horizontalInfiniteCycleViewPager = view.findViewById(R.id.scrollPlace);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());//get intent from HomeActivity
        permissionLocation = new PermissionLocation(curLocation,MapsFragment.this);
        Bundle bundle = getArguments();
        if(bundle!=null)
            tripId = bundle.getInt("param1",0);
        initData();
        markersItems = null;
        startARouteInt = false;
        dataItemScroll = new Data(tripLocationList).getData();

        if (android.os.Build.VERSION.SDK_INT >= 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    private void initData() {
        database = MyDatabase.getInstance(getActivity());
        tripLocationList = database.myDAO().getListTripLocationFromTrip(tripId);
        //sort tripLocation
//        for (int i=0;i<tripLocationList.size();i++){
//            if (tripLocationList.get(i).getTimePassed()==null)
//                    continue;
//            for (int j=i+1;j<tripLocationList.size();j++){
//                if (tripLocationList.get(j).getTimePassed()==null ||
//                        tripLocationList.get(i).getTimePassed().compareTo(tripLocationList.get(j).getTimePassed())<0){
//                    TripLocation tmp = tripLocationList.get(i);
//                    tripLocationList.set(i,tripLocationList.get(j));
//                    tripLocationList.set(j,tmp);
//                }
//            }
//        }
        //init roundIntent
        if (tripLocationList.get(0).getTimePassed()==null) roundIntent=1;
        else if (tripLocationList.get(tripLocationList.size()-1).getTimePassed()!=null)
            roundIntent = tripLocationList.size();
        else for (int i=0;i<tripLocationList.size();i++){
                if (tripLocationList.get(i).getTimePassed()==null){
                    roundIntent = i;
                    break;
                }
            }
        Log.d(TAG, "initData: routeIntent start: "+roundIntent);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady: enter");
        mMap = googleMap;

        getLastLocation(false);

        horizontalInfiniteCycleViewPager.setAdapter(new ScrollViewAdapter(dataItemScroll, getContext(), mMap));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item,itemsDisplay);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (position!=0){
                    getPlacesWithItem(items[position], position);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        btnGotoRouting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (roundIntent < tripLocationList.size()+1){
                    startARoute();
                }
                else{
                    Intent intent = new Intent(getActivity(), FinishTrip.class);
                    intent.putExtra("tripId", tripId);
                    startActivity(intent);
                }
            }
        });

        //init places
        Places.initialize(getActivity(),getResources().getString(R.string.google_maps_key));
        //set editText non focusable
        editTextSearch.setFocusable(false);
        editTextSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //init place field list
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS,Place.Field.ID,
                        Place.Field.LAT_LNG, Place.Field.NAME);
                //create intent
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,
                        fieldList).build(getActivity());
                startActivityForResult(intent,100);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==100 && resultCode== RESULT_OK ){
            final Place place = Autocomplete.getPlaceFromIntent(data);
            Log.d(TAG, "onActivityResult: "+place.getName()+ " "+ place.getId());

            String url = "https://maps.googleapis.com/maps/api/place/details/json" +
                    "?place_id=" + place.getId() +
                    "&fields=formatted_phone_number,formatted_address,opening_hours,website,url,price_level,rating,name,user_ratings_total,review,photo" +
                    "&key=" + getActivity().getResources().getString(R.string.google_maps_key);
            Log.d("Maps", "getDetailPlace: " + url);
            //exe place task method to download json data
            new PlaceTask(mMap, MapsFragment.this, -1).execute(url);
        }
        else if (resultCode ==  AutocompleteActivity.RESULT_ERROR){
            Status status = Autocomplete.getStatusFromIntent(data);
            Toast.makeText(getActivity(),status.getStatusMessage(),Toast.LENGTH_SHORT).show();
        }
    }


    private void getPlacesWithItem(String item, int position) {
        if (curLocation!=null){
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                    "?location="+curLocation.latitude + ","+curLocation.longitude +
                    "&radius=50" +
                    "&name=" + item +
                    "&sensor=true" +
                    "&fields=name,rating,formatted_phone_number" +
                    "&key=" + getResources().getString(R.string.google_maps_key);

            Log.d(TAG, "getPlacesWithItem: "+ url);
            //exe place task method to download json data
            new PlaceTask(mMap,MapsFragment.this, position).execute(url);
        }
        else{
            Toast.makeText(getActivity(),"Unable to your Location!", Toast.LENGTH_SHORT).show();
        }
    }

    public void arrayRoutes() {
        //init for find route
        index_minDistance = 0;
        round=1;
        Log.d(TAG, "arrayRoutes: enter");
        //create list latlng for find routes
        latLngArrayList = new ArrayList<>();
        latLngArrayList.add(curLocation);
        for (int i=roundIntent-1; i<tripLocationList.size(); i++){
            latLngArrayList.add(mLocationsArrayList.get(tripLocationList.get(i).getLocationId()).getLatlngLatlng());
        }
        if (curLocation == null){
            Toast.makeText(getContext(),"Can get your location. Turn back again!",Toast.LENGTH_LONG).show();
        }
        else{
            sizeLatLngList = latLngArrayList.size();
            distanceList = new Integer[sizeLatLngList-1];

            routeMapsRound = new ArrayList<>();
            routeMaps = new ArrayList<Route>(sizeLatLngList-1);
            polylinesRoute = new ArrayList<Polyline>(sizeLatLngList-1);

            mMap.animateCamera( CameraUpdateFactory.newLatLngZoom(latLngArrayList.get(roundIntent-1), zoomDefault));
            mMap.addMarker(new MarkerOptions().position(curLocation)
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_home)));
            for (int nextLoca=1;nextLoca<sizeLatLngList;nextLoca++){
                for (int nextNextLoca = nextLoca;nextNextLoca<sizeLatLngList;nextNextLoca++){
                    Log.d(TAG, "arrayRoutes: "+nextLoca+" "+nextNextLoca);
                    Findroutes(latLngArrayList.get(nextLoca-1),latLngArrayList.get(nextNextLoca));
                }
                Log.d(TAG, "arrayRoutes: roundIntent"+roundIntent + "nextLoca: "+nextLoca);
                addScaleIconLocation(mMap,mLocationsArrayList.get(tripLocationList.get(nextLoca-1).getLocationId()),120,120, false);
            }
        }
    }


    private void addScaleIconLocation(GoogleMap mMap, MyLocation myLocation, int width, int height, boolean filter) {
        Bitmap image = MyLocation.getPicture(myLocation.getIcon());
        Bitmap scaledImage = Bitmap.createScaledBitmap(image, width, height, filter);
        BitmapDescriptor scaledIcon = BitmapDescriptorFactory.fromBitmap(scaledImage);
        mMap.addMarker(new MarkerOptions().position(myLocation.getLatlngLatlng())
                .title(myLocation.getName())
                .icon(scaledIcon));
    }


    public void startARoute() {
        Log.d(TAG, "startARoute: roundIntent:"+roundIntent);
        startARouteInt=true;
        btnGotoRouting.setText("Địa điểm kế tiếp");
        LatLng start = routeMaps.get(roundIntent-1).getPoints().get(0);
        for (int i=0;i<roundIntent-1;i++)
            showRoute(routeMaps.get(i),i,mMap,true,false);
        showRoute(routeMaps.get(roundIntent-1),roundIntent-1,mMap,false,startARouteInt);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(start,zoomDefault+1));
        if (roundIntent == tripLocationList.size())
            btnGotoRouting.setText("Kết thúc hành trình");
        btnGotoRouting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                roundIntent++;
                database.myDAO().updateTimePassed(tripId,tripLocationList.get(roundIntent-2).getLocationId(), Calendar.getInstance().getTime());
                tripLocationList.get(roundIntent-2).setTimePassed(Calendar.getInstance().getTime());
                //checkRightDestination(curLocation,end);
                if (roundIntent < tripLocationList.size()+1){
                    dataItemScroll = new Data(tripLocationList).getData();
                    horizontalInfiniteCycleViewPager.setAdapter(new ScrollViewAdapter(dataItemScroll, getContext(), mMap));
                    startARoute();
                }
                else{
                    Intent intent = new Intent(getActivity(), FinishTrip.class);
                    intent.putExtra("tripId", tripId);
                    startActivity(intent);
                }
            }
        });
    }


    private void checkRightDestination(LatLng curLocation, LatLng end) {
        if ((curLocation.latitude-end.latitude<epsilon && end.latitude-curLocation.latitude<epsilon)
                && (curLocation.longitude-end.longitude<epsilon && end.longitude-curLocation.longitude<epsilon)){
            database.myDAO().updateTimePassed(tripId,roundIntent-2,Calendar.getInstance().getTime());
        }
        else {
            Toast.makeText(getActivity(),"Here is not Destination. Please go on!",Toast.LENGTH_LONG).show();
        }
    }


    // function to find Routes.
    public void Findroutes(LatLng Start, LatLng End)
    {
        if(Start==null || End==null) {
            if (Start==null)
            Toast.makeText(getActivity(),"Unable to get your location", Toast.LENGTH_LONG).show();
        }
        else{
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(Start, End)
                    .key(getResources().getString(R.string.google_maps_key))
                    .build();

            routing.execute();
        }
    }

    //Routing call back functions.
    @Override
    public void onRoutingFailure(RouteException e) {
        Toast.makeText(getActivity(),"Finding Route failed!", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onRoutingFailure: ");
    }

    @Override
    public void onRoutingStart() {
        Toast.makeText(getActivity(),"Finding Route...",Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onRoutingStart: ");
    }

    @Override
    public void onRoutingCancelled() {
        Toast.makeText(getActivity(),"Finding Route cancelled!", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onRoutingCancelled: ");
    }


    //If Route finding success..
    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        //add route(s) to the map using polyline
        for (int i = 0; i <route.size(); i++) {
            if(i==shortestRouteIndex)
            {
                routeMapsRound.add(route.get(i));
                distanceList[index_minDistance]=route.get(i).getDistanceValue();
                index_minDistance+=1;
                if (index_minDistance == sizeLatLngList-round){
                    addARoute();
                }
                break;
            }
            Log.d(TAG, "onRoutingSuccess: ");
        }
    }

    void addARoute(){
        List < Integer > numberList = Arrays.asList(distanceList);
        index_minDistance = numberList.indexOf(Collections.min(numberList));
        if (index_minDistance!=0){
            //swap latlng
            LatLng temp = latLngArrayList.get(round);
            latLngArrayList.set(round,latLngArrayList.get(index_minDistance+round));
            latLngArrayList.set(index_minDistance+round,temp);

            //swap tripLocationList
            TripLocation tmp = tripLocationList.get(roundIntent - 1 + round-1);
            tripLocationList.set(roundIntent - 1 + round-1,tripLocationList.get(roundIntent - 1 + index_minDistance+round-1));
            tripLocationList.set(roundIntent - 1 + index_minDistance+round-1,tmp);
        }
        routeMaps.add(routeMapsRound.get(index_minDistance));
        showRoute(routeMaps.get(round-1),round-1, mMap,roundIntent>round, startARouteInt);
        round++;

        routeMapsRound.clear();
        routeMapsRound = new ArrayList<>();
        distanceList = new Integer[sizeLatLngList-round];
        index_minDistance = 0;
    }

    public void showRoute(Route r, int indexRoute, GoogleMap mMap, boolean changeColor, boolean startARoute){
        Log.d(TAG, "showRoute: sizePoliline:"+polylinesRoute.size());
        boolean deletepoly = true;
        if (polylinesRoute.size() == sizeLatLngList-1){
            polylinesRoute.get(indexRoute).remove();
            polylinesRoute.remove(indexRoute);
        }

        else
            deletepoly = false;
        PolylineOptions polyOptions = new PolylineOptions();

        if (changeColor)
            polyOptions.color(Color.argb(100,100,100,100));
        if (startARoute)
            polyOptions.color(Color.argb(255,0,0,255));
        polyOptions.width(7);
        polyOptions.addAll(r.getPoints());
        Polyline polyline = mMap.addPolyline(polyOptions);
        if (deletepoly)
            polylinesRoute.add(indexRoute,polyline);
        else polylinesRoute.add(polyline);
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation(final boolean onlyGetLocation)
    {
        Log.d(TAG, "getLastLocation: enter");
        // check if permissions are given
        if (permissionLocation.checkPermissions()) {
            // check if location is enabled
            if (permissionLocation.isLocationEnabled()) {

                Log.d(TAG, "getLastLocation: isLocationEnable");
                // getting last location from FusedLocationClient object
                Task<Location> task = mFusedLocationClient.getLastLocation();
                task.addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            curLocation = new LatLng(location.getLatitude()
                                    , location.getLongitude());
                            Log.d(TAG, "Location: " + curLocation.toString());
                            if (!onlyGetLocation){
                                arrayRoutes();
                            }
                            mMap.setMyLocationEnabled(true);
                        }
                        else  {
                            permissionLocation.requestNewLocationData();
                        }
                    }
                });
            }
            else {
                permissionLocation.showAlertDialog();
                getLastLocation(onlyGetLocation);
            }
        }
        else {
            // if permissions aren't available, request for permissions
            permissionLocation.requestPermissions();
            getLastLocation(onlyGetLocation);
        }
    }

    // If everything is alright then
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation(false);
            }
        }
    }
}