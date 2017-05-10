package org.iilab.pb;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.iilab.pb.alert.PanicAlert;
import org.iilab.pb.data.PBDatabase;
import org.iilab.pb.fragment.AdvancedSettingsFragment;
import org.iilab.pb.fragment.AdvancedSettingsSubScreenFragment;
import org.iilab.pb.fragment.LanguageSettingsFragment;
import org.iilab.pb.fragment.MainSetupAlertFragment;
import org.iilab.pb.fragment.SetupCodeFragment;
import org.iilab.pb.fragment.SetupContactsFragment;
import org.iilab.pb.fragment.SetupMessageFragment;
import org.iilab.pb.fragment.SimpleFragment;
import org.iilab.pb.fragment.WarningFragment;
import org.iilab.pb.model.Page;
import org.iilab.pb.trigger.HardwareTriggerService;

import static org.iilab.pb.common.AppConstants.FROM_MAIN_ACTIVITY;
import static org.iilab.pb.common.AppConstants.IS_BACK_BUTTON_PRESSED;
import static org.iilab.pb.common.AppConstants.PAGE_COMPONENT_ADVANCED_SETTINGS;
import static org.iilab.pb.common.AppConstants.PAGE_COMPONENT_ALERT;
import static org.iilab.pb.common.AppConstants.PAGE_COMPONENT_CODE;
import static org.iilab.pb.common.AppConstants.PAGE_COMPONENT_CONTACTS;
import static org.iilab.pb.common.AppConstants.PAGE_COMPONENT_LANGUAGE;
import static org.iilab.pb.common.AppConstants.PAGE_COMPONENT_MESSAGE;
import static org.iilab.pb.common.AppConstants.PAGE_FROM_NOT_IMPLEMENTED;
import static org.iilab.pb.common.AppConstants.PAGE_HOME_NOT_CONFIGURED;
import static org.iilab.pb.common.AppConstants.PAGE_HOME_READY;
import static org.iilab.pb.common.AppConstants.PAGE_ID;
import static org.iilab.pb.common.AppConstants.PAGE_TYPE_MODAL;
import static org.iilab.pb.common.AppConstants.PAGE_TYPE_SIMPLE;
import static org.iilab.pb.common.AppConstants.PAGE_TYPE_WARNING;
import static org.iilab.pb.common.AppConstants.WIZARD_FLAG_HOME_NOT_CONFIGURED;
import static org.iilab.pb.common.ApplicationSettings.getSelectedLanguage;
import static org.iilab.pb.common.ApplicationSettings.isAlertActive;
import static org.iilab.pb.common.ApplicationSettings.setWizardState;

/**
 * Created by aoe on 2/15/14.
 */
