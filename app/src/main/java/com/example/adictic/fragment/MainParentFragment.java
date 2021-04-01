package com.example.adictic.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.adictic.R;
import com.example.adictic.TodoApp;
import com.example.adictic.activity.BlockAppsActivity;
import com.example.adictic.activity.DayUsageActivity;
import com.example.adictic.activity.GeoLocActivity;
import com.example.adictic.activity.HorarisActivity;
import com.example.adictic.activity.HorarisMainActivity;
import com.example.adictic.activity.informe.InformeActivity;
import com.example.adictic.entity.AppUsage;
import com.example.adictic.entity.FillNom;
import com.example.adictic.entity.GeneralUsage;
import com.example.adictic.rest.TodoApi;
import com.example.adictic.util.Funcions;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainParentFragment extends Fragment {

    private TodoApi mTodoService;
    private long idChildSelected = -1;
    private final FillNom fillNom;
    private View root;

    private ImageView IV_liveIcon;

    private PieChart pieChart;

    public MainParentFragment(FillNom fill){
        idChildSelected = fill.idChild;
        fillNom = fill;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.main_parent, container, false);
        mTodoService = ((TodoApp) getActivity().getApplication()).getAPI();

        IV_liveIcon = (ImageView) root.findViewById(R.id.IV_CurrentApp);

        View.OnClickListener blockApps = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), BlockAppsActivity.class);
                i.putExtra("idChild",idChildSelected);
                getActivity().startActivity(i);
            }
        };

        Button BT_BlockApps = (Button) root.findViewById(R.id.BT_ConsultaPrivada);
        BT_BlockApps.setOnClickListener(blockApps);

        View.OnClickListener informe = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), InformeActivity.class);
                i.putExtra("idChild",idChildSelected);
                getActivity().startActivity(i);
            }
        };

        Button BT_Informe = (Button) root.findViewById(R.id.BT_ContingutInformatiu);
        BT_Informe.setOnClickListener(informe);

        View.OnClickListener appUsage = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), DayUsageActivity.class);
                i.putExtra("idChild",idChildSelected);
                getActivity().startActivity(i);
            }
        };

        Button BT_appUse = (Button) root.findViewById(R.id.BT_faqs);
        BT_appUse.setOnClickListener(appUsage);

        View.OnClickListener horaris = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), HorarisMainActivity.class);
                i.putExtra("idChild",idChildSelected);
                getActivity().startActivity(i);
            }
        };

        Button BT_horaris = (Button) root.findViewById(R.id.BT_oficines);
        BT_horaris.setOnClickListener(horaris);

        View.OnClickListener geoloc = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), GeoLocActivity.class);
                i.putExtra("idChild",idChildSelected);
                getActivity().startActivity(i);
            }
        };

        ConstraintLayout CL_Geoloc = (ConstraintLayout) root.findViewById(R.id.CL_geoloc);
        CL_Geoloc.setOnClickListener(geoloc);


        LocalBroadcastManager.getInstance(root.getContext()).registerReceiver(messageReceiver,
                new IntentFilter("liveApp"));

        Button blockButton = (Button) root.findViewById(R.id.BT_BlockDevice);
        blockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button b = v.findViewById(R.id.BT_BlockDevice);
                Call<String> call = null;
                if(b.getText().equals(getString(R.string.block_device))){
                    call = mTodoService.blockChild(idChildSelected);
                }
                else call = mTodoService.unblockChild(idChildSelected);
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful()) {
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                    }
                });
                if(b.getText().equals(getString(R.string.block_device))) b.setText(getString(R.string.unblock_device));
                else b.setText(getString(R.string.block_device));
            }
        });

        Button nitButton = (Button) root.findViewById(R.id.BT_nits);
        nitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), HorarisActivity.class);
                i.putExtra("idChild",idChildSelected);
                getActivity().startActivity(i);
            }
        });

        Button BT_FreeTime = (Button) root.findViewById(R.id.BT_FreeTime);
        BT_FreeTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Call<String> call = null;
                if(BT_FreeTime.getText().equals(getString(R.string.free_time))){
                    call = mTodoService.blockChild(idChildSelected);
                }
                else call = mTodoService.unblockChild(idChildSelected);
                call.enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        if (response.isSuccessful()) {
                        }
                    }

                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                    }
                });
                if(BT_FreeTime.getText().equals(getString(R.string.free_time))) BT_FreeTime.setText(getString(R.string.stop_free_time));
                else BT_FreeTime.setText(getString(R.string.free_time));
            }
        });

        ConstraintLayout CL_info = (ConstraintLayout) root.findViewById(R.id.CL_info);
        ConstraintLayout CL_infoButtons = (ConstraintLayout) root.findViewById(R.id.CL_infoButtons);
        CL_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(CL_infoButtons.getVisibility()==View.GONE){
                    CL_infoButtons.setVisibility(View.VISIBLE);

                    ImageView IV_openInfo = (ImageView) root.findViewById(R.id.IV_openInfo);
                    IV_openInfo.setImageResource(R.drawable.ic_arrow_close);
                }
                else{
                    CL_infoButtons.setVisibility(View.GONE);

                    ImageView IV_openInfo = (ImageView) root.findViewById(R.id.IV_openInfo);
                    IV_openInfo.setImageResource(R.drawable.ic_arrow_open);
                }
            }
        });
        /** Posar icona de desplegar en la posició correcta **/
        if(CL_infoButtons.getVisibility()==View.GONE){
            ImageView IV_openInfo = (ImageView) root.findViewById(R.id.IV_openInfo);
            IV_openInfo.setImageResource(R.drawable.ic_arrow_open);
        }
        else{
            ImageView IV_openInfo = (ImageView) root.findViewById(R.id.IV_openInfo);
            IV_openInfo.setImageResource(R.drawable.ic_arrow_close);
        }

        ConstraintLayout CL_limit = (ConstraintLayout) root.findViewById(R.id.CL_suport);
        ConstraintLayout CL_limitButtons = (ConstraintLayout) root.findViewById(R.id.CL_suportButtons);
        CL_limit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(CL_limitButtons.getVisibility()==View.GONE){
                    CL_limitButtons.setVisibility(View.VISIBLE);

                    ImageView IV_openLimit = (ImageView) root.findViewById(R.id.IV_openSuport);
                    IV_openLimit.setImageResource(R.drawable.ic_arrow_close);
                }
                else{
                    CL_limitButtons.setVisibility(View.GONE);

                    ImageView IV_openLimit = (ImageView) root.findViewById(R.id.IV_openSuport);
                    IV_openLimit.setImageResource(R.drawable.ic_arrow_open);
                }
            }
        });
        /** Posar icona de desplegar en la posició correcta **/
        if(CL_limitButtons.getVisibility()==View.GONE){
            ImageView IV_openLimit = (ImageView) root.findViewById(R.id.IV_openSuport);
            IV_openLimit.setImageResource(R.drawable.ic_arrow_open);
        }
        else{
            ImageView IV_openLimit = (ImageView) root.findViewById(R.id.IV_openSuport);
            IV_openLimit.setImageResource(R.drawable.ic_arrow_close);
        }

        getStats();

        return root;
    }

    private void getStats(){
        String dataAvui = Funcions.date2String(Calendar.getInstance().get(Calendar.DAY_OF_MONTH),Calendar.getInstance().get(Calendar.MONTH),Calendar.getInstance().get(Calendar.YEAR));
        Call<Collection<GeneralUsage>> call = mTodoService.getGenericAppUsage(idChildSelected,dataAvui,dataAvui);
        call.enqueue(new Callback<Collection<GeneralUsage>>() {
            @Override
            public void onResponse(Call<Collection<GeneralUsage>> call, Response<Collection<GeneralUsage>> response) {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    makeGraph(response.body());
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.error_noData), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Collection<GeneralUsage>> call, Throwable t) {
                Toast.makeText(getActivity().getApplicationContext(), getString(R.string.error_noData), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void makeGraph(Collection<GeneralUsage> genericAppUsage){
        pieChart = (PieChart) root.findViewById(R.id.Ch_Pie);
        long totalUsageTime = 0;

        Map<String,Long> mapUsage = new HashMap<>();
        List<BarEntry> barEntries = new ArrayList<>();

        for(GeneralUsage gu : genericAppUsage){
            if(gu.totalTime > 0){
                totalUsageTime+=gu.totalTime;
                for(AppUsage au: gu.usage){
                    if(mapUsage.containsKey(au.app.appName)) mapUsage.put(au.app.appName,mapUsage.get(au.app.appName)+au.totalTime);
                    else mapUsage.put(au.app.appName,au.totalTime);
                }
                barEntries.add(new BarEntry( gu.day+(gu.month*100),gu.totalTime/(float)3600000));
            }
        }

        setPieChart(mapUsage,totalUsageTime);
    }

    private void setPieChart(Map<String,Long> mapUsage, long totalUsageTime){
        ArrayList<PieEntry> yValues = new ArrayList<>();
        long others = 0;
        for(Map.Entry<String,Long> entry : mapUsage.entrySet()){
            if(entry.getValue() >= totalUsageTime*0.05) yValues.add(new PieEntry(entry.getValue(),entry.getKey()));
            else{
                others+=entry.getValue();
            }
        }

        yValues.add(new PieEntry(others,"Altres"));

        PieDataSet pieDataSet = new PieDataSet(yValues, "Ús d'apps");
        pieDataSet.setSliceSpace(3f);
        pieDataSet.setSelectionShift(5f);
        pieDataSet.setColors(TodoApp.GRAPH_COLORS);

        PieData pieData = new PieData(pieDataSet);
        pieData.setValueFormatter(new PercentFormatter());
        pieData.setValueTextSize(10);

        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setCenterTextSize(25);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);

        pieChart.animateY(1000);

        pieChart.setDrawEntryLabels(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                final PieEntry pe = (PieEntry) e;

                TextView TV_pieApp = (TextView) root.findViewById(R.id.TV_PieApp);
                TV_pieApp.setText(pe.getLabel());


                Pair<Integer,Integer> appTime = Funcions.millisToString(e.getY());

                if(appTime.first == 0) pieChart.setCenterText(getResources().getString(R.string.mins,appTime.second));
                else pieChart.setCenterText(getResources().getString(R.string.hours_endl_minutes,appTime.first,appTime.second));
            }

            @Override
            public void onNothingSelected() {
                TextView TV_pieApp = (TextView) root.findViewById(R.id.TV_PieApp);
                TV_pieApp.setText(getResources().getString(R.string.press_pie_chart));
                pieChart.setCenterText("");
            }
        });
        pieChart.invalidate();
    }

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            TextView currentApp = root.findViewById(R.id.TV_CurrentApp);

            String pkgName = intent.getStringExtra("pkgName");

            Funcions.setIconDrawable(getContext(),pkgName,IV_liveIcon);

            currentApp.setText(intent.getStringExtra("appName"));
        }
    };

    @Override
    protected void finalize() throws Throwable {
        askChildForLiveApp(idChildSelected, false);
        super.finalize();
    }

    private void askChildForLiveApp(long idChild, boolean liveApp){
        Call<String> call = mTodoService.askChildForLiveApp(idChild, liveApp);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (!response.isSuccessful()){
                    Toast toast = Toast.makeText(getActivity(), getString(R.string.error_liveApp), Toast.LENGTH_LONG);
                    toast.show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast toast = Toast.makeText(getActivity(), getString(R.string.error_liveApp), Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }
}
