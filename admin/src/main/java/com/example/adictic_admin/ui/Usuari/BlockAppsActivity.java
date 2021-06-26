package com.example.adictic_admin.ui.Usuari;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adictic_admin.App;
import com.example.adictic_admin.R;
import com.example.adictic_admin.entity.BlockAppEntity;
import com.example.adictic_admin.rest.Api;
import com.example.adictic_admin.util.Funcions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BlockAppsActivity extends AppCompatActivity {
    private Api mTodoService;

    private Long idChild;
    private List<BlockAppEntity> blockAppList;

    private RV_Adapter RVadapter;
    private EditText ET_Search;
    private RecyclerView RV_appList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.block_app_layout);

        mTodoService = ((App) this.getApplication()).getAPI();
        idChild = getIntent().getLongExtra("idChild",-1);

        ET_Search = findViewById(R.id.ET_search);

        RV_appList = findViewById(R.id.RV_Apps);
        RV_appList.setLayoutManager(new LinearLayoutManager(this));

        setRecyclerView();
        setSearchBar();
    }

    private void setSearchBar() {
        ET_Search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });
    }

    private void filter(String s) {
        List<BlockAppEntity> filterList = new ArrayList<>();

        for (BlockAppEntity blockedApp : blockAppList) {
            CharSequence cat = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                cat = ApplicationInfo.getCategoryTitle(getApplicationContext(), blockedApp.appCategory);
                if (cat == null) {
                    cat = getResources().getString(R.string.other);
                }
            }
            if ((cat != null && cat.toString().toLowerCase().contains(s.toLowerCase())) || blockedApp.appName.toLowerCase().contains(s.toLowerCase())) {
                filterList.add(blockedApp);
            }
        }

        RVadapter.filterList(filterList);
    }

    private void setRecyclerView() {
        Call<Collection<BlockAppEntity>> call = mTodoService.getBlockApps(idChild);

        call.enqueue(new Callback<Collection<BlockAppEntity>>() {
            @Override
            public void onResponse(@NonNull Call<Collection<BlockAppEntity>> call, @NonNull Response<Collection<BlockAppEntity>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    blockAppList = new ArrayList<>(response.body());

                    blockAppList.removeIf(obj -> obj.pkgName.equals(getPackageName()));
                    Collections.sort(blockAppList);
                    RVadapter = new RV_Adapter(BlockAppsActivity.this, blockAppList);

                    RV_appList.setAdapter(RVadapter);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Collection<BlockAppEntity>> call, @NonNull Throwable t) { }
        });
    }

    public class RV_Adapter extends RecyclerView.Adapter<RV_Adapter.MyViewHolder> {
        List<BlockAppEntity> blockAppList;
        Context mContext;
        LayoutInflater mInflater;

        RV_Adapter(Context context, List<BlockAppEntity> list) {
            mContext = context;
            blockAppList = list;
            mInflater = LayoutInflater.from(mContext);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // infalte the item Layout
            View v = mInflater.inflate(R.layout.block_app_item, parent, false);


            // set the view's size, margins, paddings and layout parameters
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {

            final BlockAppEntity blockedApp = blockAppList.get(position);

            holder.pkgName = blockedApp.pkgName;

            Funcions.setIconDrawable(mContext, blockedApp.pkgName, holder.IV_appIcon);

            holder.TV_appName.setText(blockedApp.appName);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence cat = ApplicationInfo.getCategoryTitle(mContext, blockedApp.appCategory);
                if (cat == null) {
                    cat = getResources().getString(R.string.other);
                }
                holder.TV_category.setText(cat);
            } else {
                holder.TV_category.setVisibility(View.INVISIBLE);
            }

            if (blockedApp.appTime > 0) {
                Pair<Integer, Integer> pairTime = Funcions.millisToString(blockedApp.appTime);

                if(pairTime.first == 0)
                    holder.TV_appMaxTime.setText(getString(R.string.mins, pairTime.second));
                else if(pairTime.second == 0)
                    holder.TV_appMaxTime.setText(getString(R.string.hrs, pairTime.first));
                else
                    holder.TV_appMaxTime.setText(getString(R.string.hours_endl_minutes, pairTime.first, pairTime.second));

                holder.TV_appMaxTime.setVisibility(View.VISIBLE);
                holder.IV_block.setVisibility(View.GONE);
            } else if (blockedApp.appTime == 0) {
                holder.TV_appMaxTime.setVisibility(View.GONE);
                holder.IV_block.setVisibility(View.VISIBLE);
            } else {
                holder.IV_block.setVisibility(View.GONE);
                holder.TV_appMaxTime.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return blockAppList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        public void filterList(List<BlockAppEntity> fList) {
            blockAppList = fList;
            notifyDataSetChanged();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {
            protected View mRootView;
            String pkgName;
            ImageView IV_appIcon, IV_block;

            TextView TV_appName, TV_appMaxTime, TV_category;

            MyViewHolder(@NonNull View itemView) {
                super(itemView);

                mRootView = itemView;

                IV_appIcon = itemView.findViewById(R.id.IV_appIcon);
                IV_block = itemView.findViewById(R.id.IV_block);

                TV_appName = itemView.findViewById(R.id.TV_appName);
                TV_appMaxTime = itemView.findViewById(R.id.TV_appMaxTime);
                TV_category = itemView.findViewById(R.id.TV_Category);
            }
        }
    }
}
