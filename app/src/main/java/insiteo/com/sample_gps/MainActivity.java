package insiteo.com.sample_gps;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.insiteo.lbs.Insiteo;
import com.insiteo.lbs.common.CommonConstants;
import com.insiteo.lbs.common.ISError;
import com.insiteo.lbs.common.auth.entities.ISUserSite;
import com.insiteo.lbs.common.init.ISEPackageType;
import com.insiteo.lbs.common.init.ISEServerType;
import com.insiteo.lbs.common.init.ISPackage;
import com.insiteo.lbs.common.init.listener.ISIInitListener;
import com.insiteo.lbs.common.utils.geometry.ISGeoMatrix;
import com.insiteo.lbs.common.utils.geometry.ISPointD;
import com.insiteo.lbs.location.ISILocationListener;
import com.insiteo.lbs.location.ISLocation;
import com.insiteo.lbs.location.ISLocationProvider;
import com.insiteo.lbs.map.entities.ISMap;
import com.insiteo.lbs.map.render.ISERenderMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity implements ISIInitListener, ISILocationListener {

    TextView latValue;
    TextView longValue;
    TextView textDl;
    public final static String TAG = "SampleApp GPS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latValue = (TextView)findViewById(R.id.text_lat_value);
        longValue = (TextView)findViewById(R.id.text_long_value);
        textDl = (TextView)findViewById(R.id.text_dl);
        boolean permRequired = checkPermissionStatus();

        if(!permRequired)
            init();
    }
    
    private void init(){
        String apiKey = "2ad95a1d-713d-4100-8d9f-4917f8dc8de7";
        String url = "https://services2." + ISEServerType.PROD + ".insiteo.com/v" +
                CommonConstants.URL_VERSION + "/Insiteo.Dispatch." + ISEServerType.PROD + "/Insiteo.Dispatch.svc";
        Insiteo.getInstance().initialize(this, this, apiKey, null, false, ISEServerType.PROD, url, ISERenderMode.MODE_2D);
    }

    @Override
    public void onInitDone(ISError error, ISUserSite suggestedSite, boolean fromLocalCache) {
        Log.d(TAG, "onInitDone");
        if(error == null) {
            Log.d(TAG, "onInitDone2");
            Insiteo.getInstance().startAndUpdate(suggestedSite, this);
            // The suggested site will be started
        }
    }

    @Override
    public void onStartDone(ISError error, Stack<ISPackage> packageToUpdate) {
        Log.d(TAG, "onStartDone");

        if(error == null) {
            if(!packageToUpdate.isEmpty()) {
                // Package update are available. They will be downloaded.
                Log.d(TAG, "onStartDone1");
            } else {
                // No package require to be updated. The SDK is no ready to be used.
                Log.d(TAG, "onStartDone2");
            }
        }
    }

    @Override
    public void onPackageUpdateProgress(ISEPackageType packageType, boolean download,
                                        long progress, long total) {
        showUpdateUI();
        textDl.setText("Downloading " + progress + " / " + total);
    }

    @Override
    public void onDataUpdateDone(ISError error) {
        if(error == null) {
            // Packages have been updated. The SDK is no ready to be used.
            Log.d(TAG, "onDataUpdateDone !");
            ISLocationProvider locProvider = ISLocationProvider.getInstance();
            locProvider.start(this);
            textDl.setText(" ");
        }
    }

    @Override
    public Location selectClosestToLocation() {
        return null;
    }


    private void showUpdateUI() {
        Log.d(TAG, "ShowUpdateUI");
    }

    @Override
    public void onLocationInitDone(ISError isError) {

    }

    @Override
    public void onLocationReceived(ISLocation isLocation) {
        Log.d("InsiteoLocation " , "X: " + isLocation.getCoord().x + " | Y: " + isLocation.getCoord().y);
        ISMap map = Insiteo.getCurrentSite().getMap(isLocation.getMapID());
        if(map == null)
            return;
        ISGeoMatrix matrix = map.getGeoMatrix();
        ISPointD latlongPoint = matrix.toLatLong(isLocation.getCoord().x, isLocation.getCoord().y);

        latValue.setText(latlongPoint.x +"");
        longValue.setText(latlongPoint.y +"");
    }

    @Override
    public void onAzimuthReceived(float v) {

    }

    @Override
    public void onCompassAccuracyTooLow() {

    }

    @Override
    public void onNeedToActivateGPS() {

    }

    @Override
    public void onLocationLost(ISLocation isLocation) {

    }

    @Override
    public void noRegisteredBeaconDetected() {

    }

    @Override
    public void onWifiActivationRequired() {

    }

    @Override
    public void onBleActivationRequired() {

    }


    //**********************************************************************************************
    // Android M permissions request
    //**********************************************************************************************

    private static final int PERMISSION_REQUEST = 0;

    private boolean checkPermissionStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final List<String> permissionsList = new ArrayList<String>();

            StringBuilder message = new StringBuilder();
            message.append("The application requires the following permissions: \n ");

            if (!isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                message.append("\n - ACCESS_COARSE_LOCATION in order to scan BLE and WIFI signals\n");

            }

            if (!isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                message.append("\n - WRITE_EXTERNAL_STORAGE in order to write temporary files on the SD card \n");

            }

            if (!isPermissionGranted(Manifest.permission.READ_PHONE_STATE)) {
                permissionsList.add(Manifest.permission.READ_PHONE_STATE);
                message.append("\n - READ_PHONE_STATE to pause scans on phone calls \n");

            }

            if (!permissionsList.isEmpty()) {
                displayPermissionsAlert("Permission required", message.toString(),
                        permissionsList.toArray(new String[permissionsList.size()]));
                return true;
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void displayPermissionsAlert(String title, String message, final String[] permissions) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                requestPermissions(permissions, PERMISSION_REQUEST);
            }
        });
        builder.show();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST: {

                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        init();
                        Log.d(TAG, permissions[i] + " granted");
                    } else {
                        Log.w(TAG, permissions[i] + " refused");
                        finish();
                    }
                }

                return;
            }
        }
    }

    private boolean isPermissionGranted(String permission) {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                permission);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }
}
