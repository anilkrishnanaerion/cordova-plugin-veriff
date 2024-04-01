

package org.apache.cordova.csantosm;

import android.content.Intent;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.Nullable;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

// Veriff imports
import com.veriff.Branding;
import com.veriff.Configuration;
import com.veriff.Result;
import com.veriff.Sdk;

public class VeriffCordovaPlugin extends CordovaPlugin {

  // Actions
  private static final String LAUNCH_ACTION = "launchVeriffSDK";

  private static final int REQUEST_CODE = 800;
  private static CallbackContext callbackContextApp;
  private static JSONObject veriffConfig = new JSONObject();


  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.callbackContextApp = callbackContext;
    if (action.equals(LAUNCH_ACTION)) {
      if (args.isNull(0)) {
        this.callbackContextApp.error("Error: Session token is required");
        return false;
      }
      if (!args.isNull(1)) {
        veriffConfig = new JSONObject(args.getString(1));
      }
      String sessionUrl = args.getString(0);
      this.launchVeriffSDK(sessionUrl);
      return true;
    }

    this.callbackContextApp.error("Method not implemented");
    return false;
  }

  private void launchVeriffSDK(String sessionUrl) {
    cordova.setActivityResultCallback(this);
    Branding.Builder branding = new Branding.Builder();

    if(veriffConfig.length() > 0) {
      try {
        branding.primary(Color.parseColor(veriffConfig.getString("themeColor")));
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }

    Configuration configuration = new Configuration.Builder()
            .branding(branding.build())
            .build();
    Intent intent = Sdk.createLaunchIntent(cordova.getActivity(), sessionUrl, configuration);
    cordova.startActivityForResult(this, intent, REQUEST_CODE);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == REQUEST_CODE && data != null) {
      Result result = Result.fromResultIntent(data);
      if (result != null) {
        try {
          handleResult(result);
        } catch (JSONException e) {
          this.callbackContextApp.error(e.getStackTrace().toString());
        }
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  public void handleResult(Result result) throws JSONException {
    JSONObject resultJson = new JSONObject();
    Result.Status status = result.getStatus();
    resultJson.put("status", status.toString());
    Log.d("Handle VeriffSDK result", status.toString());
    switch (status) {
      case CANCELED:
        // User canceled the session.
        resultJson.put("message", "User canceled the verification process");
        break;
      case ERROR:
        // An error occurred during the flow, Veriff has already shown UI, no need to display
        // a separate error message here
        resultJson.put("message", result.getError());
        break;
      case DONE:
        // Session is completed from clients perspective.
        resultJson.put("message", "Session is completed from clients perspective");
        break;
      default:
        resultJson.put("message", "Unknown result state");
        break;
    }
    this.callbackContextApp.success(resultJson);
  }

}
