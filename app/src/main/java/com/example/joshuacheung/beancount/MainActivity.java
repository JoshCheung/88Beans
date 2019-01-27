package com.example.joshuacheung.beancount;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.Context;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.support.annotation.MainThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.app.Dialog;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MAINACTIVITY";
    public static ArrayList<CoffeeElement> listData = new ArrayList<>();
    public ArrayList <String> mNames = new ArrayList<>();
    ArrayAdapter<CoffeeElement> adapter = null;
    DatabaseHelper mDatabasehelper;
    private DatePickerDialog.OnDateSetListener mDateSetListener;

     enum CoffeeTypes{
        HAYES("Hayes Valley", 22),
        BLEND("Blend" ,30),
        SOE("Single Origin Espresso", 22),
        DECAF("Decaf", 22),
        SOD("Single Origin Drip", 23);

        private int factor;
        private String name;

        public int getFactor() {
            return factor;
        }

        public String getName() {
            return name;
        }

        CoffeeTypes(String name, int factor) {
            this.name = name;
            this.factor = factor;
        }
    }

    public HashMap <String, ArrayList> weeks = new HashMap<>();

    // Store the dates of each week
    public ArrayList<String> dates = new ArrayList<>();

    private class ListAdapter extends ArrayAdapter<CoffeeElement> {
        public ListAdapter() {
            super(MainActivity.this, R.layout.main_coffee_element, R.id.coffeeListView, listData);
        } //Change according to Database
        public View getView(final int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.main_coffee_element, null);
            }
            //Set Coffee Title
            TextView title = itemView.findViewById(R.id.coffeeTitle);
            TextView date = itemView.findViewById(R.id.dateOutput);
            TextView cups = itemView.findViewById(R.id.cups_served);
            TextView weight = itemView.findViewById(R.id.weight_output);
            title.setText(listData.get(position).getName());
