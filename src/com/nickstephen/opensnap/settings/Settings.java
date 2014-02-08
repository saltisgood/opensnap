package com.nickstephen.opensnap.settings;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.text.InputType;

import com.nickstephen.lib.misc.StatMethods;
import com.nickstephen.opensnap.R;

import org.holoeverywhere.preference.CheckBoxPreference;
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

			/* final CheckBoxPreference check = (CheckBoxPreference)this.findPreference(this.getString(R.string.pref_preview_key));
			final CheckBoxPreference check2 = (CheckBoxPreference)this.findPreference(this.getString(R.string.pref_time_key));
			final CheckBoxPreference check3 = (CheckBoxPreference)this.findPreference(this.getString(R.string.pref_markopen_key));
			final CheckBoxPreference check4 = (CheckBoxPreference)this.findPreference(this.getString(R.string.pref_addtolibrary_key));

			//boolean allowSaves = SettingsAccessor.getAllowSaves(this);

			if (mPremiumEnabled) {
				check.setEnabled(true);
				check2.setEnabled(true);
				check3.setEnabled(true);
				check4.setEnabled(true);
			} */

			//pref = (EditTextPreference)this.findPreference(this.getString(R.string.pref_allow_save_key));
            /* CheckBoxPreference savePref = (CheckBoxPreference)this.findPreference(this.getString(R.string.pref_allow_save_key));
			pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference arg0, Object arg1) {
					if (SettingsAccessor.getAllowSaves(Settings.this, (String)arg1)) {
						StatMethods.hotBread(Settings.this, "Authorisation successful!", Toast.LENGTH_LONG);
						check.setEnabled(true);
						check2.setEnabled(true);
						check3.setEnabled(true);
						check4.setEnabled(true);
					}
					else {
						StatMethods.hotBread(Settings.this, "Authorisation failed. Better luck next time!", Toast.LENGTH_LONG);
						check.setEnabled(false);
						check2.setEnabled(false);
						check3.setEnabled(false);
						check4.setEnabled(false);
					}
					return true;
				}
			}); */
			
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
			
			/* final android.preference.CheckBoxPreference check = (android.preference.CheckBoxPreference)this.findPreference(this.getString(R.string.pref_preview_key));
			final android.preference.CheckBoxPreference check2 = (android.preference.CheckBoxPreference)this.findPreference(this.getString(R.string.pref_time_key));
			final android.preference.CheckBoxPreference check3 = (android.preference.CheckBoxPreference)this.findPreference(this.getString(R.string.pref_markopen_key));
			final android.preference.CheckBoxPreference check4 = (android.preference.CheckBoxPreference)this.findPreference(this.getString(R.string.pref_addtolibrary_key));
			//boolean allowSaves = SettingsAccessor.getAllowSaves(this.getActivity());
			if (mPremiumEnabled) {
				check.setEnabled(true);
				check2.setEnabled(true);
				check3.setEnabled(true);
				check4.setEnabled(true);
			} */
			
			/* pref = (android.preference.EditTextPreference)this.findPreference(this.getString(R.string.pref_allow_save_key));
			pref.setOnPreferenceChangeListener(new android.preference.Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(android.preference.Preference arg0, Object arg1) {
					if (SettingsAccessor.getAllowSaves(SettingsFrag.this.getActivity(), (String)arg1)) {
						StatMethods.hotBread(SettingsFrag.this.getActivity(), "Authorisation successful!", Toast.LENGTH_LONG);
						check.setEnabled(true);
						check2.setEnabled(true);
						check3.setEnabled(true);
						check4.setEnabled(true);
					}
					else {
						StatMethods.hotBread(SettingsFrag.this.getActivity(), "Authorisation failed. Better luck next time!", Toast.LENGTH_LONG);
						check.setEnabled(false);
						check2.setEnabled(false);
						check3.setEnabled(false);
						check4.setEnabled(false);
					}
					return true;
				}
			}); */
			
			android.preference.ListPreference theme = (android.preference.ListPreference)this.findPreference(this.getString(R.string.pref_theme_key));
			theme.setOnPreferenceChangeListener(new android.preference.Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(android.preference.Preference preference, Object newValue) {
					SettingsFrag.this.getActivity().setResult(RESULT_RESTART_ACTIVITY);
					
					return true;
				}
			});
		}
	}
}
