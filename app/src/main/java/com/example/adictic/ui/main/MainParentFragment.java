package com.example.adictic.ui.main;

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
import com.example.adictic.entity.AppUsage;
import com.example.adictic.entity.FillNom;
import com.example.adictic.entity.GeneralUsage;
import com.example.adictic.rest.TodoApi;
import com.example.adictic.ui.BlockAppsActivity;
import com.example.adictic.ui.DayUsageActivity;
import com.example.adictic.ui.GeoLocActivity;
import com.example.adictic.ui.HorarisActivity;
import com.example.adictic.ui.events.EventsActivity;
import com.example.adictic.ui.informe.InformeActivity;
import com.example.adictic.util.Funcions;
import com.example.adictic.util.TodoApp;
import com.github.mikephil.charting.charts.PieChart;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainParentFragment extends Fragment {

    private final FillNom fillNom;
    private TodoApi mTodoService;
    private long idChildSelected = -1;
    private View root;

    private ImageView IV_liveIcon;
    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            TextView currentApp = root.findViewById(R.id.TV_CurrentApp);

            String pkgName = intent.getStringExtra("pkgName");

            Funcions.setIconDrawable(getContext(), pkgName, IV_liveIcon);

            currentApp.setText(intent.getStringExtra("appName"));
        }
    };
    private PieChart pieChart;

    public MainParentFragment(FillNom fill) {
        idChildSelected = fill.idChild;
        fillNom = fill;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.main_parent, container, false);
        mTodoService = ((TodoApp) getActivity().getApplication()).getAPI();

        IV_liveIcon = (ImageView) root.findViewById(R.id.IV_CurrentApp);

        setButtons();
        getStats();

        return root;
    }

    private void setButtons() {
        View.OnClickListener blockApps = v -> {
            Intent i = new Intent(getActivity(), BlockAppsActivity.class);
            i.putExtra("idChild", idChildSelected);
            startActivity(i);
        };

        Button BT_BlockApps = (Button) root.findViewById(R.id.BT_ConsultaPrivada);
        BT_BlockApps.setOnClickListener(blockApps);

        View.OnClickListener informe = v -> {
            Intent i = new Intent(getActivity(), InformeActivity.class);
            i.putExtra("idChild", idChildSelected);
            startActivity(i);
        };

        Button BT_Informe = (Button) root.findViewById(R.id.BT_ContingutInformatiu);
        BT_Informe.setOnClickListener(informe);

        View.OnClickListener appUsage = v -> {
            Intent i = new Intent(getActivity(), DayUsageActivity.class);
            i.putExtra("idChild", idChildSelected);
            startActivity(i);
        };

        Button BT_appUse = (Button) root.findViewById(R.id.BT_faqs);
        BT_appUse.setOnClickListener(appUsage);

        View.OnClickListener horaris = v -> {
            Intent i = new Intent(getActivity(), EventsActivity.class);
            i.putExtra("idChild", idChildSelected);
            startActivity(i);
        };

        Button BT_horaris = (Button) root.findViewById(R.id.BT_oficines);
        BT_horaris.setOnClickListener(horaris);

        View.OnClickListener geoloc = v -> {
            Intent i = new Intent(getActivity(), GeoLocActivity.class);
            i.putExtra("idChild", idChildSelected);
            startActivity(i);
        };

        ConstraintLayout CL_Geoloc = (ConstraintLayout) root.findViewById(R.id.CL_geoloc);
        CL_Geoloc.setOnClickListener(geoloc);


        if (TodoApp.getTutor() == 1) {
            LocalBroadcastManager.getInstance(root.getContext()).registerReceiver(messageReceiver,
                    new IntentFilter("liveApp"));
        } else {
            TextView currentApp = root.findViewById(R.id.TV_CurrentApp);
            currentApp.setText(getString(R.string.title_activity_splash_screen));
            String pkgName = getActivity().getApplicationContext().getPackageName();
            Funcions.setIconDrawable(getContext(), pkgName, IV_liveIcon);
        }

        Button blockButton = (Button) root.findViewById(R.id.BT_BlockDevice);
        blockButton.setVisibility(View.GONE);

        if (TodoApp.getTutor() == 1) {
            blockButton.setVisibility(View.VISIBLE);
            blockButton.setOnClickListener(v -> {
                Button b = v.findViewById(R.id.BT_BlockDevice);
                Call<String> call;
                if (b.getText().equals(getString(R.string.block_device))) {
                    call = mTodoService.blockChild(idChildSelected);
                } else call = mTodoService.unblockChild(idChildSelected);
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
                if (b.getText().equals(getString(R.string.block_device)))
                    b.setText(getString(R.string.unblock_device));
                else b.setText(getString(R.string.block_device));
            });
        }

        Button nitButton = (Button) root.findViewById(R.id.BT_nits);
        nitButton.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), HorarisActivity.class);
            i.putExtra("idChild", idChildSelected);
            startActivity(i);
        });

        Button BT_FreeTime = (Button) root.findViewById(R.id.BT_FreeTime);
        BT_FreeTime.setVisibility(View.GONE);
        if (TodoApp.getTutor() == 1) {
            BT_FreeTime.setVisibility(View.VISIBLE);
            BT_FreeTime.setOnClickListener(v -> {
                Call<String> call = null;
                if (BT_FreeTime.getText().equals(getString(R.string.free_time))) {
                    call = mTodoService.blockChild(idChildSelected);
                } else call = mTodoService.unblockChild(idChildSelected);
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
                if (BT_FreeTime.getText().equals(getString(R.string.free_time)))
                    BT_FreeTime.setText(getString(R.string.stop_free_time));
                else BT_FreeTime.setText(getString(R.string.free_time));
            });
        }

        ConstraintLayout CL_info = (ConstraintLayout) root.findViewById(R.id.CL_info);
        ConstraintLayout CL_infoButtons = (ConstraintLayout) root.findViewById(R.id.CL_infoButtons);
        CL_info.setOnClickListener((View.OnClickListener) v -> {
            if (CL_infoButtons.getVisibility() == View.GONE) {
                CL_infoButtons.setVisibility(View.VISIBLE);

                ImageView IV_openInfo = (ImageView) root.findViewById(R.id.IV_openInfo);
                IV_openInfo.setImageResource(R.drawable.ic_arrow_close);
            } else {
                CL_infoButtons.setVisibility(View.GONE);

                ImageView IV_openInfo = (ImageView) root.findViewById(R.id.IV_openInfo);
                IV_openInfo.setImageResource(R.drawable.ic_arrow_open);
            }
        });

        /** Posar icona de desplegar en la posició correcta **/
        if (CL_infoButtons.getVisibility() == View.GONE) {
            ImageView IV_openInfo = (ImageView) root.findViewById(R.id.IV_openInfo);
            IV_openInfo.setImageResource(R.drawable.ic_arrow_open);
        } else {
            ImageView IV_openInfo = (ImageView) root.findViewById(R.id.IV_openInfo);
            IV_openInfo.setImageResource(R.drawable.ic_arrow_close);
        }

        ConstraintLayout CL_limit = (ConstraintLayout) root.findViewById(R.id.CL_suport);
        ConstraintLayout CL_limitButtons = (ConstraintLayout) root.findViewById(R.id.CL_suportButtons);
        CL_limit.setOnClickListener((View.OnClickListener) v -> {
            if (CL_limitButtons.getVisibility() == View.GONE) {
                CL_limitButtons.setVisibility(View.VISIBLE);

                ImageView IV_openLimit = (ImageView) root.findViewById(R.id.IV_openSuport);
                IV_openLimit.setImageResource(R.drawable.ic_arrow_close);
            } else {
                CL_limitButtons.setVisibility(View.GONE);

                ImageView IV_openLimit = (ImageView) root.findViewById(R.id.IV_openSuport);
                IV_openLimit.setImageResource(R.drawable.ic_arrow_open);
            }
        });

        /** Posar icona de desplegar en la posició correcta **/
        if (CL_limitButtons.getVisibility() == View.GONE) {
            ImageView IV_openLimit = (ImageView) root.findViewById(R.id.IV_openSuport);
            IV_openLimit.setImageResource(R.drawable.ic_arrow_open);
        } else {
            ImageView IV_openLimit = (ImageView) root.findViewById(R.id.IV_openSuport);
            IV_openLimit.setImageResource(R.drawable.ic_arrow_close);
        }
    }

    private void getStats() {
        String dataAvui = Funcions.date2String(Calendar.getInstance().get(Calendar.DAY_OF_MONTH), Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.YEAR));
        Call<Collection<GeneralUsage>> call = mTodoService.getGenericAppUsage(idChildSelected, dataAvui, dataAvui);
        call.enqueue(new Callback<Collection<GeneralUsage>>() {
            @Override
            public void onResponse(Call<Collection<GeneralUsage>> call, Response<Collection<GeneralUsage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Collection<GeneralUsage> collection = response.body();
                    Funcions.canviarMesosDeServidor(collection);
                    makeGraph(collection);
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

    private void makeGraph(Collection<GeneralUsage> genericAppUsage) {
        pieChart = (PieChart) root.findViewById(R.id.Ch_Pie);
        long totalUsageTime = 0;

        Map<String, Long> mapUsage = new HashMap<>();

        for (GeneralUsage gu : genericAppUsage) {
            if (gu.totalTime > 0) {
                totalUsageTime += gu.totalTime;
                for (AppUsage au : gu.usage) {
                    if (mapUsage.containsKey(au.app.appName))
                        mapUsage.put(au.app.appName, mapUsage.get(au.app.appName) + au.totalTime);
                    else mapUsage.put(au.app.appName, au.totalTime);
                }
            }
        }

        setMascot(totalUsageTime);
        setPieChart(mapUsage, totalUsageTime);
    }

    private void setMascot(long totalUsageTime) {
        ImageView IV_mascot = root.findViewById(R.id.IV_mascot);

        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 21) {
            if (totalUsageTime < TimeUnit.HOURS.toMillis(1))
                IV_mascot.setImageResource(R.drawable.mascot_min);
            else if (totalUsageTime < TimeUnit.MINUTES.toMillis(90))
                IV_mascot.setImageResource(R.drawable.mascot_hora);
            else if (totalUsageTime < TimeUnit.MINUTES.toMillis(135))
                IV_mascot.setImageResource(R.drawable.mascot_molt);
            else IV_mascot.setImageResource(R.drawable.mascot_max);
        } else {
            IV_mascot.setImageResource(R.drawable.mascot_nit);
        }
    }

    private void setPieChart(Map<String, Long> mapUsage, long totalUsageTime) {
        ArrayList<PieEntry> yValues = new ArrayList<>();
        long others = 0;
        for (Map.Entry<String, Long> entry : mapUsage.entrySet()) {
            if (entry.getValue() >= totalUsageTime * 0.05)
                yValues.add(new PieEntry(entry.getValue(), entry.getKey()));
            else {
                others += entry.getValue();
            }
        }

        yValues.add(new PieEntry(others, "Altres"));

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
        pieChart.setHoleColor(Color.TRANSPARENT);
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


                Pair<Integer, Integer> appTime = Funcions.millisToString(e.getY());

                if (appTime.first == 0)
                    pieChart.setCenterText(getResources().getString(R.string.mins, appTime.second));
                else
                    pieChart.setCenterText(getResources().getString(R.string.hours_endl_minutes, appTime.first, appTime.second));
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

    @Override
    protected void finalize() throws Throwable {
        if (TodoApp.getTutor() == 1)
            Funcions.askChildForLiveApp(getContext(), idChildSelected, false);
        super.finalize();
    }
}