public class MainActivity extends BaseFragmentActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    TextView tvToastMessage;

    Page currentPage;
    String pageId;
    String selectedLang;

    Boolean flagRiseFromPause = false;
    private static final String TAG = MainActivity.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.root_layout);

        tvToastMessage = (TextView) findViewById(R.id.tv_toast);

        try {
            pageId = getIntent().getExtras().getString(PAGE_ID);
        } catch (Exception e) {
            pageId = PAGE_HOME_NOT_CONFIGURED;
            e.printStackTrace();
        }
        selectedLang = getSelectedLanguage(this);

        Log.i(TAG, "pageId = " + pageId);

        if (pageId.equals(PAGE_HOME_NOT_CONFIGURED)) {
            Log.d(TAG, "Restarting the Wizard");

            if ((isAlertActive(this))) {
                new PanicAlert(this).deActivate();
            }

            setWizardState(MainActivity.this, WIZARD_FLAG_HOME_NOT_CONFIGURED);
            changeAppIcontoPB();

            // We're restarting the wizard so we deactivate the HardwareTriggerService
            stopService(new Intent(this, HardwareTriggerService.class));


            Intent i = new Intent(MainActivity.this, WizardActivity.class);
            i.putExtra(PAGE_ID, pageId);
            startActivity(i);

            callFinishActivityReceiver();

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("org.iilab.pb.RESTART_INSTALL");
            sendBroadcast(broadcastIntent);

            finish();
            return;
        }

        PBDatabase dbInstance = new PBDatabase(this);
        dbInstance.open();
        currentPage = dbInstance.retrievePage(pageId, selectedLang);
        dbInstance.close();

        if (currentPage == null) {
            Log.d(TAG, "page = null");
            Toast.makeText(this, "Still to be implemented.", Toast.LENGTH_SHORT).show();
            PAGE_FROM_NOT_IMPLEMENTED = true;
            finish();
            return;
        } else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            Fragment fragment = null;

            if (currentPage.getType().equals(PAGE_TYPE_SIMPLE)) {
                tvToastMessage.setVisibility(View.INVISIBLE);
                fragment = new SimpleFragment().newInstance(pageId, FROM_MAIN_ACTIVITY);
            } else if (currentPage.getType().equals(PAGE_TYPE_WARNING)) {
                tvToastMessage.setVisibility(View.INVISIBLE);
                fragment = new WarningFragment().newInstance(pageId, FROM_MAIN_ACTIVITY);

            } else if (currentPage.getType().equals(PAGE_TYPE_MODAL)) {
                tvToastMessage.setVisibility(View.INVISIBLE);
                Intent i = new Intent(MainActivity.this, MainModalActivity.class);
                i.putExtra(PAGE_ID, pageId);
                startActivity(i);
                finish();
                return;
            } else {
                if (currentPage.getComponent().equals(PAGE_COMPONENT_CONTACTS))
                    fragment = new SetupContactsFragment().newInstance(pageId, FROM_MAIN_ACTIVITY);
                else if (currentPage.getComponent().equals(PAGE_COMPONENT_MESSAGE))
                    fragment = new SetupMessageFragment().newInstance(pageId, FROM_MAIN_ACTIVITY);
                else if (currentPage.getComponent().equals(PAGE_COMPONENT_CODE))
                    fragment = new SetupCodeFragment().newInstance(pageId, FROM_MAIN_ACTIVITY);
                else if (currentPage.getComponent().equals(PAGE_COMPONENT_ALERT))
                    fragment = new MainSetupAlertFragment().newInstance(pageId, FROM_MAIN_ACTIVITY);
                else if (currentPage.getComponent().equals(PAGE_COMPONENT_LANGUAGE))
                    fragment = new LanguageSettingsFragment().newInstance(pageId, FROM_MAIN_ACTIVITY);
                else if (currentPage.getComponent().equals(PAGE_COMPONENT_ADVANCED_SETTINGS)) {
                    fragment = new AdvancedSettingsFragment().newInstance(pageId, FROM_MAIN_ACTIVITY);
                } else
                    fragment = new SimpleFragment().newInstance(pageId, FROM_MAIN_ACTIVITY);
            }
            fragmentTransaction.add(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }
    }


    private void changeAppIcontoPB() {
        Log.d(TAG, " changeAppIcontoPB");
        getPackageManager().setComponentEnabledSetting(
                new ComponentName("org.nanot.hi", "org.iilab.pb.HomeActivity-setup"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        getPackageManager().setComponentEnabledSetting(
                new ComponentName("org.nanot.hi", "org.iilab.pb.HomeActivity-calculator"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "pageId = " + pageId + " and flagRiseFromPause = " + flagRiseFromPause);


                /*
        Check-1
        if this page is resumed from the page still not implemented, then we'll handle it here.
        If we don't do this check, then the resume procedure falls under Check-3 & execute that code snippet, which is not proper.
         */
        if (PAGE_FROM_NOT_IMPLEMENTED) {
            Log.d(TAG, "returning from not-implemented page.");
            PAGE_FROM_NOT_IMPLEMENTED = false;
            return;
        }

        /*
        Check-2
        if this page is resumed by navigating-back from the next page, then we'll handle it here.
        If we don't do this check, then the resume procedure falls under Check-3 & execute that code snippet, which is not proper.
         */
        if (IS_BACK_BUTTON_PRESSED) {
            Log.d(TAG, "back button pressed");
            IS_BACK_BUTTON_PRESSED = false;
            return;
        }


        if (flagRiseFromPause) {
            Intent i = new Intent(MainActivity.this, CalculatorActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.show_from_bottom, R.anim.hide_to_top);

            callFinishActivityReceiver();

            finish();
            return;
        }
        return;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "flagRiseFromPause = " + true);
        flagRiseFromPause = true;
    }

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onStart() {
        super.onStart();
//        if(pageId.equals("home-not-configured")){
//            Log.e("??????????????", "home-not-configured");
//        	ApplicationSettings.setWizardState(MainActivity.this, WIZARD_FLAG_HOME_NOT_CONFIGURED);
//        	Intent i = new Intent(MainActivity.this, WizardActivity.class);
//        	i.putExtra("page_id", "home-not-configured");
//        	startActivity(i);
//        }
        Log.d(TAG, "onStart");
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "   fragment back pressed in " + currentPage.getId()+" "+pageId);
        if (pageId.equals(PAGE_HOME_READY)) {
//        if (pageId.equals(PAGE_SETUP_ALARM_RETRAINING)){
            // don't go back
//        	finish();
//        	startActivity(AppUtil.behaveAsHomeButton());
        } else if (currentPage.getComponent() != null && currentPage.getComponent().equals(PAGE_COMPONENT_ADVANCED_SETTINGS)) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            }
            else {
                super.onBackPressed();
                IS_BACK_BUTTON_PRESSED = true;
            }
        } else {
            super.onBackPressed();
            IS_BACK_BUTTON_PRESSED = true;
        }
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat,
                                           PreferenceScreen preferenceScreen) {
        Log.d(TAG, "callback called to attach the preference sub screen");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        AdvancedSettingsSubScreenFragment fragment = AdvancedSettingsSubScreenFragment.newInstance(pageId, FROM_MAIN_ACTIVITY);
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);
        ft.replace(R.id.fragment_container, fragment, preferenceScreen.getKey());
        ft.addToBackStack(null);
        ft.commit();
        return true;
    }


}
