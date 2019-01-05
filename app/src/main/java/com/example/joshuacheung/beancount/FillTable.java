package com.example.joshuacheung.beancount;

import android.database.Cursor;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.List;

public class FillTable extends AppCompatActivity {

    public String title = "";
    public static final String TAG = "FILL_TABLE";
    public DatabaseHelper mDatabasehelper;

    public int totalCups = 0;
    public double totalWeight = 0.0;

    public ArrayList <CoffeeElement> coffeeData = new ArrayList<>();

    public HashMap <String, ArrayList> weeks = new HashMap<>();

    public ArrayList<String> mNames = new ArrayList<>();

    // Plot data points
    public LineGraphSeries<DataPoint> series1;

    public static double weeklyAverage = 0.0;

    // Store the dates of each week
    public ArrayList<String> dates = new ArrayList<>();

    // determines what week is being looked at
    public static int weekIndex;

    private static class DateWeightPair {

        private String date;
        private String name;
        private double weight;

        public DateWeightPair(String name, String date, double weight) {
            this.name = name;
            this.date = date;
            this.weight = weight;
        }

        public String getName() {
            return name;
        }

        public String getDate() {
            return date;
        }

        public double getWeight() {
            return weight;
        }

        public String toString() {
            return getName() + ", " + getDate() + ": " + getWeight();
        }


        public static Comparator<DateWeightPair> DateComparison = new Comparator<DateWeightPair>() {

            public int compare(DateWeightPair d1, DateWeightPair d2) {
                String date1 = d1.getDate().toUpperCase();
                String date2 = d2.getDate().toUpperCase();

//              descending order
                return date2.compareTo(date1);
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_layout);
        FloatingActionButton homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(FillTable.this, MainActivity.class);
                startActivity(myIntent);
            }
        });
        mDatabasehelper = new DatabaseHelper(this);
        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        setTitle(title);
        filter(title);
        getWeekly();
        fillWeeklyTable(weekIndex, title);
        graphWeek(weekIndex, title);
        pastYearToDate();
        getNames();
        initRecyclerView();
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

    /*
        * Filters through all data in the table
        * appends the ones with the same name as the title
        * to a list
     */
    public void filter(String name) {
        Cursor data = mDatabasehelper.getData();
        while (data.moveToNext()) {
            if (name.equals(data.getString(4))) {
                coffeeData.add(
                        new CoffeeElement(data.getString(1),
                        Integer.parseInt(data.getString(2)),
                        Double.parseDouble(data.getString(3)),
                        data.getString(4)));

                totalCups += Integer.parseInt(data.getString(2));
                totalWeight += Double.parseDouble(data.getString(3));
            }
        }
    }

    /*
        * On click for past Month
        * Set title
        * Displays line graph
     */
    public void monthOnClick(View view) throws Exception {
        int months = -1;
        TextView title = (TextView) findViewById(R.id.graphTitle);
        title.setText("Past Month");
        graphMonths(months);
    }

    /*
        * On click for past 6 months button
        * Set title
        * Displays line graph
     */

    public void sixMonthOnClick(View view) throws Exception {
        int months = -6;
        TextView title = (TextView) findViewById(R.id.graphTitle);
        title.setText("Past 6 Months");
        graphMonths(months);
    }

    /*
        * On click for past year button
        * Set title
        * Displays line graph
     */
    public void pastYear(View view) throws Exception {
        int months = -12;
        TextView title = (TextView) findViewById(R.id.graphTitle);
        title.setText("Past Year");
        graphMonths(months);
    }

    /*
        * Finds the past months for each button by date
     */
    public ArrayList getPastMonths(int num) throws ParseException {

        ArrayList<DateWeightPair> pastMonth = new ArrayList<>();

        Calendar currentDateBefore6Month = Calendar.getInstance();
        currentDateBefore6Month.add(Calendar.MONTH, num);

        Log.d(TAG, currentDateBefore6Month.getTime().toString());

        //Start at the most recent date which is at the end of list
        for (int idx = coffeeData.size()-1; idx >= 0; idx--) {
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(convertDisplayDateToLocalDate(coffeeData.get(idx).getDate()));

            // comparison for date for previous month and check for exact day
            if (date.after(currentDateBefore6Month.getTime())) {
                pastMonth.add(new DateWeightPair(coffeeData.get(idx).getName(), coffeeData.get(idx).getDate(), coffeeData.get(idx).getWeight()));
            }
            else {
                continue;
            }
        }
        return pastMonth;
    }

    /*
        * Displays the line graph for
        * Month, 6 Months, and Past Year
     */
    public void graphMonths(int num) throws Exception {
        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.removeAllSeries();
        series1 = new LineGraphSeries<>();

        ArrayList<DateWeightPair> pastMonth = getPastMonths(num);
        Collections.reverse(pastMonth);
//        try {
            // Loop through and initialize all data points
            for (int i = pastMonth.size()-1; i >= 0 ; i--) {
                String date = convertDisplayDateToLocalDate(pastMonth.get(i).getDate());
                Date date1 = new SimpleDateFormat("yyyy-MM-dd").parse(date);
                series1.appendData(new DataPoint(date1.getTime(), pastMonth.get(i).getWeight()), true, 31 * Math.abs(num));
            }

            // Rendering
            graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(FillTable.this));

            // Getting the starting date
            String start = convertDisplayDateToLocalDate(pastMonth.get(0).getDate());
            Date date2 = new SimpleDateFormat("yyyy-MM-dd").parse(start);

            // Getting ending date
            int size = pastMonth.size()-1;
            String end = convertDisplayDateToLocalDate(pastMonth.get(size).getDate());
            Date date1 = new SimpleDateFormat("yyyy-MM-dd").parse(end);

            // Set timeline on LineGraph
            graph.getViewport().setMinX(date1.getTime());
            graph.getViewport().setMaxX(date2.getTime());

            graph.getViewport().setScrollable(true); // enables horizontal scrolling
            graph.getViewport().setScrollableY(true); // enables vertical scrolling

            // enables horizontal zooming and scrolling
            graph.getViewport().setScalable(true);
            graph.getViewport().setScalableY(true);

            graph.getViewport().setXAxisBoundsManual(true);
            series1.setDrawDataPoints(true);
            series1.setDataPointsRadius(10);
            series1.setThickness(8);
            series1.setColor(Color.argb(255, 1, 161, 221));

            // Tapping on Data Points
            series1.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {

                    DateFormat simple = new SimpleDateFormat("MM-dd-yyyy");
                    Date date = new Date((long) dataPoint.getX());

                    //printing value of Date
                    toastMessage(simple.format(date) +": " + dataPoint.getY() +"lb");
                }
            });
            graph.addSeries(series1);
