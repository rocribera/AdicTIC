package com.adictic.admin.ui.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.adictic.admin.MainActivity;
import com.adictic.admin.R;
import com.adictic.admin.rest.AdminApi;
import com.adictic.admin.util.AdminApp;
import com.adictic.admin.util.Funcions;
import com.adictic.common.entity.AdminProfile;
import com.adictic.common.util.Callback;
import com.adictic.common.util.Constants;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import retrofit2.Call;
import retrofit2.Response;

public class ProfileMainFragment extends Fragment {
    private AdminApi mService;
    private SharedPreferences sharedPreferences;
    private MainActivity parentActivity = null;
    private View root;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedPreferences = Funcions.getEncryptedSharedPreferences(getContext());
        parentActivity = (MainActivity) getActivity();
        root = inflater.inflate(R.layout.profile_main_activity, container, false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //super.onViewCreated(view, savedInstanceState);
        mService = ((AdminApp) requireActivity().getApplication()).getAPI();
        root = view;
        getInfoFromServer();
    }

    private void getInfoFromServer() {
        if(parentActivity!=null && parentActivity.yourAdminProfile!=null){
            showProfileInfo(parentActivity.yourAdminProfile);
            return;
        }
        Call<AdminProfile> call = mService.getProfile(sharedPreferences.getLong(Constants.SHARED_PREFS_ID_ADMIN,-1));
        call.enqueue(new Callback<AdminProfile>() {
            @Override
            public void onResponse(Call<AdminProfile> call, Response<AdminProfile> response) {
                    super.onResponse(call, response);
                if(response.isSuccessful() && response.body() != null){
                    parentActivity.yourAdminProfile = response.body();
                    showProfileInfo(response.body());
                }
            }

            @Override
            public void onFailure(Call<AdminProfile> call, Throwable t) {

            }
        });
    }

    private void showProfileInfo(AdminProfile profile){
        ViewPager2 viewPager = (ViewPager2) root.findViewById(R.id.VP_home_parent_pager);
        TabLayout tabLayout = (TabLayout) root.findViewById(R.id.TL_home_parent_tabs);

        TabProfileAdapter adapter = new TabProfileAdapter(getContext(),ProfileMainFragment.this, profile);

        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout,viewPager,
                (tab, position) -> tab.setText(adapter.getPageTitle(position))
        ).attach();
    }
}
