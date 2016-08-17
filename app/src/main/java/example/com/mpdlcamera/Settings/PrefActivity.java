package example.com.mpdlcamera.Settings;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import example.com.mpdlcamera.R;

/**
 * Created by kiran on 22.10.15.
 */
public class PrefActivity extends AppCompatActivity{

 //   private View rootView;
  // Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.backup_setting_preference);

     /* rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
*/

        PrefFragment prefFragment = new PrefFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, prefFragment);
        fragmentTransaction.commit();
    }

    //Fragment class which is used to set the preference values for backup settings
    public static class PrefFragment extends PreferenceFragment {

        private SwitchPreference switchPreferenceLau;
        private SwitchPreference switchPreferenceRpfd;
        private ListPreference backupPreference;
        SharedPreferences mPrefs;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            addPreferencesFromResource(R.xml.preference);
            Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

            ((AppCompatActivity) getActivity()).getSupportActionBar().setHomeButtonEnabled(true);
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            PreferenceManager preferenceManager = getPreferenceManager();

            switchPreferenceLau = (SwitchPreference) findPreference("LogoutAfterUpload");
            switchPreferenceRpfd = (SwitchPreference) findPreference("RemovePhotosAfterUpload");
            backupPreference = (ListPreference) findPreference("status");

            //set the summary value depending on the preference values
            if(mPrefs.contains("status")) {
                backupPreference.setSummary(mPrefs.getString("status",""));
            }
            else
                backupPreference.setSummary("wifi");


            if(backupPreference != null) {

                backupPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        String value = (String) newValue;

                        backupPreference.setSummary(value);
                        return  true;
                    }
                });

            }

            //Logout after uploading
            if(switchPreferenceLau != null) {

                switchPreferenceLau.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Boolean statusLau = ((Boolean) newValue).booleanValue();

                        SharedPreferences.Editor e = mPrefs.edit();
                        e.putBoolean("LogoutAfterUpload", statusLau);
                        e.commit();
                        return  true;
                    }
                });

            }

            //Remove the Photos after uploading
            if(switchPreferenceRpfd != null) {

                switchPreferenceRpfd.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        Boolean statusRemove = ((Boolean) newValue).booleanValue();

                        SharedPreferences.Editor e = mPrefs.edit();
                        e.putBoolean("RemovePhotosAfterUpload", statusRemove);
                        e.commit();
                        return true;
                    }
                });
            }



        }
    }

    /*
        method for the selection of options
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

}