//            date.setText(listData.get(position).getDate());
            cups.setText(listData.get(position).getCupsSold() + "");
            weight.setText(String.format("%.2f lb", listData.get(position).getWeight()));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Vibrator vibe = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE) ;
                    vibe.vibrate(30);
                    final Dialog dialog = new Dialog(MainActivity.this);
                    dialog.setContentView(R.layout.edit_or_delete_instance);
                    Button update = dialog.findViewById(R.id.update);
                    Button delete = dialog.findViewById(R.id.delete);
                    Spinner spinner = dialog.findViewById(R.id.spinner);
                    final DatePicker datePicker = dialog.findViewById(R.id.Datepick);
                    ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(
                            MainActivity.this, R.layout.spinner_item, mNames);
                    myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(myAdapter);
                    spinner.setSelection(mNames.indexOf(listData.get(position).getName()));

                    EditText cups = dialog.findViewById(R.id.cups_served);
                    cups.setText(String.valueOf(listData.get(position).getCupsSold()));

                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MM-dd-yyyy");
                    Date oldDate = new Date();

                    try{
                        oldDate = dateFormat.parse(listData.get(position).getDate());
                    }
                    catch (Exception e) {
                        toastMessage("Could not convert Date");
                    }

                    LocalDate localDate = oldDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    int year  = localDate.getYear();
                    int month = (localDate.getMonthValue()-1);
                    int day   = localDate.getDayOfMonth();

                    datePicker.updateDate(year, month, day);

                    TextView weightOutput = dialog.findViewById(R.id.weight_output);
                    double oldWeight = listData.get(position).getWeight();
                    weightOutput.setText(String.format(String.valueOf(oldWeight)));
                    String oldName = listData.get(position).getName();

                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            for (CoffeeTypes coffeeTypes : CoffeeTypes.values()) {
                                if (coffeeTypes.name == mNames.get(i)) {
                                    cups.addTextChangedListener(new TextWatcher() {
                                        @Override
                                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                                        }

                                        @Override
                                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                                        }

                                        @Override
                                        public void afterTextChanged(Editable editable) {
                                            try {
                                                if (!cups.getText().toString().equals("") || cups.getText().toString() == null) {
                                                    int cupsServed = Integer.parseInt(cups.getText().toString());
                                                    String weight = calculateFactor(coffeeTypes.factor, cupsServed);
                                                    weightOutput.setText(String.format("%s", weight));
                                                }
                                                else {
                                                    weightOutput.setText("");
                                                }
                                            }
                                            catch (Exception e) {
                                                toastMessage("Number is too big");
                                                dialog.dismiss();
                                            }
                                        }
                                    });
                                }
                            }
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });


                    update.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Cursor data = mDatabasehelper.getItemID(oldName);
                            int itemID = -1;
                            while (data.moveToNext()) {
                                itemID = data.getInt(0);
                            }
                            if (itemID > -1) {
                                Log.d("Item clicked", "onItemCLick: the ID is " + itemID);
                                toastMessage("Item was clicked for updating: " + itemID +", " + listData.get(position).toString());
                                Date date = new Date(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth()-1);
                                SimpleDateFormat simpledateformat = new SimpleDateFormat("EEEE");
                                String dayOfWeek = simpledateformat.format(date);
                                String day = "";
                                if (datePicker.getDayOfMonth() < 10) {
                                    day = "0" + datePicker.getDayOfMonth();
                                }
                                String entryDate = (dayOfWeek + ", " + datePicker.getMonth()+1)+"-"+day+"-"+datePicker.getYear();
                                int cups_sold = Integer.parseInt(cups.getText().toString());
                                String newName = spinner.getSelectedItem().toString();
                                double weight = Double.parseDouble(weightOutput.getText().toString());
                                mDatabasehelper.updateItem(itemID, oldName, entryDate, cups_sold, weight, newName);
                                listData.set(position, new CoffeeElement(entryDate, cups_sold, weight, newName));
                                adapter.notifyDataSetChanged();
                            } else {
                                toastMessage("No ID associated with that name");
                            }
                            dialog.dismiss();
                        }
                    });

                    delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            toastMessage("Item was clicked: " + listData.get(position).getName());
                            String name = listData.get(position).getName();
                            Log.d("ITEM CLICKED", "onItemClicked: you clicked on: " + name);

                            Cursor data = mDatabasehelper.getItemID(name);
                            int itemID = -1;
                            while (data.moveToNext()) {
                                itemID = data.getInt(0);
                            }
                            if (itemID > -1) {
                                Log.d("Item clicked", "onItemCLick: the ID is " + itemID);
                                toastMessage("Item was clicked for deletion: " + itemID +", " + listData.get(position).getName());
                                mDatabasehelper.deleteName(itemID, name);
                                listData.remove(position);
                                adapter.notifyDataSetChanged();
                            } else {
                                toastMessage("No ID associated with that name");
                            }
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
            });
            return itemView;
        }
    }

    public void getNames() {
        mNames.add("Hayes Valley");
        mNames.add("Blend");
        mNames.add("Single Origin Espresso");
        mNames.add("Decaf");
        mNames.add("Single Origin Drip");
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this, mNames);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listData = new ArrayList<>();
        mDatabasehelper = new DatabaseHelper(this);
        adapter = new ListAdapter();
//        pushSomeData();
        setContentView(R.layout.app_bar_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addCoffeeItem(view);
            }
        });

        FloatingActionButton homeButton = findViewById(R.id.home_button);
        homeButton.setEnabled(false);

        setDate();
        todayEntry();
        getNames();
        initRecyclerView();
        weekAverages();
    }

    public class Averages {
        private String name;
        private double weekdayAverage;
        private double weekendAverage;

        public Averages(String name, double weekdayAverage, double weekendAverage) {
            this.name = name;
            this.weekdayAverage = weekdayAverage;
            this.weekendAverage = weekendAverage;
        }

        public String getName() {
            return name;
        }

        public double getWeekdayAverage() {
            return weekdayAverage;
        }

        public double getWeekendAverage() {
            return weekendAverage;
        }

        public String toString() { return name + "Weekday Average: " + getWeekdayAverage() + ", Weekend Average: " + getWeekendAverage(); }
    }



