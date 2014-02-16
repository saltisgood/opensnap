package com.nickstephen.opensnap.settings;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.text.InputType;

import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;

import org.holoeverywhere.preference.EditTextPreference;
import org.holoeverywhere.preference.ListPreference;
import org.holoeverywhere.preference.Preference;
import org.holoeverywhere.preference.Preference.OnPreferenceChangeListener;
import org.holoeverywhere.preference.PreferenceActivity;
import org.holoeverywhere.preference.PreferenceScreen;
import org.holoeverywhere.widget.Toast;

/**
 * The Settings activity. Plain and simple
 * @author Nick Stephen (a.k.a. saltisgood)
 */
public class Settings extends PreferenceActivity {
	public static final int REQUEST_RESULT_CODE = 634;
	public static final int RESULT_RESTART_ACTIVITY = 512;
    public static final int RESULT_PURCHASE_PREMIUM = 763;

    private boolean mPremiumEnabled;
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			this.getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFrag()).commit();
		}
		else {
			this.addPreferencesFromResource(R.xml.preferences); 
			
			EditTextPreference pref = (EditTextPreference)this.findPreference(this.getString(R.string.pref_snapstokeep_key));
			pref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
            pref.setOnPreferenceChangeListener(mIntPrefCheckL);

            mPremiumEnabled = SettingsAccessor.getPremium(this);

            PreferenceScreen premium = (PreferenceScreen)this.findPreference(this.getString(R.string.pref_cat_snap_special_key));
            premium.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (mPremiumEnabled) {
                        return false;
                    } else {
                        Settings.this.setResult(RESULT_PURCHASE_PREMIUM);
                        Settings.this.finish();
                        return true;
                    }
                }
            });
			
			ListPreference theme = (ListPreference)this.findPreference(this.getString(R.string.pref_theme_key));
			theme.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					Settings.this.setResult(RESULT_RESTART_ACTIVITY);
					
					return true;
				}
			});
		}
	}

    private final OnPreferenceChangeListener mIntPrefCheckL = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (newValue instanceof String) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Integer.valueOf((String) newValue);
                    return true;
                } catch (NumberFormatException e) {
                    // ignore and pass control to final return
                }
            }
            StatMethods.hotBread(Settings.this, "Please enter a valid integer number", Toast.LENGTH_SHORT);
            return false;
        }
    };
	
	@SuppressLint("NewApi")
	public static class SettingsFrag extends PreferenceFragment {
        private boolean mPremiumEnabled;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			//this.addPreferencesFromResource(R.xml.preferences);
            this.addPreferencesFromResource(R.xml.preferences_honeycomb);
			android.preference.EditTextPreference pref = (android.preference.EditTextPreference)this.findPreference(this.getString(R.string.pref_snapstokeep_key));
			pref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
            pref.setOnPreferenceChangeListener(mIntPrefCheckL);

            mPremiumEnabled = SettingsAccessor.getPremium(this.getActivity());

            android.preference.PreferenceScreen premium = (android.preference.PreferenceScreen)this.findPreference(this.getString(R.string.pref_cat_snap_special_key));
            premium.setOnPreferenceClickListener(new android.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(android.preference.Preference preference) {
                    if (mPremiumEnabled) {
                        return false;
                    } else {
                        SettingsFrag.this.getActivity().setResult(RESULT_PURCHASE_PREMIUM);
                        SettingsFrag.this.getActivity().finish();
                        return true;
                    }
                }
            });
			
			android.preference.ListPreference theme = (android.preference.ListPreference)this.findPreference(this.getString(R.string.pref_theme_key));
			theme.setOnPreferenceChangeListener(new android.preference.Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(android.preference.Preference preference, Object newValue) {
					SettingsFrag.this.getActivity().setResult(RESULT_RESTART_ACTIVITY);
					
					return true;
				}
			});
		}

        private final android.preference.Preference.OnPreferenceChangeListener mIntPrefCheckL = new android.preference.Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(android.preference.Preference preference, Object newValue) {
                if (newValue instanceof String) {
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        Integer.valueOf((String) newValue);
                        return true;
                    } catch (NumberFormatException e) {
                        // ignore and pass control to final return
                    }
                }
                StatMethods.hotBread(SettingsFrag.this.getActivity(), "Please enter a valid integer number", Toast.LENGTH_SHORT);
                return false;
            }
        };
	}
}
