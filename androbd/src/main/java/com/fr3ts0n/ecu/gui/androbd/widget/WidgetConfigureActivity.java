package com.fr3ts0n.ecu.gui.androbd.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fr3ts0n.ecu.EcuDataItem;
import com.fr3ts0n.ecu.EcuDataItems;
import com.fr3ts0n.ecu.gui.androbd.R;
import com.fr3ts0n.ecu.prot.obd.ObdProt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Configuration Activity shown when an OBD widget is added or edited.
 */
public class WidgetConfigureActivity extends AppCompatActivity
{
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private RadioGroup rgType;
    private RadioButton rbDigital;
    private RadioButton rbGauge;
    private RadioButton rbChart;
    private LinearLayout layoutGaugeOptions;
    private LinearLayout layoutChartOptions;
    private EditText etMinVal;
    private EditText etMaxVal;
    private EditText etRedlineVal;
    private Spinner spinnerTimeWindow;
    private Spinner spinnerUpdateRate;
    private EditText etSearchPid;
    private Button btnSave;

    private final List<EcuDataItem> allItems = new ArrayList<>();
    private final List<EcuDataItem> filteredItems = new ArrayList<>();
    private PidListAdapter adapter;
    private EcuDataItem selectedItem = null;

    private static final int[] TIME_WINDOW_VALUES = {60, 120, 300, 600};
    private static final String[] TIME_WINDOW_LABELS = {
            "60 Sek. (1 Minute)",
            "120 Sek. (2 Minuten)",
            "300 Sek. (5 Minuten)",
            "600 Sek. (10 Minuten)"
    };

