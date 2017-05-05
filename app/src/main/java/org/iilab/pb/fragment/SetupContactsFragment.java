package org.iilab.pb.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.iilab.pb.MainActivity;
import org.iilab.pb.R;
import org.iilab.pb.WizardActivity;
import org.iilab.pb.adapter.PageItemAdapter;
import org.iilab.pb.common.AppConstants;
import org.iilab.pb.common.AppUtil;
import org.iilab.pb.common.ApplicationSettings;
import org.iilab.pb.common.ContactEditTexts;
import org.iilab.pb.common.ContactPickerFragment;
import org.iilab.pb.common.MyTagHandler;
import org.iilab.pb.data.PBDatabase;
import org.iilab.pb.model.Page;
import org.iilab.pb.model.PageItem;
import org.iilab.pb.model.SMSSettings;

import java.util.Arrays;
import java.util.List;

/**
 * Created by aoe on 12/11/13.
 */
public class SetupContactsFragment extends Fragment {
    private ContactEditTexts contactEditTexts;

    private static final String PAGE_ID = "page_id";
    private static final String PARENT_ACTIVITY = "parent_activity";
    private Activity activity;
    DisplayMetrics metrics;
    TextView tvTitle, tvContent, tvIntro, tvWarning;
    Button bAction;
    ListView lvItems;
    LinearLayout llWarning;
    Page currentPage;
    PageItemAdapter pageItemAdapter;
    List<Integer> fragmentIds = Arrays.asList(R.id.first_contact, R.id.second_contact, R.id.third_contact);
    CheckBox warnSecGuards;

    public static SetupContactsFragment newInstance(String pageId, int parentActivity) {
        SetupContactsFragment f = new SetupContactsFragment();
        Bundle args = new Bundle();
        args.putString(PAGE_ID, pageId);
        args.putInt(PARENT_ACTIVITY, parentActivity);
        f.setArguments(args);
        return(f);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_type_interactive_contacts, container, false);

        tvTitle = (TextView) view.findViewById(R.id.fragment_title);
        tvIntro = (TextView) view.findViewById(R.id.fragment_intro);
        tvContent = (TextView) view.findViewById(R.id.fragment_contents);
        warnSecGuards = (CheckBox) view.findViewById(R.id.checkbox_warn_sec_guards);
        warnSecGuards.setVisibility(View.VISIBLE);

        bAction = (Button) view.findViewById(R.id.fragment_action);
        ContactPickerFragment contactPickerFragment=(ContactPickerFragment)getChildFragmentManager().findFragmentById(R.id.first_contact);
        if(contactPickerFragment==null) {
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            for (Integer id : fragmentIds) {
                contactPickerFragment = new ContactPickerFragment();
                transaction.replace(id, contactPickerFragment);
            }
            transaction.commit();
        }
        bAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(">>>>", "action button pressed");
                SMSSettings newSMSSettings = getContactNumbersFromView();

                SMSSettings.saveContacts(activity, newSMSSettings);
                displayContactNumbers();

                String pageId = currentPage.getAction().get(0).getLink();
                int parentActivity = getArguments().getInt(PARENT_ACTIVITY);
                Intent i;
                if(parentActivity == AppConstants.FROM_WIZARD_ACTIVITY){
                    i = new Intent(activity, WizardActivity.class);
                } else{
                    String confirmation = (currentPage.getAction().get(0).getConfirmation() == null)
                            ? AppConstants.DEFAULT_CONFIRMATION_MESSAGE
                            : currentPage.getAction().get(0).getConfirmation();
                    Toast.makeText(activity, confirmation, Toast.LENGTH_SHORT).show();

                    i = new Intent(activity, MainActivity.class);
                }
                i.putExtra(PAGE_ID, pageId);
                startActivity(i);

                if(parentActivity == AppConstants.FROM_MAIN_ACTIVITY){
                    activity.finish();
                }
            }
        });


        lvItems = (ListView) view.findViewById(R.id.fragment_item_list);

        llWarning = (LinearLayout) view.findViewById(R.id.ll_fragment_warning);
        tvWarning  = (TextView) view.findViewById(R.id.fragment_warning);

        lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PageItem selectedItem = (PageItem) parent.getItemAtPosition(position);

                String pageId = selectedItem.getLink();
                int parentActivity = getArguments().getInt(PARENT_ACTIVITY);
                Intent i;

                if (parentActivity == AppConstants.FROM_WIZARD_ACTIVITY) {
                    i = new Intent(activity, WizardActivity.class);
                } else {
                    i = new Intent(activity, MainActivity.class);
                }
                i.putExtra(PAGE_ID, pageId);
                startActivity(i);

            }
        });
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        activity = getActivity();
        if (activity != null) {
            metrics = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            String pageId = getArguments().getString(PAGE_ID);
            String selectedLang = ApplicationSettings.getSelectedLanguage(activity);

            PBDatabase dbInstance = new PBDatabase(activity);
            dbInstance.open();
            currentPage = dbInstance.retrievePage(pageId, selectedLang);
            dbInstance.close();

            tvTitle.setText(currentPage.getTitle());

            if(currentPage.getContent() == null)
                tvContent.setVisibility(View.GONE);
            else
                tvContent.setText(Html.fromHtml(currentPage.getContent(), null, new MyTagHandler()));

            if(currentPage.getIntroduction() == null)
                tvIntro.setVisibility(View.GONE);
            else
                tvIntro.setText(currentPage.getIntroduction());

            if(currentPage.getWarning() == null)
                llWarning.setVisibility(View.GONE);
            else
                tvWarning.setText(currentPage.getWarning());

            bAction.setText(currentPage.getAction().get(0).getTitle());

            pageItemAdapter = new PageItemAdapter(activity, null);
            lvItems.setAdapter(pageItemAdapter);
            pageItemAdapter.setData(currentPage.getItems());

            AppUtil.updateImages(true, currentPage.getContent(), activity, metrics, tvContent, AppConstants.IMAGE_INLINE);
        }
    }

    public void initializeContactNumbers(){
        contactEditTexts = new ContactEditTexts(getChildFragmentManager(), bAction, getActivity());
        SMSSettings currentSettings = SMSSettings.retrieve(activity);
        if(currentSettings.isConfigured()) {
            displayContactNumbers();
        }
        bAction.setEnabled(contactEditTexts.hasAtleastOneValidPhoneNumber());
    }
    private void displayContactNumbers() {
        contactEditTexts.maskPhoneNumbers();
    }


    private SMSSettings getContactNumbersFromView() {
        List<String> phoneNumbers = contactEditTexts.getPhoneNumbers();
        // TODO: add static tel number here! --security guard's number
        if (warnSecGuards.isChecked()){
            phoneNumbers.add(getString(R.string.security_guard_tel_no));
        }
        Log.d("setupContacts", phoneNumbers.toString());
        return new SMSSettings(phoneNumbers);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        List<Fragment> fragments = getChildFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onActivityResult(requestCode, resultCode, intent);
            }
        }
    }

}