//        }
//        catch (Exception e) {
//            toastMessage("No Data available");
//        }
    }


    /*
       Displays the totals for each month for the past year
    */
    public void pastYearToDate() {
        // Get the total for the year
        double yearLongTotal = 0.0;

        // initialize 12 buckets for each month
        double [] months = new double [13];

        // Track from today's date
        LocalDate now = LocalDate.now();
        String [] toDate = now.toString().split("-");
        int yyyy = Integer.parseInt(toDate[0]);

        // Loop through data
        for (CoffeeElement element : coffeeData) {
            String[] stripExtra = convertDisplayDateToLocalDate(element.getDate()).split("-");

            // Check the month and year
            if (Integer.parseInt(stripExtra[0]) == yyyy - 1 && toDate[1].equals(stripExtra[1]) || Integer.parseInt(stripExtra[0]) < yyyy - 1) {
                // break when the month is the same and year is pervious
                break;
            } else {
                // add to the appropriate buckets
                yearLongTotal += element.getWeight();
                int month = Integer.parseInt(stripExtra[1]);
                switch (month) {
                    case 1:
                        months[month] += element.getWeight();
                        break;
                    case 2:
                        months[month] += element.getWeight();
                        break;
                    case 3:
                        months[month] += element.getWeight();
                        break;
                    case 4:
                        months[month] += element.getWeight();
                        break;
                    case 5:
                        months[month] += element.getWeight();
                        break;
                    case 6:
                        months[month] += element.getWeight();
                        break;
                    case 7:
                        months[month] += element.getWeight();
                        break;
                    case 8:
                        months[month] += element.getWeight();
                        break;
                    case 9:
                        months[month] += element.getWeight();
                        break;
                    case 10:
                        months[month] += element.getWeight();
                        break;
                    case 11:
                        months[month] += element.getWeight();
                        break;
                    case 12:
                        months[month] += element.getWeight();
                        break;
                }
            }
        }
    }


    /*
        * Get the weekly data for everything for
        * the titled Coffee
     */
    public void getWeekly() {

        //start at the end of the list to get first value's weeks
        for (int idx = coffeeData.size()-1; idx >= 0; idx--) {

            // Separate each part of the date so it is readable to LocalDate
            String [] stripExtra =  convertDisplayDateToLocalDate(coffeeData.get(idx).getDate()).split("-");
            int mm = Integer.parseInt(stripExtra[1]);
            int dd = Integer.parseInt(stripExtra[2]);
            int yyyy = Integer.parseInt(stripExtra[0]);

            // Get the name for checking in other methods
            String inputName = coffeeData.get(idx).getName();

            // Date to be put into a new List
            String inputDate = convertDisplayDateToLocalDate(coffeeData.get(idx).getDate());

            //Weight to be put into DateWeightPair element
            double weight = coffeeData.get(idx).getWeight();

            // Calculate the mondays of the date
            LocalDate monday =
                    LocalDate.of(yyyy, mm, dd)
                            .with(TemporalAdjusters.previousOrSame( DayOfWeek.MONDAY ) ) ;

            // Caluculate the Sundays of the date
            LocalDate sunday = LocalDate.of(yyyy, mm, dd)
                    .with(TemporalAdjusters.nextOrSame( DayOfWeek.SUNDAY ) ) ;

            // Create a String as a key for the dates
//            String displayMon = convertDateText(monday.toString());
//            String displaySun = convertDateText(sunday.toString());
            String key = monday.toString()  + " to " + sunday.toString();
//            String key = displayMon + " to " + displaySun;

            // If the Dates do not exist, push it to the table and add the element to the list
            if (!weeks.containsKey(key)) {
                ArrayList<DateWeightPair> pairs = new ArrayList<>();
                weeks.put(key, pairs);
                dates.add(key);
                weeks.get(key).add(new DateWeightPair(inputName, inputDate, weight));
            }
            else {
                weeks.get(key).add(new DateWeightPair(inputName, inputDate, weight));
            }
        }

        // update the week index
        weekIndex = dates.size()-1;

        // sort the Dates list in chronological order
        Collections.sort(dates);
//        dates.stream().forEach(x -> Log.d(TAG, "DATES: " + x));
    }


    /*
        * Small conversion from 'YYYY-MM-DD' to 'MM-DD-YYYY'
     */
    public String convertDateText(String localDate) {
        String [] arr = localDate.split("-");
        return arr[1] + "-" + arr[2] + "-" + arr[0];
    }

    // Date conversion from 'MM-DD-YYYY' to 'YYYY-MM-DD'
    public String convertDisplayDateToLocalDate(String date) {
        String convertDate = date.substring(date
                        .lastIndexOf(" ")).replace(" ", "");
        String [] convert = convertDate.split("-");
        return convert[2] + "-" + convert[0] + "-" + convert[1];
    }


    public void toastMessage(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        v.setTextColor(Color.argb(255, 1, 161, 221));
        toast.show();
    }


    /*
        *Selects previous week of what is currently being looked at
     */
    public void previous(View view) {

        if (weekIndex <= 0) {
            toastMessage("No More Data for Previous Weeks");
        }
        else {
            TextView graphTitle = (TextView) findViewById(R.id.graphTitle);
            graphTitle.setText("Past Weeks");
            weekIndex--;
//            Log.d(TAG, "WeekIndex: " + weekIndex);
//            Log.d(TAG, dates.get(weekIndex));
            fillWeeklyTable(weekIndex, title);
            graphWeek(weekIndex, title);
        }
    }


    /*
        * Selects next week of what is currently being looked at
     */
    public void next(View view) {
        if (weekIndex >= dates.size()-1) {
            toastMessage("No More Data for future Weeks");
        }
        else {
            TextView graphTitle = (TextView) findViewById(R.id.graphTitle);
            graphTitle.setText("Past Weeks");
            weekIndex++;
//            Log.d(TAG, "WeekIndex: " + weekIndex);
//            Log.d(TAG, dates.get(weekIndex));
            fillWeeklyTable(weekIndex, title);
            graphWeek(weekIndex, title);

        }
    }

    public void showEntries(View view) {
        Intent showEntries = new Intent(this, DataListing.class);
        showEntries.putExtra("type", getTitle());
        startActivity(showEntries);
    }

    /*
        * Fill up the table of currently looked at week
     */
    public void fillWeeklyTable(int index, String title) {
        // used for caluculating the weight for the entire week
        double weight = 0.0;

        // initialize week values for TextView
        TextView weekText = findViewById(R.id.weekText);
        TextView mon = findViewById(R.id.monOutput);
        TextView tues = findViewById(R.id.tuesOutput);
        TextView wed = findViewById(R.id.wedOutput);
        TextView thur = findViewById(R.id.thurOutput);
        TextView fri = findViewById(R.id.friOutput);
        TextView sat = findViewById(R.id.satOutput);
        TextView sun = findViewById(R.id.sunOutput);
        TextView weightTotal = findViewById(R.id.totalWeight);

        mon.setText("");
        tues.setText("");
        wed.setText("");
        thur.setText("");
        fri.setText("");
        sat.setText("");
        sun.setText("");

        // get the current week at the index
        try {
            String currentWeek = dates.get(index);
            Log.d(TAG, "Current week: " + currentWeek);
            String [] format = currentWeek.split(" ");
            String start = convertDateText(format[0]);
            String end = convertDateText(format[2]);
            //get arrayList of Date and weight pairs
            ArrayList<DateWeightPair> pairs = weeks.get(currentWeek);
            pairs.stream().forEach(x -> Log.d(TAG, "Dates: " + x.getDate()));

            // For each pair in the given array
            for (DateWeightPair pair : pairs) {
                if (pair.getName().equals(title)) {
                    weight += pair.getWeight();
                    LocalDate event = LocalDate.parse(pair.getDate());

                    // Set the TextView according to which day they fall on
                    weekText.setText(String.format("%s %s %s", start, format[1], end));
                    switch(event.getDayOfWeek()) {
                        case MONDAY:
                            mon.setText(pair.getWeight() + "lb");
                            break;
                        case TUESDAY:
                            tues.setText(pair.getWeight() + "lb");
                            break;
                        case WEDNESDAY:
                            wed.setText(pair.getWeight() + "lb");
                            break;
                        case THURSDAY:
                            thur.setText(pair.getWeight() + "lb");
                            break;
                        case FRIDAY:
                            fri.setText(pair.getWeight() + "lb");
                            break;
                        case SATURDAY:
                            sat.setText(pair.getWeight() + "lb");
                            break;
                        case SUNDAY:
                            sun.setText(pair.getWeight() + "lb");
                            break;
                        default:
                            break;
                    }
                }
            }
            // Display the total weight used for the week
            weightTotal.setText(String.format("%.2f lb", weight));
        }
        catch (Exception e) {
            toastMessage("No Data to Display");
        }
    }


    public void weeklyAverages() {
        double weight = 0.0;
        for (String date: dates) {
            ArrayList<DateWeightPair> pairs = weeks.get(date);
            weight += pairs.stream().mapToDouble(x -> x.getWeight()).sum();
        }
        Log.d(TAG, String.valueOf(weight/dates.size()));
        weeklyAverage = weight/dates.size();
//        return weight/dates.size();
    }


    public void weekOnClick(View view) throws Exception{
        TextView graphTitle = (TextView) findViewById(R.id.graphTitle);
        graphTitle.setText("Past Week");
        weekIndex = dates.size()-1;
        graphWeek(weekIndex, title);
    }

    public void graphWeek (int index, String title) {
        String [] daysOfTheWeek = {"Mon", "Tues", "Wed", "Thur", "Fri", "Sat", "Sun"};
        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.removeAllSeries();
        series1 = new LineGraphSeries<>();
        try {
            String currentWeek = dates.get(index);
            ArrayList<DateWeightPair> pairs = weeks.get(currentWeek);
            Collections.sort(pairs, DateWeightPair.DateComparison);

            for (int i = 1; i <= pairs.size(); i++) {
                if (pairs.get(i-1).getName().equals(title)) {
                    LocalDate event = LocalDate.parse(pairs.get(pairs.size()-i).getDate());
                    double weight = pairs.get(pairs.size() - i).getWeight();
                    switch(event.getDayOfWeek()) {
                        case MONDAY:
                            series1.appendData(new DataPoint(1, weight), true, 7);
                            break;
                        case TUESDAY:
                            series1.appendData(new DataPoint(2, weight), true, 7);
                            break;
                        case WEDNESDAY:
                            series1.appendData(new DataPoint(3, weight), true, 7);
                            break;
                        case THURSDAY:
                            series1.appendData(new DataPoint(4, weight), true, 7);
                            break;
                        case FRIDAY:
                            series1.appendData(new DataPoint(5, weight), true, 7);
                            break;
                        case SATURDAY:
                            series1.appendData(new DataPoint(6, weight), true, 7);
                            break;
                        case SUNDAY:
                            series1.appendData(new DataPoint(7, weight), true, 7);
                            break;
                        default:
                            break;
                    }
                }
            }

            StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
            staticLabelsFormatter.setHorizontalLabels(new String[] {"Mon", "Tues", "Wed", "Thur", "Fri", "Sat", "Sun"});
            graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

            graph.getViewport().setMinX(1);
            graph.getViewport().setMaxX(7);

            graph.getViewport().setScrollable(true); // enables horizontal scrolling
            graph.getViewport().setScrollableY(true); // enables vertical scrolling

            // enables horizontal zooming and scrolling
            graph.getViewport().setScalable(true);
            graph.getViewport().setScalableY(true);

            graph.getViewport().setXAxisBoundsManual(true);
            series1.setDrawDataPoints(true);
            series1.setDataPointsRadius(10);
            series1.setThickness(8);
            series1.setColor(Color.argb(255, 1, 161, 221));

            // Tapping on Data Points
            series1.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {

                    //printing value of Date
                    toastMessage(daysOfTheWeek[(int)dataPoint.getX() - 1] + ": "+dataPoint.getY() +"lb");
                }
            });
            graph.addSeries(series1);
        }
        catch (Exception e) {
            toastMessage("No Data Available");
        }
    }
}

