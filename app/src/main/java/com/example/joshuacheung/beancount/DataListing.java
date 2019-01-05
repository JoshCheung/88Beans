package com.example.joshuacheung.beancount;

import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class DataListing extends AppCompatActivity {

    public static final String TAG = "DATA_LISTING";
    DatabaseHelper mDatabasehelper;
    ArrayAdapter<CoffeeElement> adapter;
    ArrayList<CoffeeElement> dataList = new ArrayList<>();

    ExpandableListView expandableListView;
    ExpandableListAdapter expandableListAdapter;
    List<Date> expandableListTitle;
    SortedMap<Date, List<CoffeeElement>> expandableListDetail;
    public static String title = "";

    ArrayList<String> mNames = new ArrayList<>();
    public void getNames() {
        mNames.add("Hayes Valley");
        mNames.add("Blend");
        mNames.add("Single Origin Espresso");
        mNames.add("Decaf");
        mNames.add("Single Origin Drip");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mDatabasehelper = new DatabaseHelper(this);
        super.onCreate(savedInstanceState);
        getNames();
        try {
            Intent intent = getIntent();
            this.overridePendingTransition(R.anim.slide_in_left,
                    R.anim.slide_out_left);
            title = intent.getStringExtra("type");

            if (!title.equals("88 Beans")) {
                setContentView(R.layout.data_listing);
                FloatingActionButton backButton = findViewById(R.id.back);
                backButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(DataListing.this, FillTable.class);
                        intent.putExtra("title", title);
                        finish();
                        ActivityOptions options = ActivityOptions.makeCustomAnimation(DataListing.this, R.anim.slide_in_right, R.anim.slide_out_right);
                        startActivity(intent, options.toBundle());

                    }
                });
                adapter = new DataListing.ListAdapter();
                displayData(title);
            }
            else {
                setContentView(R.layout.expandable_listview);
                FloatingActionButton backButton = findViewById(R.id.back);
                backButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(DataListing.this, MainActivity.class);
                        ActivityOptions options = ActivityOptions.makeCustomAnimation(DataListing.this, R.anim.slide_in_right, R.anim.slide_out_right);
                        startActivity(intent, options.toBundle());
                    }
                });


                expandableListView = (ExpandableListView) findViewById(R.id.expandableListView);

                expandableListDetail = getData();

                expandableListTitle = new ArrayList<>(expandableListDetail.keySet());
                expandableListAdapter = new CustomExpandableListAdapter(this, expandableListTitle, expandableListDetail);
                expandableListView.setAdapter(expandableListAdapter);
                expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

                    @Override
                    public void onGroupExpand(int groupPosition) {
                        Toast.makeText(getApplicationContext(),
                                expandableListTitle.get(groupPosition) + " List Expanded.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                expandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {

                    @Override
                    public void onGroupCollapse(int groupPosition) {
                        Toast.makeText(getApplicationContext(),
                                expandableListTitle.get(groupPosition) + " List Collapsed.",
                                Toast.LENGTH_SHORT).show();

                    }
                });

                expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                    @Override
                    public boolean onChildClick(ExpandableListView parent, View v,
                                                int groupPosition, int position, long id) {
                        Vibrator vibe = (Vibrator) DataListing.this.getSystemService(Context.VIBRATOR_SERVICE);
                        vibe.vibrate(30);
                        final Dialog dialog = new Dialog(DataListing.this);
                        dialog.setContentView(R.layout.edit_or_delete_instance);
                        Button update = dialog.findViewById(R.id.update);
                        Button delete = dialog.findViewById(R.id.delete);
                        Spinner spinner = (Spinner) dialog.findViewById(R.id.spinner);
                        final DatePicker datePicker = (DatePicker) dialog.findViewById(R.id.Datepick);

                        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(
                                DataListing.this, R.layout.spinner_item, mNames);
                        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(myAdapter);
                        // get the ArrayList
                        spinner.setSelection(expandableListTitle.get(groupPosition).toString().indexOf(expandableListDetail.get(
                                expandableListTitle.get(groupPosition)).get(position).getName()));

                        EditText cups = (EditText) dialog.findViewById(R.id.cups_served);

                        cups.setText(String.valueOf(expandableListDetail.get(
                                expandableListTitle.get(groupPosition)).get(
                                position).getCupsSold()));

                        TextView weightOutput = dialog.findViewById(R.id.weight_output);

                        double oldWeight = expandableListDetail.get(
                                expandableListTitle.get(groupPosition)).get(
                                position).getWeight();

                        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MM-dd-yyyy");
                        Date oldDate = new Date();
                        try{
                            oldDate = dateFormat.parse(expandableListDetail.get(
                                    expandableListTitle.get(groupPosition)).get(
                                    position).getDate());
                        }
                        catch (Exception e) {
                            toastMessage("Could not convert Date");
                        }

                        LocalDate localDate = oldDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        int year  = localDate.getYear();
                        int month = (localDate.getMonthValue()-1);
                        int day   = localDate.getDayOfMonth();

                        datePicker.updateDate(year, month, day);

                        weightOutput.setText(String.format(String.valueOf(oldWeight)));

                        String oldName = expandableListDetail.get(
                                expandableListTitle.get(groupPosition)).get(
                                position).getName();

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

                                    toastMessage("Item was clicked for updating: " + itemID +", " + expandableListDetail.get(
                                            expandableListTitle.get(groupPosition)).get(
                                            position).toString());

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
                                    int cups_sold = Integer.parseInt(cups.getText().toString());
                                    String newName = spinner.getSelectedItem().toString();
                                    double weight = Double.parseDouble(weightOutput.getText().toString());
                                    mDatabasehelper.updateItem(itemID, oldName, entryDate, cups_sold, weight, newName);
                                    expandableListDetail.get(expandableListTitle.get(groupPosition))
                                            .set(position, new CoffeeElement(entryDate, cups_sold, weight, newName));
                                } else {
                                    toastMessage("No ID associated with that name");
                                }
                                dialog.dismiss();
                                finish();
                                startActivity(getIntent());
                            }
                        });

                        delete.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                toastMessage("Item was clicked: " + expandableListDetail.get(
                                        expandableListTitle.get(groupPosition)).get(position).getName());
                                String name = expandableListDetail.get(
                                        expandableListTitle.get(groupPosition)).get(position).getName();
                                Log.d("ITEM CLICKED", "onItemClicked: you clicked on: " + name);

                                Cursor data = mDatabasehelper.getItemID(name);
                                int itemID = -1;
                                while (data.moveToNext()) {
                                    itemID = data.getInt(0);
                                }
                                if (itemID > -1) {
                                    Log.d("Item clicked", "onItemCLick: the ID is " + itemID);
                                    toastMessage("Item was clicked for deletion: " + itemID +", " + expandableListDetail.get(
                                            expandableListTitle.get(groupPosition)).get(position).getName());
                                    mDatabasehelper.deleteName(itemID, name);
                                    expandableListDetail.get(
                                            expandableListTitle.get(groupPosition)).remove(position);

                                } else {
                                    toastMessage("No ID associated with that name");
                                }
                                dialog.dismiss();
                                finish();
                                startActivity(getIntent());
                            }
                        });
                        dialog.show();
                        return false;
                    }
                });
            }
        }
        catch (Exception e) {
            toastMessage("No Data Available");
        }
    }

    public SortedMap<Date, List<CoffeeElement>> getData() throws Exception {

        SortedMap<Date, List<CoffeeElement>> expandableListDetail = new TreeMap<Date, List<CoffeeElement>>(Collections.reverseOrder());
        Cursor data = mDatabasehelper.getData();
        if (data.getCount() == 0) {
            return null;
        }
        while (data.moveToNext()) {
            SimpleDateFormat format=new SimpleDateFormat("MM-dd-yyyy");

            String preDate = data.getString(1);
            String convertDate = preDate.substring(preDate
                    .lastIndexOf(" ")).replace(" ", "");
            Date date1 = format.parse(convertDate);

            if (!expandableListDetail.containsKey(date1)) {
                expandableListDetail.put(date1, new ArrayList<>());
                expandableListDetail.get(date1).add(new CoffeeElement(data.getString(1), Integer.parseInt(data.getString(2)),
                        Double.parseDouble(data.getString(3)), data.getString(4)));
            }
            else {
                expandableListDetail.get(date1).add(new CoffeeElement(data.getString(1), Integer.parseInt(data.getString(2)),
                        Double.parseDouble(data.getString(3)), data.getString(4)));
            }
        }
        return expandableListDetail;
    }


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


    private class ListAdapter extends ArrayAdapter<CoffeeElement> {
        public ListAdapter() {
            super(DataListing.this, R.layout.coffee_element, R.id.dataListView, dataList);
        } //Change according to Database
        public View getView(final int position, View convertView, ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = getLayoutInflater().inflate(R.layout.coffee_element, null);
            }
            //Set Coffee Title

            TextView date = itemView.findViewById(R.id.dateOutput);
            TextView cups = itemView.findViewById(R.id.cups_served);
            TextView weight = itemView.findViewById(R.id.weight_output);

            date.setText(dataList.get(position).getDate());
            cups.setText(dataList.get(position).getCupsSold()+"");
            weight.setText(String.format("%.2f lb", dataList.get(position).getWeight()));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Vibrator vibe = (Vibrator) DataListing.this.getSystemService(Context.VIBRATOR_SERVICE);
                    vibe.vibrate(30);
                    final Dialog dialog = new Dialog(DataListing.this);
                    dialog.setContentView(R.layout.edit_or_delete_instance);
                    Button update = dialog.findViewById(R.id.update);
                    Button delete = dialog.findViewById(R.id.delete);
                    Spinner spinner = (Spinner) dialog.findViewById(R.id.spinner);
                    final DatePicker datePicker = (DatePicker) dialog.findViewById(R.id.Datepick);
                    ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(
                            DataListing.this, R.layout.spinner_item, mNames);
                    myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(myAdapter);
                    spinner.setSelection(dataList.indexOf(dataList.get(position).getName()));

                    EditText cups = (EditText) dialog.findViewById(R.id.cups_served);
                    cups.setText(String.valueOf(dataList.get(position).getCupsSold()));

                    TextView weightOutput = dialog.findViewById(R.id.weight_output);
                    double oldWeight = dataList.get(position).getWeight();

                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MM-dd-yyyy");
                    Date oldDate = new Date();

                    try{
                        oldDate = dateFormat.parse(dataList.get(position).getDate());
                    }
                    catch (Exception e) {
                        toastMessage("Could not convert Date");
                    }

                    LocalDate localDate = oldDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    int year  = localDate.getYear();
                    int month = (localDate.getMonthValue()-1);
                    int day   = localDate.getDayOfMonth();

                    datePicker.updateDate(year, month, day);

                    weightOutput.setText(String.format(String.valueOf(oldWeight)));
                    String oldName = dataList.get(position).getName();

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
                                toastMessage("Item was clicked for updating: " + itemID +", " + dataList.get(position).toString());
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
                                int cups_sold = Integer.parseInt(cups.getText().toString());
                                String newName = spinner.getSelectedItem().toString();
                                double weight = Double.parseDouble(weightOutput.getText().toString());
                                mDatabasehelper.updateItem(itemID, oldName, entryDate, cups_sold, weight, newName);
                                dataList.set(position, new CoffeeElement(entryDate, cups_sold, weight, newName));
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
                            toastMessage("Item was clicked: " + dataList.get(position).getName());
                            String name = dataList.get(position).getName();
                            Log.d("ITEM CLICKED", "onItemClicked: you clicked on: " + name);

                            Cursor data = mDatabasehelper.getItemID(name);
                            int itemID = -1;
                            while (data.moveToNext()) {
                                itemID = data.getInt(0);
                            }
                            if (itemID > -1) {
                                Log.d("Item clicked", "onItemCLick: the ID is " + itemID);
                                toastMessage("Item was clicked for deletion: " + itemID +", " + dataList.get(position).getName());
                                mDatabasehelper.deleteName(itemID, name);
                                dataList.remove(position);
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



    public String calculateFactor(int cupFactor, int cupsServed) {
        Format df = new DecimalFormat("#.00");
        double factor = cupFactor * 1.3/453.5;
        double weight = cupsServed * factor;
        if (weight < 1) {
            return "0" + df.format(weight);
        }
        return df.format(weight);
    }

    public void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void displayData(String title) {
        TextView listings = findViewById(R.id.dataListTitle);
        listings.setText(title + ": All Entries");
        ListView list = (ListView) findViewById(R.id.dataListView);
        list.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        Cursor data = mDatabasehelper.getData();
        if (data.getCount() == 0) {
            return;
        }
        while (data.moveToNext()) {
            if (data.getString(4).equals(title)) {
                dataList.add(new CoffeeElement(data.getString(1), Integer.parseInt(data.getString(2)),
                        Double.parseDouble(data.getString(3)), data.getString(4)));
            }
        }
        Collections.sort(dataList, CoffeeElement.DateComparison);
    }
}
