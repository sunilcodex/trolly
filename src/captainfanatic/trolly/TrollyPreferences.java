package captainfanatic.trolly;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class TrollyPreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
