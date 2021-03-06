package com.andrewvora.apps.mynpu.activities;

import android.animation.Animator;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.andrewvora.apps.mynpu.OurApplication;
import com.andrewvora.apps.mynpu.R;
import com.andrewvora.apps.mynpu.dialogs.NpuDialogFragment;
import com.andrewvora.apps.mynpu.fragments.OurMapFragment;
import com.andrewvora.apps.mynpu.listeners.SimpleAnimatorListener;
import com.andrewvora.apps.mynpu.utils.GeoUtil;
import com.andrewvora.apps.mynpu.utils.ViewUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonMultiPolygon;
import com.google.maps.android.geojson.GeoJsonPolygon;
import com.google.maps.android.ui.IconGenerator;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;

/**
 * An {@link android.app.Activity} that handles map-related features of this app:
 *  - finding NPUs
 *  - Seeing the NPU boundaries
 *  - setting their NPU
 *      - from an address
 *      - from a dropdown menu
 *
 * Created by root on 5/29/16.
 * @author flqa
 */
public class MapActivity extends AppCompatActivity implements
        OnMapReadyCallback, NpuDialogFragment.EventListener
{
    /*===========================================*
     * Constants
     *===========================================*/
    public static final String TAG_NPU = "CurrentlySetNpu";

    /*===========================================*
     * Member Variables
     *===========================================*/
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.input_address) EditText mAddressEditText;
    @BindView(R.id.text_npu) TextView mSelectedNpuTextView;
    @BindView(R.id.search_fab) FloatingActionButton mSearchFab;

    private GeoJsonLayer mNpuLayer;
    private GoogleMap mGoogleMap;
    private Marker mCurrentLocMarker;

    /*===========================================*
     * Overridden Methods
     *===========================================*/
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);

        // configure Toolbar
        setSupportActionBar(mToolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_map_activity);
        }

        // configure the Map
        // get the MapFragment instance
        OurMapFragment fragment = (OurMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);

        // begin fetching the map data
        if(fragment != null) {
            fragment.getMapAsync(this);
        }

        // configure Views
        String savedNpu;
        if((savedNpu = getSharedPreferences(OurApplication.APP_PREFERENCES, MODE_PRIVATE)
                .getString(TAG_NPU, "")).isEmpty())
        {
            savedNpu = getString(R.string.title_dialog_select_npu);
        }

        mSelectedNpuTextView.setText(savedNpu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        // set up the default camera position of the map
        // should start from the ATL area
        float zoomLvl = 10.5f; // enough to see all the NPUs
        LatLng atlantaLatLng = new LatLng(33.7490, -84.3880); // the center of Atlanta
        // update camera position
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(atlantaLatLng, zoomLvl);
        googleMap.moveCamera(cameraUpdate);

        // apply the NPU layer onto the map
        try {
            mNpuLayer = new GeoJsonLayer(googleMap, R.raw.npus, getApplicationContext());
            mNpuLayer.getDefaultPolygonStyle().setStrokeWidth(2f);
            mNpuLayer.addLayerToMap();
        }
        catch (Exception ioe) {
            Log.e(MapActivity.class.getSimpleName(), ioe.getMessage());
        }

         // display a label for each NPU
        for(GeoJsonFeature feature : mNpuLayer.getFeatures()) {
            String npuLetter = feature.getProperty("NPU");
            String npu = String.format(getString(R.string.text_npu_template), npuLetter);

            IconGenerator ig = new IconGenerator(this);
            ig.setColor(ViewUtil.getRandomColor(this));
            ig.setTextAppearance(R.style.MarkerTextStyle);
            Bitmap icon = ig.makeIcon(npu);
            BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(icon);

            List<LatLng> coordinates = null;
            if(feature.hasGeometry() && feature.getGeometry().getType().equals("MultiPolygon")) {
                GeoJsonMultiPolygon polygon = (GeoJsonMultiPolygon) feature.getGeometry();
                coordinates = polygon.getPolygons().get(0).getCoordinates().get(0);
            }
            else if(feature.hasGeometry() && feature.getGeometry().getType().equals("Polygon")) {
                GeoJsonPolygon polygon = (GeoJsonPolygon) feature.getGeometry();
                coordinates = polygon.getCoordinates().get(0);
            }

            if (coordinates != null) {
	            LatLng center = GeoUtil.getCentroid(coordinates);

	            MarkerOptions markerOptions = new MarkerOptions();
	            markerOptions.draggable(false).position(center).icon(descriptor);
	            mGoogleMap.addMarker(markerOptions);
            }
        }
    }

    @Override
    public void onNpuSelected(int index) {
        String npu = getResources().getStringArray(R.array.list_npus)[index];
        setNpuInPreferences(npu);

        mSelectedNpuTextView.setText(npu);
    }

    /*===========================================*
     * Event Listener Methods
     *===========================================*/
    @OnClick(R.id.text_npu)
    void onSelectedNpuClicked() {
        String[] npuArr = getResources().getStringArray(R.array.list_npus);
        String selectedNpuName = mSelectedNpuTextView.getText().toString();

        int selectedNpuIndex = getIndexOf(selectedNpuName, npuArr);

        Bundle arguments = new Bundle();
        arguments.putInt(NpuDialogFragment.TAG_SELECTED_INDEX, selectedNpuIndex);

        NpuDialogFragment selectNpuDialog = new NpuDialogFragment();
        selectNpuDialog.setArguments(arguments);
        selectNpuDialog.show(getFragmentManager(), NpuDialogFragment.TAG);
    }

    @OnClick(R.id.search_fab)
    void onSearchFabClicked() {
        // update the Views' visibilities
        toggleNpuViews();

        // hide the keyboard
        ViewUtil.hideSoftKeyboard(mAddressEditText);
    }

    @OnEditorAction(R.id.input_address)
    boolean onSearchEditorAction(TextView v, KeyEvent event) {
        if(event == null || event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if(mCurrentLocMarker != null) mCurrentLocMarker.remove();

        LatLng latLng = reverseGeocodeAddress(v.getText().toString());
        GeoJsonFeature feature = determineNPU(latLng);

	    // add marker to show our geocoded result
	    addMarker(latLng);

        boolean hasFeature = feature != null;

        if(hasFeature) {
            // prompt the user if they would like to set the marker
            String template = getString(R.string.text_npu_template);
            String npu = String.format(template, feature.getProperty("NPU"));
            mSelectedNpuTextView.setText(npu);

            // save the NPU into SharedPrefs
            setNpuInPreferences(npu);
            // update the UI
            toggleNpuViews();
            // hide the keyboard
            ViewUtil.hideSoftKeyboard(mAddressEditText);
        } else {
            moveMapToCurrentLoc();
            showFailureSnackBar();
        }

        return hasFeature;
    }

    /*===========================================*
     * Private Methods
     *===========================================*/
    private void addMarker(LatLng latLng) {
	    MarkerOptions markerOptions = new MarkerOptions()
			    .title(getString(R.string.title_geocode_result_marker))
			    .draggable(false)
			    .position(latLng);

	    mCurrentLocMarker = mGoogleMap.addMarker(markerOptions);
    }

    private void moveMapToCurrentLoc() {
    	if (mGoogleMap != null && mCurrentLocMarker != null) {
    		mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(mCurrentLocMarker.getPosition()));
	    }
    }

    private void showFailureSnackBar() {
	    Snackbar.make((View) mToolbar.getParent(),
			    getString(R.string.text_dialog_cannot_find_npu),
			    Snackbar.LENGTH_INDEFINITE)
	            .setAction(R.string.dismiss, v -> {})
	            .show();
    }

    private void toggleNpuViews() {
        final int animDuration = 200;
        final boolean searchMode = mAddressEditText.getVisibility() == View.VISIBLE;
        final int drawableRes = searchMode ?
                R.drawable.ic_search_white_24dp :
                R.drawable.ic_close_white_24dp;

        // toggle the visibility of the Views
        if(searchMode) {
            mSelectedNpuTextView.setAlpha(0.0f);
            mSelectedNpuTextView.setVisibility(View.VISIBLE);

            mSelectedNpuTextView.animate()
		            .alpha(1.0f)
		            .setDuration(animDuration)
		            .setListener(null)
                    .start();

            mAddressEditText.animate()
		            .alpha(0.0f)
                    .setDuration(animDuration)
		            .setListener(new SimpleAnimatorListener() {
		                @Override
		                public void onAnimationEnd(Animator animation) {
		                        mAddressEditText.setVisibility(View.GONE);
		                }
		            }).start();
        }
        else {
            mAddressEditText.setAlpha(0.0f);
            mAddressEditText.setVisibility(View.VISIBLE);

            mAddressEditText.animate()
		            .alpha(1.0f)
		            .setDuration(animDuration)
		            .setListener(null)
                    .start();

            mSelectedNpuTextView.animate()
		            .alpha(0.0f)
                    .setDuration(animDuration)
		            .setListener(new SimpleAnimatorListener() {
		                @Override
		                public void onAnimationEnd(Animator animation) {
		                	mAddressEditText.requestFocus();
		                    mSelectedNpuTextView.setVisibility(View.GONE);
		                }
		            }).start();
        }

        // change the FAB icon accordingly
        mSearchFab.animate().rotation(searchMode ? 0f : 180f)
                .setListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mSearchFab.setImageResource(drawableRes);
                    }
                })
                .setDuration(animDuration).start();
    }

    private void setNpuInPreferences(String npuLetter) {
        boolean saved = getSharedPreferences(OurApplication.APP_PREFERENCES, MODE_PRIVATE)
                .edit().putString(TAG_NPU, npuLetter).commit();
        if(saved) {
            String msg = String.format(getString(R.string.text_npu_set), npuLetter.toUpperCase());
            View rootView = (View) mToolbar.getParent();

            Snackbar.make(rootView, msg, Snackbar.LENGTH_LONG)
		            .setAction(R.string.dismiss, v -> {})
		            .show();
        }
    }

    private LatLng reverseGeocodeAddress(String address) {
        Geocoder geocoder = new Geocoder(this);
        LatLng geocodeResult = null;

        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);

            if(addresses.size() > 0) {
                Address firstResult = addresses.get(0);
                geocodeResult = new LatLng(firstResult.getLatitude(), firstResult.getLongitude());
            }
        }
        catch (Exception e) {
            // reverse geocoder failed
            Log.e(MapActivity.class.getSimpleName(), e.getMessage());
        }

        return geocodeResult;
    }

    private GeoJsonFeature determineNPU(LatLng coord) {
        if(coord == null) return null;

        for(GeoJsonFeature feature : mNpuLayer.getFeatures()) {
            // check that the feature is a Polygon
            if(feature.hasGeometry() && feature.getGeometry().getType().equals("Polygon")) {
                GeoJsonPolygon polygon = (GeoJsonPolygon) feature.getGeometry();

                // check if the geocoded LatLng is within the feature
                boolean containsAddress = PolyUtil.containsLocation(
                		coord,
		                polygon.getCoordinates().get(0),
		                false);

                if(containsAddress) {
                	return feature;
                }
            }
            else if(feature.hasGeometry() && feature.getGeometry().getType().equals("MultiPolygon")) {
                GeoJsonMultiPolygon multiPolygon = (GeoJsonMultiPolygon) feature.getGeometry();
                for(GeoJsonPolygon polygon : multiPolygon.getPolygons()) {
                    // check if the geocoded LatLng is within the feature
                    boolean containsAddress = PolyUtil.containsLocation(
                    		coord,
		                    polygon.getCoordinates().get(0),
		                    false);

                    if(containsAddress) {
                    	return feature;
                    }
                }
            }
        }

        return null;
    }

    private static <T> int getIndexOf(T item, T[] items) {
        for(int i = 0; i < items.length; i++) {
            if(items[i].equals(item)) {
                return i;
            }
        }

        return -1;
    }
}
