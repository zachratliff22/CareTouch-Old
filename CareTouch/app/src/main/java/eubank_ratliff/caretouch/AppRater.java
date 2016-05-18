package eubank_ratliff.caretouch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

public class AppRater {
    private final static String APP_TITLE = "CareTouch";// App Name
    private final static String APP_PNAME = "eubank_ratliff.caretouch";// Package Name

    private final static int DAYS_UNTIL_PROMPT = 3;//Min number of days
    private final static int LAUNCHES_UNTIL_PROMPT = 3;//Min number of launches

    public static void app_launched(Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
        if (prefs.getBoolean("dontshowagain", false)) { return ; }

        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        long launch_count = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launch_count);
        Log.d("Rate Count: ", String.valueOf(launch_count));

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", date_firstLaunch);
        }

        // Wait at least n days before opening
        if (launch_count >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= date_firstLaunch +
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                showRateDialog(mContext, editor);
            }
        }

        editor.commit();
    }

    public static void showRateDialog(final Context mContext, final SharedPreferences.Editor editor) {
        AlertDialog dialog = new AlertDialog.Builder(mContext).setTitle("Rate CareTouch?")
                .setPositiveButton("Sure!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME)));
                        dialog.dismiss();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (editor != null) {
                            editor.putBoolean("dontshowagain", true);
                            editor.commit();
                        }
                        dialog.dismiss();
                    }
                }).setNeutralButton("Maybe Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setIcon(android.R.drawable.ic_dialog_info).setCancelable(false).setMessage("If you enjoy using CareTouch and would like to continue helping homeless people " +
                        "across the country, perhaps you could take a moment to rate our app on the Google Play Store?").create();

        dialog.show();
    }
}