//    public void weekAverages() {
//        HashMap<String, Double> weekdayTotals = new HashMap<>();
//        HashMap<String, Double> weekendTotals = new HashMap<>();
//
//        HashMap<String, Integer> weekendCounter = new HashMap<>();
//        HashMap<String, Integer> weekdayCounter = new HashMap<>();
//
//        ArrayList<Averages> smallData = new ArrayList<>();
//
//        Cursor data = mDatabasehelper.getData();
//
//        if (data.getCount() == 0) {
//            return;
//        }
//        while (data.moveToNext()) {
//            String name = data.getString(4);
//            String date = data.getString(1);
//            double weight = Double.parseDouble(data.getString(3));
//
//            if (date.indexOf("Saturday") >= 0 || date.indexOf("Sunday") >= 0) {
//                if (!weekendTotals.containsKey(name)) {
//                    weekendTotals.put(name, weight);
//                    weekendCounter.put(name, 1);
//                }
//                else {
//                    weekendTotals.put(name, weekendTotals.get(name) + weight);
//                    weekendCounter.put(name, weekendCounter.get(name) + 1);
//                }
//            }
//            else {
//                if (!weekdayTotals.containsKey(name)) {
//                    weekdayTotals.put(name, weight);
//                    weekdayCounter.put(name, 1);
//                }
//                else {
//                    weekdayTotals.put(name, weekdayTotals.get(name) + weight);
//                    weekdayCounter.put(name, weekdayCounter.get(name) + 1);
//                }
//            }
//        }
//
//
//
////        try {
////            if (!weekdayTotals.isEmpty()) {
////                Set<String> keys = weekdayTotals.keySet();
////                if (!weekendTotals.isEmpty()) {
////                    for (String key : weekendTotals.keySet()) {
////                        keys.add(key);
////                    }
////                    for (String key : keys) {
////                        smallData.add(new Averages(key, (weekdayTotals.get(key)/(weekdayCounter.get(key)/5)), (weekendTotals.get(key))/(weekendCounter.get(key)/2)));
////                    }
////                }
////            }
////        }
////        catch (Exception e){
////
////        }
//
//
//        AverageAdapter adapter = new AverageAdapter(this, smallData);
//        ListView averages = findViewById(R.id.small_data);
//        averages.setAdapter(adapter);
//    }

    public void weekAverages() {

        // Name of the coffee and double the weight
        HashMap<String, HashMap<String, Double>> weekTotals = new HashMap<>();
        int weekends = 0;
        int weekdays = 0;

        ArrayList<Averages> smallData = new ArrayList<>();

        Cursor data = mDatabasehelper.getData();

        if (data.getCount() == 0) {
            return;
        }
        while (data.moveToNext()) {

            String name = data.getString(4);
            String date = data.getString(1);
            double weight = Double.parseDouble(data.getString(3));

            if (!weekTotals.containsKey(name)) {
                weekTotals.put(name, new HashMap<String, Double>());
                weekTotals.get(name).put("weekday", 0.0);
                weekTotals.get(name).put("weekend", 0.0);
            }
            if (date.substring(0, date.indexOf(",")).equals("Saturday") || date.substring(0, date.indexOf(",")).equals("Sunday")) {
                weekTotals.get(name).put("weekend", weekTotals.get(name).get("weekend") + weight);
                weekends++;
            }
            else {
                weekTotals.get(name).put("weekday", weekTotals.get(name).get("weekday") + weight);
                weekdays++;
            }
        }

        for (String key: weekTotals.keySet() ) {
            double avgWeekday = 0.0;
            double avgWeekend = 0.0;
            if (weekdays >= 5) {
                avgWeekday = weekTotals.get(key).get("weekday")/ (weekdays/5);
            }
            else {
                avgWeekday = weekTotals.get(key).get("weekday")/weekdays;
            }
            if (weekends >= 2) {
                avgWeekend = weekTotals.get(key).get("weekend")/ (weekends/2);
            }
            else {
                avgWeekday = weekTotals.get(key).get("weekend")/weekends;
            }
            smallData.add(new Averages(key, avgWeekday, avgWeekend));
        }
        AverageAdapter adapter = new AverageAdapter(this, smallData);
        ListView averages = findViewById(R.id.small_data);
        averages.setAdapter(adapter);
    }

    public class AverageAdapter extends ArrayAdapter<Averages> {
        public AverageAdapter(Context context, ArrayList<Averages> averages) {
            super(context, 0, averages);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Averages average = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.averages_entry, parent, false);
            }
            // Lookup view for data population
            TextView tvName = convertView.findViewById(R.id.name);
            TextView weekdayaverageTV = convertView.findViewById(R.id.weekday_average);
            TextView weekendaverageTV = convertView.findViewById(R.id.weekend_average);

            // Populate the data into the template view using the data object
            tvName.setText(average.getName());
            weekdayaverageTV.setText(String.format("%.2f lb", average.getWeekdayAverage()));
            weekendaverageTV.setText(String.format("%.2f lb", average.getWeekendAverage()));
            // Return the completed view to render on screen
            return convertView;
        }
    }

    @Override
    public void onBackPressed() {
         super.onBackPressed();
    }


    public void allData(View view) {
        try {
            Intent intent = new Intent(MainActivity.this, DataListing.class);
            intent.putExtra("type", "88 Beans");
            startActivity(intent);
        }
        catch (Exception e) {
            toastMessage("No Data Available");
        }
    }

    // Allows user to add type of Coffee bean to track
    public void addCoffeeItem(View view) {
        // Use the current date as the default date in the picker
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.add_bean_type);

        Spinner spinner = dialog.findViewById(R.id.spinner);
        final DatePicker datePicker = dialog.findViewById(R.id.Datepick);
        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(
                MainActivity.this, R.layout.spinner_item, mNames);
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(myAdapter);
        EditText cupsSold = dialog.findViewById(R.id.cups_served);
        TextView weightOutput = dialog.findViewById(R.id.weight_output);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                weightOutput.setText("");
                cupsSold.setText("");
                for (CoffeeTypes coffeeTypes : CoffeeTypes.values()) {
                    if (coffeeTypes.name == mNames.get(i)) {
                        cupsSold.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                            }

                            @Override
                            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                            }

                            @Override
                            public void afterTextChanged(Editable editable) {
                                try {
                                    if (!cupsSold.getText().toString().equals("") || cupsSold.getText().toString() == null) {
                                        int cupsServed = Integer.parseInt(cupsSold.getText().toString());
                                        String weight = calculateFactor(coffeeTypes.factor, cupsServed);
                                        weightOutput.setText(String.format("%s", weight));
                                    }
                                    else {
                                        weightOutput.setText("");
                                    }
                                }
                                catch (Exception e) {
                                    toastMessage("Number is too big");
                                    dialog.dismiss();
                                }
                            }
                        });
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Button enter = dialog.findViewById(R.id.enter);
        Button cancel = dialog.findViewById(R.id.cancel);
        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cupsSold.getText().toString().equals("")) {
                    toastMessage("Cups cannot be left empty");
                }
                else {
                    String name = spinner.getSelectedItem().toString();
                    int cups = Integer.parseInt(cupsSold.getText().toString());
                    double weight = Double.parseDouble(weightOutput.getText().toString());
//                    String dbTime = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now);
                    SimpleDateFormat simpledateformat = new SimpleDateFormat("EEEE");
                    String day;
                    String month;
                    if (datePicker.getDayOfMonth() < 10) {
                        day = "0" + datePicker.getDayOfMonth();
                    }
                    else {
                        day = String.valueOf(datePicker.getDayOfMonth());
                    }

                    if (datePicker.getMonth() < 10) {
                        month = "0" + (datePicker.getMonth()+1);
                    }
                    else {
                        month = String.valueOf((datePicker.getMonth() + 1));
                    }
                    Date date = new Date(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth()-1);
                    String dayOfWeek = simpledateformat.format(date);
                    String entryDate = (dayOfWeek + ", " + month +"-"+day+"-"+datePicker.getYear());
                    if (checkIsValid(entryDate, name)) {
                        listData.add(0, new CoffeeElement(entryDate, cups, weight, name));
                        AddData(entryDate, cups, weight, name);
                    }
                    adapter.notifyDataSetChanged();
                    dialog.dismiss();
                }
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public String calculateFactor(int cupFactor, int cupsServed) {
        Format df = new DecimalFormat("#.00");
        double factor = cupFactor * 1.3/453.5;
        double weight = cupsServed * factor;
        if (weight < 1) {
            return "0" + df.format(weight);
        }
        return df.format(weight);
    }

    public boolean checkIsValid(String date, String name) {
        for (CoffeeElement element: listData) {
            if (element.getDate().equals(date) && element.getName().equals(name)) {
                toastMessage("Entry for the day already exists");
                return false;
            }
        }
        return true;
    }

    public void pushSomeData() {
        AddData("Friday, 04-06-2018", 3000, 30, "Hayes Valley");
        AddData("Saturday, 10-20-2018", 100, 900, "Hayes Valley");
        AddData("Sunday, 10-21-2018", 100, 123, "Hayes Valley");
        AddData("Monday, 10-22-2018", 100, 111, "Hayes Valley");
        AddData("Tuesday, 10-23-2018", 100, 900, "Hayes Valley");
        AddData("Wednesday, 10-24-2018", 100, 123, "Hayes Valley");
        AddData("Thursday, 10-25-2018", 100, 111, "Hayes Valley");
        AddData("Friday, 10-26-2018", 100, 900, "Hayes Valley");
        AddData("Saturday, 10-27-2018", 100, 123, "Hayes Valley");
        AddData("Sunday, 10-28-2018", 100, 111, "Hayes Valley");
        AddData("Monday, 10-29-2018", 100, 900, "Hayes Valley");
        AddData("Tuesday, 10-30-2018", 100, 123, "Hayes Valley");
        AddData("Wednesday, 10-31-2018", 100, 111, "Hayes Valley");
        AddData("Thursday, 11-01-2018", 100, 900, "Hayes Valley");
        AddData("Friday, 11-02-2018", 100, 123, "Hayes Valley");
        AddData("Saturday, 11-03-2018", 100, 111, "Hayes Valley");
        AddData("Sunday, 11-04-2018", 100, 900, "Hayes Valley");
        AddData("Monday, 11-05-2018", 100, 200, "Hayes Valley");
        AddData("Tuesday, 11-06-2018", 100, 189, "Hayes Valley");
        AddData("Wednesday, 11-07-2018", 100, 789, "Hayes Valley");
        AddData("Thursday, 11-08-2018", 100, 123, "Hayes Valley");
        AddData("Friday, 11-09-2018", 100, 678, "Hayes Valley");
        AddData("Saturday, 11-10-2018", 100, 567, "Hayes Valley");
        AddData("Sunday, 11-11-2018", 100, 345, "Hayes Valley");
        AddData("Monday, 11-12-2018", 100, 234, "Hayes Valley");
        AddData("Tuesday, 11-13-2018", 100, 456, "Hayes Valley");
        AddData("Wednesday, 11-14-2018", 100, 123, "Hayes Valley");
        AddData("Thursday, 11-15-2018", 100, 111, "Hayes Valley");
        AddData("Friday, 11-16-2018", 100, 9, "Hayes Valley");
        AddData("Saturday, 11-17-2018", 100, 90, "Hayes Valley");
        AddData("Sunday, 11-18-2018", 100, 60, "Hayes Valley");
        AddData("Monday, 11-19-2018", 100, 900, "Hayes Valley");
        AddData("Tuesday, 11-20-2018", 100, 123, "Hayes Valley");
        AddData("Wednesday, 11-21-2018", 100, 111, "Hayes Valley");
        AddData("Thursday, 11-22-2018", 100, 900, "Hayes Valley");
        AddData("Friday, 11-23-2018", 100, 123, "Hayes Valley");
        AddData("Saturday, 11-24-2018", 100, 13, "Hayes Valley");
        AddData("Sunday, 11-25-2018", 100, 600, "Hayes Valley");
        AddData("Monday, 11-26-2018", 100, 3, "Hayes Valley");
        AddData("Tuesday, 11-27-2018", 100, 1000, "Hayes Valley");
        AddData("Wednesday, 11-28-2018", 100, 900, "Hayes Valley");
        AddData("Thursday, 11-29-2018", 100, 789, "Hayes Valley");
        AddData("Friday, 11-30-2018", 100, 456, "Hayes Valley");
        AddData("Saturday, 12-01-2018", 100, 300, "Hayes Valley");
        AddData("Sunday, 12-02-2018", 100, 200, "Hayes Valley");
        AddData("Monday, 12-03-2018", 100, 500, "Hayes Valley");
        AddData("Tuesday, 12-04-2018", 100, 400, "Hayes Valley");
        AddData("Wednesday, 12-05-2018", 100, 300, "Hayes Valley");
        AddData("Thursday, 12-06-2018", 100, 200, "Hayes Valley");
        AddData("Friday 12-07-2018", 100, 100, "Hayes Valley");
        AddData("Saturday, 12-08-2018", 100, 800, "Hayes Valley");
        AddData("Sunday, 12-09-2018", 1, 200, "Hayes Valley");
        AddData("Monday, 12-10-2018", 100, 500, "Hayes Valley");
        AddData("Tuesday, 12-11-2018", 200, 600, "Hayes Valley");
        AddData("Wednesday, 12-12-2018", 300, 700, "Hayes Valley");
        AddData("Thursday, 12-13-2018", 400, 800, "Hayes Valley");
        AddData("Friday, 12-14-2018", 1000, 10, "Hayes Valley");
        AddData("Saturday, 12-15-2018", 2000, 20, "Hayes Valley");
        AddData("Sunday, 12-16-2018", 3000, 30, "Hayes Valley");

        AddData("Saturday, 07-21-2018", 70, 20, "Blend");
        AddData("Sunday, 07-22-2018", 70, 93, "Blend");
        AddData("Monday, 07-23-2018", 70, 500, "Blend");
        AddData("Tuesday, 07-24-2018", 70, 20, "Blend");
        AddData("Wednesday, 07-25-2018", 70, 93, "Blend");
        AddData("Thursday, 07-26-2018", 70, 500, "Blend");
        AddData("Friday, 07-27-2018", 70, 20, "Blend");
        AddData("Saturday, 07-28-2018", 70, 93, "Blend");
        AddData("Sunday, 07-29-2018", 70, 500, "Blend");
        AddData("Monday, 07-30-2018", 70, 20, "Blend");
        AddData("Tuesday, 07-31-2018", 70, 93, "Blend");
        AddData("Wednesday, 08-01-2018", 70, 500, "Blend");
        AddData("Thursday, 08-02-2018", 70, 20, "Blend");
        AddData("Friday, 08-03-2018", 70, 93, "Blend");
        AddData("Saturday, 08-04-2018", 70, 500, "Blend");
        AddData("Sunday, 08-05-2018", 70, 20, "Blend");
        AddData("Monday, 08-06-2018", 70, 200, "Blend");
        AddData("Tuesday, 08-07-2018", 70, 189, "Blend");
        AddData("Wednesday, 08-08-2018", 70, 30, "Blend");
        AddData("Thursday, 08-09-2018", 70, 93, "Blend");
        AddData("Friday, 08-10-2018", 70, 678, "Blend");
        AddData("Saturday, 08-11-2018", 70, 567, "Blend");
        AddData("Sunday, 08-12-2018", 70, 345, "Blend");
        AddData("Monday, 08-13-2018", 70, 234, "Blend");
        AddData("Tuesday, 08-14-2018", 70, 456, "Blend");
        AddData("Wednesday, 08-15-2018", 70, 93, "Blend");
        AddData("Thursday, 08-16-2018", 70, 500, "Blend");
        AddData("Friday, 08-17-2018", 70, 900, "Blend");
        AddData("Saturday, 08-18-2018", 70, 90, "Blend");
        AddData("Sunday, 08-19-2018", 70, 60, "Blend");
        AddData("Monday, 08-20-2018", 70, 20, "Blend");
        AddData("Tuesday, 08-21-2018", 70, 93, "Blend");
        AddData("Wednesday, 08-22-2018", 70, 500, "Blend");
        AddData("Thursday, 08-23-2018", 70, 20, "Blend");
        AddData("Friday, 08-24-2018", 70, 93, "Blend");
        AddData("Saturday, 08-25-2018", 70, 13, "Blend");
        AddData("Sunday, 08-26-2018", 70, 350, "Blend");
        AddData("Monday, 08-27-2018", 70, 3, "Blend");
        AddData("Tuesday, 08-28-2018", 70, 700, "Blend");
        AddData("Wednesday, 08-29-2018", 70, 20, "Blend");
        AddData("Thursday, 08-30-2018", 70, 30, "Blend");
        AddData("Friday, 08-31-2018", 70, 456, "Blend");
        AddData("Saturday, 09-01-2018", 70, 300, "Blend");
        AddData("Sunday, 09-02-2018", 70, 200, "Blend");
        AddData("Monday, 09-03-2018", 70, 250, "Blend");
        AddData("Tuesday, 09-04-2018", 70, 400, "Blend");
        AddData("Wednesday, 09-05-2018", 70, 300, "Blend");
        AddData("Thursday, 09-06-2018", 70, 200, "Blend");
        AddData("Friday 09-07-2018", 70, 70, "Blend");
        AddData("Saturday, 09-08-2018", 70, 07, "Blend");
        AddData("Sunday, 09-09-2018", 1, 200, "Blend");
        AddData("Monday, 09-10-2018", 70, 500, "Blend");
        AddData("Tuesday, 09-11-2018", 200, 600, "Blend");
        AddData("Wednesday, 09-12-2018", 300, 700, "Blend");
        AddData("Thursday, 09-13-2018", 400, 80, "Blend");
        AddData("Friday, 09-14-2018", 700, 7, "Blend");
        AddData("Saturday, 09-15-2018", 2000, 20, "Blend");
        AddData("Sunday, 09-16-2018", 3000, 30, "Blend");
    }

    public void todayEntry() {
        ListView list = findViewById(R.id.coffeeListView);
        list.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        Cursor data = mDatabasehelper.getData();
        if (data.getCount() == 0) {
            return;
        }
        TextView date = findViewById(R.id.date);
        String dateStr = date.getText().toString();
        while (data.moveToNext()) {
            Log.d(TAG, "OBJECT: "+ data.getString(1));
            Log.d(TAG, "DATE: " + dateStr);
            Log.d(TAG, "" + data.getString(1).equals(dateStr));
            if (data.getString(1).equals(dateStr)) {
                listData.add(0, new CoffeeElement(data.getString(1), Integer.parseInt(data.getString(2)),
                        Double.parseDouble(data.getString(3)), data.getString(4)));
            }
        }
    }

    public void AddData(String date, int cups_sold, double weight, String name) {
        boolean insertData = mDatabasehelper.addCoffeeType(date, cups_sold, weight, name);
        if (insertData) {
            toastMessage("Data successfully inserted");
        } else {
            toastMessage("Failed to Insert");
        }
    }

    public void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String setDate() {
        Date now = new Date();
        SimpleDateFormat sdf2 = new SimpleDateFormat("EEEE");
        String day = sdf2.format(new Date());
        String time = new SimpleDateFormat("MM-dd-yyyy", Locale.US).format(now);
        String dbTime = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now);
        TextView textView = findViewById(R.id.date);
        textView.setText(day +", " + time);
        return day +", " + time;
    }

}