    private static final int[] UPDATE_RATE_VALUES = {1000, 200, 100, 50};
    private static final String[] UPDATE_RATE_LABELS = {
            "1 Hz (1000 ms) — Energiesparmodus",
            "5 Hz (200 ms) — Extrem flüssig",
            "10 Hz (100 ms) — Ultra Reaktionsschnell",
            "20 Hz (50 ms) — Maximale Frequenz"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Apply selected language locale
        com.fr3ts0n.ecu.gui.androbd.SettingsActivity.applyLocale(this);

        // Set result to CANCELED first so if user cancels before saving, the launcher knows
        setResult(RESULT_CANCELED);
        setContentView(R.layout.widget_configure);

        // Get appWidgetId from Intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null)
        {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
        {
            finish();
            return;
        }

        rgType = findViewById(R.id.rg_widget_type);
        rbDigital = findViewById(R.id.rb_type_digital);
        rbGauge = findViewById(R.id.rb_type_gauge);
        rbChart = findViewById(R.id.rb_type_chart);
        layoutGaugeOptions = findViewById(R.id.layout_gauge_options);
        layoutChartOptions = findViewById(R.id.layout_chart_options);
        etMinVal = findViewById(R.id.et_min_val);
        etMaxVal = findViewById(R.id.et_max_val);
        etRedlineVal = findViewById(R.id.et_redline_val);
        spinnerTimeWindow = findViewById(R.id.spinner_time_window);
        spinnerUpdateRate = findViewById(R.id.spinner_update_rate);
        etSearchPid = findViewById(R.id.et_search_pid);
        btnSave = findViewById(R.id.btn_save_widget_config);

        // Populate Time Window Spinner
        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TIME_WINDOW_LABELS);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeWindow.setAdapter(timeAdapter);

        // Populate Update Rate Spinner
        ArrayAdapter<String> rateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, UPDATE_RATE_LABELS);
        rateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUpdateRate.setAdapter(rateAdapter);

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_type_gauge)
            {
                layoutGaugeOptions.setVisibility(View.VISIBLE);
                layoutChartOptions.setVisibility(View.GONE);
            }
            else if (checkedId == R.id.rb_type_chart)
            {
                layoutGaugeOptions.setVisibility(View.GONE);
                layoutChartOptions.setVisibility(View.VISIBLE);
            }
            else
            {
                layoutGaugeOptions.setVisibility(View.GONE);
                layoutChartOptions.setVisibility(View.GONE);
            }
        });

        // Load existing widget settings if editing
        String currentMnemonic = ObdWidgetPrefs.getWidgetPid(this, appWidgetId);
        String currentType = ObdWidgetPrefs.getWidgetType(this, appWidgetId);
        float currentMin = ObdWidgetPrefs.getWidgetMin(this, appWidgetId);
        float currentMax = ObdWidgetPrefs.getWidgetMax(this, appWidgetId);
        float currentRedline = ObdWidgetPrefs.getWidgetRedline(this, appWidgetId);
        int currentTimeWindow = ObdWidgetPrefs.getWidgetTimeWindow(this, appWidgetId);
        int currentUpdateRate = ObdWidgetPrefs.getWidgetUpdateInterval(this, appWidgetId);

        if (ObdWidgetPrefs.TYPE_GAUGE.equals(currentType))
        {
            rbGauge.setChecked(true);
            layoutGaugeOptions.setVisibility(View.VISIBLE);
            layoutChartOptions.setVisibility(View.GONE);
        }
        else if (ObdWidgetPrefs.TYPE_CHART.equals(currentType))
        {
            rbChart.setChecked(true);
            layoutGaugeOptions.setVisibility(View.GONE);
            layoutChartOptions.setVisibility(View.VISIBLE);
        }
        else
        {
            rbDigital.setChecked(true);
            layoutGaugeOptions.setVisibility(View.GONE);
            layoutChartOptions.setVisibility(View.GONE);
        }

        etMinVal.setText(String.format(Locale.getDefault(), "%.0f", currentMin));
        etMaxVal.setText(String.format(Locale.getDefault(), "%.0f", currentMax));
        etRedlineVal.setText(String.format(Locale.getDefault(), "%.0f", currentRedline));

        // Set Time Window Spinner selection
        for (int i = 0; i < TIME_WINDOW_VALUES.length; i++)
        {
            if (TIME_WINDOW_VALUES[i] == currentTimeWindow)
            {
                spinnerTimeWindow.setSelection(i);
                break;
            }
        }

        // Set Update Rate Spinner selection
        for (int i = 0; i < UPDATE_RATE_VALUES.length; i++)
        {
            if (UPDATE_RATE_VALUES[i] == currentUpdateRate)
            {
                spinnerUpdateRate.setSelection(i);
                break;
            }
        }

        // Ensure EcuDataItems are loaded by touching ObdProt.dataItems
        if (EcuDataItems.byMnemonic.isEmpty() && ObdProt.dataItems != null)
        {
            // Touch class loader
        }

        // Build list of items
        allItems.clear();
        synchronized (EcuDataItems.byMnemonic)
        {
            for (Map.Entry<String, EcuDataItem> entry : EcuDataItems.byMnemonic.entrySet())
            {
                EcuDataItem item = entry.getValue();
                if (item != null)
                {
                    allItems.add(item);
                    if (currentMnemonic != null && currentMnemonic.equalsIgnoreCase(item.pv != null ? (String) item.pv.get("MNEMONIC") : ""))
                    {
                        selectedItem = item;
                    }
                }
            }
        }

        // Sort items by label/mnemonic
        Collections.sort(allItems, (o1, o2) -> {
            String name1 = o1.label != null ? o1.label : o1.toString();
            String name2 = o2.label != null ? o2.label : o2.toString();
            return name1.compareToIgnoreCase(name2);
        });

        if (selectedItem == null && !allItems.isEmpty())
        {
            selectedItem = allItems.get(0);
        }

        filteredItems.clear();
        filteredItems.addAll(allItems);

        ListView listView = findViewById(R.id.pid_list_view);
        adapter = new PidListAdapter(filteredItems);
        listView.setAdapter(adapter);

        // Search Filter TextWatcher
        etSearchPid.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                filterPids(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < filteredItems.size())
            {
                selectedItem = filteredItems.get(position);
                adapter.notifyDataSetChanged();

                // Update default Min/Max/Redline values for selected item
                String mnemonic = selectedItem.pv != null && selectedItem.pv.get("MNEMONIC") != null
                        ? selectedItem.pv.get("MNEMONIC").toString()
                        : selectedItem.label;

                if ("engine_speed".equalsIgnoreCase(mnemonic) || "RPM".equalsIgnoreCase(mnemonic))
                {
                    etMinVal.setText("0"); etMaxVal.setText("8000"); etRedlineVal.setText("6000");
                }
                else if ("SPEED".equalsIgnoreCase(mnemonic) || (mnemonic != null && mnemonic.toLowerCase().contains("speed")))
                {
                    etMinVal.setText("0"); etMaxVal.setText("240"); etRedlineVal.setText("200");
                }
                else if ("COOLANT_TEMP".equalsIgnoreCase(mnemonic) || (mnemonic != null && mnemonic.toLowerCase().contains("temp")))
                {
                    etMinVal.setText("0"); etMaxVal.setText("130"); etRedlineVal.setText("105");
                }
            }
        });

        // Save Button Handler
        btnSave.setOnClickListener(v -> saveAndFinish());
    }

    private void filterPids(String query)
    {
        filteredItems.clear();
        String q = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";

        if (q.isEmpty())
        {
            filteredItems.addAll(allItems);
        }
        else
        {
            for (EcuDataItem item : allItems)
            {
                String title = item.label != null ? item.label.toLowerCase(Locale.ROOT) : "";
                String mnemonic = item.pv != null && item.pv.get("MNEMONIC") != null
                        ? item.pv.get("MNEMONIC").toString().toLowerCase(Locale.ROOT)
                        : "";

                if (title.contains(q) || mnemonic.contains(q))
                {
                    filteredItems.add(item);
                }
            }
        }
        if (adapter != null)
        {
            adapter.notifyDataSetChanged();
        }
    }

    private void saveAndFinish()
    {
        if (selectedItem == null && !allItems.isEmpty())
        {
            selectedItem = allItems.get(0);
        }

        if (selectedItem == null)
        {
            Toast.makeText(this, getString(R.string.widget_select_pid_toast), Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedMnemonic = selectedItem.pv != null && selectedItem.pv.get("MNEMONIC") != null
                ? selectedItem.pv.get("MNEMONIC").toString()
                : selectedItem.label;

        if (selectedMnemonic == null || selectedMnemonic.isEmpty())
        {
            selectedMnemonic = selectedItem.toString();
        }

        int checkedId = rgType.getCheckedRadioButtonId();
        String selectedType = ObdWidgetPrefs.TYPE_DIGITAL;
        if (checkedId == R.id.rb_type_gauge)
        {
            selectedType = ObdWidgetPrefs.TYPE_GAUGE;
        }
        else if (checkedId == R.id.rb_type_chart)
        {
            selectedType = ObdWidgetPrefs.TYPE_CHART;
        }

        float minVal = parseDecimal(etMinVal.getText().toString(), 0f);
        float maxVal = parseDecimal(etMaxVal.getText().toString(), 8000f);
        float redlineVal = parseDecimal(etRedlineVal.getText().toString(), 6000f);

        int timeWindowIdx = spinnerTimeWindow.getSelectedItemPosition();
        int timeWindowSeconds = (timeWindowIdx >= 0 && timeWindowIdx < TIME_WINDOW_VALUES.length)
                ? TIME_WINDOW_VALUES[timeWindowIdx]
                : 60;

        int updateRateIdx = spinnerUpdateRate.getSelectedItemPosition();
        int updateIntervalMs = (updateRateIdx >= 0 && updateRateIdx < UPDATE_RATE_VALUES.length)
                ? UPDATE_RATE_VALUES[updateRateIdx]
                : 200;

        // Save configuration
        ObdWidgetPrefs.saveWidgetConfig(
                this,
                appWidgetId,
                selectedMnemonic,
                selectedType,
                minVal,
                maxVal,
                redlineVal,
                timeWindowSeconds,
                updateIntervalMs
        );

        // Update widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ObdWidgetManager.updateWidget(this, appWidgetManager, appWidgetId);

        // Return success result
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private float parseDecimal(String input, float fallback)
    {
        try
        {
            return Float.parseFloat(input.trim());
        }
        catch (Exception e)
        {
            return fallback;
        }
    }

    private class PidListAdapter extends BaseAdapter
    {
        private final List<EcuDataItem> itemList;

        PidListAdapter(List<EcuDataItem> itemList)
        {
            this.itemList = itemList;
        }

        @Override
        public int getCount()
        {
            return itemList.size();
        }

        @Override
        public Object getItem(int position)
        {
            return itemList.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View view = convertView;
            if (view == null)
            {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.widget_config_item, parent, false);
            }

            EcuDataItem item = itemList.get(position);
            TextView titleView = view.findViewById(R.id.pid_item_title);
            TextView descView = view.findViewById(R.id.pid_item_description);

            String mnemonic = item.pv != null && item.pv.get("MNEMONIC") != null ? item.pv.get("MNEMONIC").toString() : "";
            String defaultTitle = item.label != null && !item.label.isEmpty() ? item.label : item.toString();
            String title = !mnemonic.isEmpty() ? com.fr3ts0n.ecu.prot.obd.Messages.getString(mnemonic, defaultTitle) : defaultTitle;
            String units = item.pv != null ? item.pv.getUnits() : "";

            if (item == selectedItem)
            {
                view.setBackgroundColor(0x3300E5FF); // Highlight selected PID
            }
            else
            {
                view.setBackgroundColor(0x00000000);
            }

            titleView.setText(title);
            descView.setText(String.format("%s %s", mnemonic, units != null && !units.isEmpty() ? "(" + units + ")" : "").trim());

            return view;
        }
    }
}
