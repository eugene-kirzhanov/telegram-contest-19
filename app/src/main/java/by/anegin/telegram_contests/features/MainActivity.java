package by.anegin.telegram_contests.features;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;
import by.anegin.telegram_contests.ChartsApp;
import by.anegin.telegram_contests.R;
import by.anegin.telegram_contests.core.data.DataRepository;
import by.anegin.telegram_contests.core.data.model.Chart;
import by.anegin.telegram_contests.core.data.model.Data;
import by.anegin.telegram_contests.core.di.AppComponent;
import by.anegin.telegram_contests.core.ui.ThemeHelper;
import by.anegin.telegram_contests.core.ui.model.UiChart;
import by.anegin.telegram_contests.core.ui.view.ChartView;
import by.anegin.telegram_contests.core.ui.view.MiniChartView;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int MENUITEM_ID_FIRST = 42;
    private static final String STATE_CURRENT_CHART_INDEX = "current_chart_index";

    private DataRepository dataRepository;

    private ThemeHelper themeHelper;

    private final Executor loadExecutor = Executors.newSingleThreadExecutor();
    private final Executor showExecutor = Executors.newSingleThreadExecutor();

    private ChartView chartView;
    private LinearLayout layoutGraphs;

    private Data data;
    private int currentChartIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prepareDependencies();

        int theme = themeHelper.getTheme();
        setTheme(theme);

        setContentView(R.layout.activity_main);

        chartView = findViewById(R.id.chartView);

        MiniChartView miniChartView = findViewById(R.id.miniChartView);
        miniChartView.attachToChartView(chartView);

        if (savedInstanceState != null) {
            currentChartIndex = savedInstanceState.getInt(STATE_CURRENT_CHART_INDEX);
        }

        layoutGraphs = findViewById(R.id.layoutGraphs);

        loadData();
    }

    private void prepareDependencies() {
        AppComponent appComponent = ((ChartsApp) getApplication()).getAppComponent();
        dataRepository = appComponent.getDataRepository();
        themeHelper = appComponent.getThemeHelper();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_CHART_INDEX, currentChartIndex);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Data data = this.data;
        int chartsCount = (data != null && data.charts != null) ? data.charts.size() : 0;
        menu.removeGroup(R.id.action_group_chart_selection);
        for (int i = 0; i < chartsCount; i++) {
            MenuItem item = menu.add(R.id.action_group_chart_selection, MENUITEM_ID_FIRST + i, i, "Chart " + (i + 1));
            item.setCheckable(true);
            item.setChecked(currentChartIndex == i);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        Data data = this.data;
        int chartsCount = (data != null && data.charts != null) ? data.charts.size() : 0;

        if (chartsCount > 0 && itemId >= MENUITEM_ID_FIRST && itemId < MENUITEM_ID_FIRST + chartsCount) {
            int index = itemId - MENUITEM_ID_FIRST;
            showChart(index);
            return true;
        } else {
            switch (itemId) {
                case R.id.action_toggle_theme:
                    toggleTheme();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }
    }

    private void loadData() {
        loadExecutor.execute(() -> {
            try {
                this.data = dataRepository.getData();
                showChart(currentChartIndex);
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Error loading data file", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showChart(int index) {
        currentChartIndex = index;
        showExecutor.execute(() -> {
            Data data = this.data;
            if (data == null || data.charts == null || index >= data.charts.size()) return;

            Chart chart = data.charts.get(index);
            UiChart uiChart = new UiChart(chart);

            runOnUiThread(() -> {
                chartView.setUiChart(uiChart);
                invalidateOptionsMenu();
            });
        });
    }

    private void toggleTheme() {
        themeHelper.toggleTheme();
        recreate();
    }

}
