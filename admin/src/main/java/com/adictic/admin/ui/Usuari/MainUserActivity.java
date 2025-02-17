package com.adictic.admin.ui.Usuari;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.adictic.admin.R;
import com.adictic.admin.rest.AdminApi;
import com.adictic.admin.util.AdminApp;
import com.adictic.common.entity.FillNom;
import com.adictic.common.ui.main.MainActivityAbstractClass;
import com.adictic.common.ui.main.TabFillsAdapter;
import com.adictic.common.util.Callback;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Collection;

import retrofit2.Call;
import retrofit2.Response;

public class MainUserActivity extends MainActivityAbstractClass {

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_parent_fragment);

        ViewPager2 viewPager = findViewById(R.id.VP_home_parent_pager);
        TabLayout tabLayout = findViewById(R.id.TL_home_parent_tabs);

        AdminApi mTodoService = ((AdminApp) getApplicationContext()).getAPI();

        long idTutor = getIntent().getLongExtra("idTutor", -1);
        long idChild = getIntent().getLongExtra("idChild", -1);

        Call<Collection<FillNom>> call;
        if(idChild != -1)
            call = mTodoService.getChildInfo(idTutor, idChild);
        else
            call = mTodoService.getUserChilds(idTutor);

        call.enqueue(new Callback<Collection<FillNom>>() {
            @Override
            public void onResponse(@NonNull Call<Collection<FillNom>> call, @NonNull Response<Collection<FillNom>> response) {
                    super.onResponse(call, response);
                if (response.isSuccessful() && response.body() != null && response.body().size() > 0) {
                    findViewById(R.id.TV_noFills).setVisibility(View.GONE);
                    ArrayList<FillNom> fills = new ArrayList<>(response.body());

                    TabFillsAdapter adapter = new TabFillsAdapter(getSupportFragmentManager(), getLifecycle(), fills);

                    viewPager.setAdapter(adapter);

                    new TabLayoutMediator(tabLayout, viewPager,
                            (tab, position) -> tab.setText(adapter.getPageTitle(position))
                    ).attach();
                } else {
                    findViewById(R.id.TV_noFills).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Collection<FillNom>> call, @NonNull Throwable t) {
                    super.onFailure(call, t);
                findViewById(R.id.TV_noFills).setVisibility(View.VISIBLE);
            }
        });
    }
}
