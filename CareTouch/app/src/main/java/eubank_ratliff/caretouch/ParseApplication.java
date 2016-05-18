package eubank_ratliff.caretouch;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by admin on 11/30/15.
 */
public class ParseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Local Datastore.


        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "WJhKqWX8aop9S5VK6vXt6uhWTBgt8VdVUonbJEHL", "VBPEcp7XWlJgd2x5F4eXFd5oZ18Egkmajj8NjxUp");
    }
}