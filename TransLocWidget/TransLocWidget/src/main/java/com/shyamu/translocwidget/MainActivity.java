package com.shyamu.translocwidget;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SVBar;
import com.shyamu.translocwidget.bl.ArrivalTimeWidget;
import com.shyamu.translocwidget.bl.Utils;
import com.shyamu.translocwidget.fragments.WidgetListFragment;
import com.shyamu.translocwidget.rest.model.TransLocAgency;
import com.shyamu.translocwidget.rest.model.TransLocRoute;
import com.shyamu.translocwidget.rest.model.TransLocStop;
import com.shyamu.translocwidget.rest.service.ServiceGenerator;
import com.shyamu.translocwidget.rest.service.TransLocClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;

import static com.shyamu.translocwidget.bl.Utils.TransLocDataType.AGENCY;
import static com.shyamu.translocwidget.bl.Utils.TransLocDataType.ROUTE;
import static com.shyamu.translocwidget.bl.Utils.TransLocDataType.STOP;


public class MainActivity extends AppCompatActivity implements WidgetListFragment.OnFragmentInteractionListener {
    private static final String TAG = "MainActivity";
    private static ArrivalTimeWidget atw = new ArrivalTimeWidget();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_list);
        Intent intent = getIntent();

        Transition enterTrans = new Explode();
        getWindow().setEnterTransition(enterTrans);

        Transition returnTrans = new Explode();
        getWindow().setReturnTransition(returnTrans);

        if (intent.hasExtra("starting_fragment")) {
            String startingFragment = intent.getStringExtra("starting_fragment");
            if (startingFragment.equals("AddAgencyFragment")) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, new AddAgencyFragment())
                        .addToBackStack(null)
                        .commit();
            }
        } else if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new WidgetListFragment())
                    .addToBackStack(null)
                    .commit();
        }

        // Prevents user from pressing back to an empty activity
        getFragmentManager().addOnBackStackChangedListener(() -> {
            if (getFragmentManager().getBackStackEntryCount() == 0) finish();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_empty, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(ArrivalTimeWidget widget) {
        Toast.makeText(getApplicationContext(), "interacted fragment with id:" + widget
                .toString(), Toast.LENGTH_LONG).show();
    }

    private static void handleServiceErrors(Context context, Utils.TransLocDataType errorFrom, Throwable e) {
        Log.e(TAG, "error in getting list of " + errorFrom, e);
        StringBuilder sb = new StringBuilder();
        sb.append("Error in retreiving a list of ");
        String listOf = null;
        switch (errorFrom) {
            case AGENCY:
                listOf = "agencies.";
                break;
            case ROUTE:
                listOf = "routes.";
                break;
            case STOP:
                listOf = "stops.";
                break;
            case ARRIVAL:
                listOf = "arrivals.";
                break;
        }
        sb.append(listOf);
        sb.append(" Please try again later");

        Utils.showAlertDialog(context, "Error", sb.toString());
    }

    public static class AddAgencyFragment extends Fragment {

        private final String TAG = this.getTag();
        ListView agencyListView;
        ProgressBar progressBar;

        public AddAgencyFragment() {

        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            menu.clear();
            inflater.inflate(R.menu.menu_empty, menu);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_add_agency, container, false);
            setHasOptionsMenu(true);
            agencyListView = (ListView) rootView.findViewById(R.id.lvAgencyList);
            progressBar = (ProgressBar) getActivity().findViewById(R.id.pbLoading);
            agencyListView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            TransLocClient client =
                    ServiceGenerator.createService(TransLocClient.class,
                            Utils.BASE_URL,
                            getString(R.string.mashape_key),
                            null,
                            AGENCY);
            client.agencies()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::populateAgencyListView,
                            e -> {
                                handleServiceErrors(getActivity(), AGENCY, e);
                            }
                    );

            return rootView;
        }

        private void populateAgencyListView(List<TransLocAgency> agencies) {
            if (agencies != null && !agencies.isEmpty()) {
                ArrayAdapter<TransLocAgency> agencyArrayAdapter = new ArrayAdapter<TransLocAgency>(getActivity(), android.R.layout.simple_spinner_dropdown_item, agencies);
                agencyListView.setAdapter(agencyArrayAdapter);

                // Animate
                TranslateAnimation animate = new TranslateAnimation(agencyListView.getWidth(),0,0,0);
                animate.setDuration(250);
                animate.setFillAfter(true);
                agencyListView.startAnimation(animate);
                agencyListView.setVisibility(View.VISIBLE);

                // Set onclicklistener to open select routes fragment
                agencyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        TransLocAgency selectedAgency = (TransLocAgency) parent.getItemAtPosition(position);
                        atw.setAgencyID(Integer.toString(selectedAgency.agencyId));
                        atw.setAgencyLongName(selectedAgency.longName);

                        // Insert the fragment by replacing any existing fragment
                        FragmentManager fragmentManager = getActivity().getFragmentManager();
                        fragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                                .replace(R.id.container, new AddRouteFragment())
                                .addToBackStack(null)
                                .commit();
                    }
                });
            } else {
                Log.e(TAG, "Agencies data was null or empty!");
            }
        }
    }

    public static class AddRouteFragment extends Fragment {

        private final String TAG = this.getTag();
        ListView routeListView;
        ProgressBar progressBar;

        public AddRouteFragment() {

        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            menu.clear();
            inflater.inflate(R.menu.menu_empty, menu);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_add_route, container, false);
            setHasOptionsMenu(true);
            routeListView = (ListView) rootView.findViewById(R.id.lvRouteList);
            progressBar = (ProgressBar) getActivity().findViewById(R.id.pbLoading);
            progressBar.setVisibility(View.VISIBLE);
            TransLocClient client =
                    ServiceGenerator.createService(TransLocClient.class,
                            Utils.BASE_URL,
                            getString(R.string.mashape_key),
                            atw.getAgencyID(),
                            ROUTE);
            client.routes(atw.getAgencyID())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::populateRoutesListView,
                            e -> Log.e(TAG, "Error in getting list of routes", e)
                    );
            return rootView;
        }

        private void populateRoutesListView(List<TransLocRoute> routes) {
            progressBar.setVisibility(View.INVISIBLE);
            if (routes != null && !routes.isEmpty()) {
                ArrayAdapter<TransLocRoute> routeArrayAdapter = new ArrayAdapter<TransLocRoute>(getActivity(), android.R.layout.simple_spinner_dropdown_item, routes);
                routeListView.setAdapter(routeArrayAdapter);

                // Animate
                TranslateAnimation animate = new TranslateAnimation(routeListView.getWidth(),0,0,0);
                animate.setDuration(250);
                animate.setFillAfter(true);
                routeListView.startAnimation(animate);
                routeListView.setVisibility(View.VISIBLE);

                // Set onclicklistener to open select stops fragment
                routeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        TransLocRoute selectedRoute = (TransLocRoute) parent.getItemAtPosition(position);
                        atw.setRouteID(Integer.toString(selectedRoute.routeID));
                        atw.setRouteName(selectedRoute.toString());

                        Fragment addStopFragment = new AddStopFragment();
                        // Insert the fragment by replacing any existing fragment
                        FragmentManager fragmentManager = getActivity().getFragmentManager();
                        fragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                                .replace(R.id.container, addStopFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                });
            } else {
                Log.e(TAG, "Routes data was null or empty!");
            }
        }
    }

    public static class AddStopFragment extends Fragment {

        private final String TAG = this.getTag();
        ListView stopListView;
        ProgressBar progressBar;

        public AddStopFragment() {

        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            menu.clear();
            inflater.inflate(R.menu.menu_empty, menu);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_add_stop, container, false);
            setHasOptionsMenu(true);
            stopListView = (ListView) rootView.findViewById(R.id.lvStopList);
            progressBar = (ProgressBar) getActivity().findViewById(R.id.pbLoading);
            progressBar.setVisibility(View.VISIBLE);
            TransLocClient client =
                    ServiceGenerator.createService(TransLocClient.class,
                            Utils.BASE_URL,
                            getString(R.string.mashape_key),
                            atw.getAgencyID(),
                            STOP);
            client.stops(atw.getAgencyID())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::populateStopsListView,
                            e -> Log.e(TAG, "Error in getting list of stops", e)
                    );
            return rootView;
        }

        private void populateStopsListView(List<TransLocStop> stops) {
            progressBar.setVisibility(View.INVISIBLE);
            if (stops != null && !stops.isEmpty()) {
                ArrayList<TransLocStop> stopList = new ArrayList<>();
                for (TransLocStop stop : stops) {
                    if (stop.routes.contains(Integer.parseInt(atw.getRouteID()))) {
                        stopList.add(stop);
                    }
                    ArrayAdapter<TransLocStop> stopArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, stopList);
                    stopListView.setAdapter(stopArrayAdapter);

                    // Animate
                    TranslateAnimation animate = new TranslateAnimation(stopListView.getWidth(),0,0,0);
                    animate.setDuration(250);
                    animate.setFillAfter(true);
                    stopListView.startAnimation(animate);
                    stopListView.setVisibility(View.VISIBLE);

                    // Set onclicklistener to open select stops fragment
                    stopListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            TransLocStop selectedStop = (TransLocStop) parent.getItemAtPosition(position);
                            atw.setStopID((Integer.toString(selectedStop.stopId)));
                            atw.setStopName(selectedStop.toString());

                            // Insert the fragment by replacing any existing fragment
                            FragmentManager fragmentManager = getActivity().getFragmentManager();
                            fragmentManager.beginTransaction()
                                    .setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out)
                                    .replace(R.id.container, new CustomizeColorsFragment())
                                    .addToBackStack(null)
                                    .commit();
                        }
                    });
                }
            } else {
                Log.e(TAG, "Stops data is null or empty!");
            }
        }
    }

    public static class CustomizeColorsFragment extends Fragment implements ColorPicker.OnColorChangedListener {
        private final String TAG = this.getTag();
        private ColorPicker picker;
        private SVBar svBar;
        private OpacityBar opacityBar;
        private Button setBackgroundColorButton;
        private Button setTextColorButton;
        private LinearLayout currentBackgroundColor;
        private LinearLayout currentTextColor;

        public CustomizeColorsFragment() {

        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            menu.clear();
            inflater.inflate(R.menu.menu_widget_list, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();
            if (id == R.id.finish_item) {
                Log.v(TAG, "Selected Finish");

                ArrayList<ArrivalTimeWidget> listViewArray;
                try {
                    listViewArray = Utils.getArrivalTimeWidgetsFromStorage(getActivity());
                } catch (IOException e) {
                    Log.e(TAG, "Error in getting previous widget list", e);
                    listViewArray = new ArrayList<>();
                }

                listViewArray.add(atw);

                try {
                    Utils.writeArrivalTimeWidgetsToStorage(getActivity(), listViewArray);
                } catch (Exception e) {
                    Log.e(TAG, "Error in writing widget list to storage");
                    Log.e(TAG, e.getMessage());
                }
                Intent intent = new Intent();
                intent.putExtra("atw", atw);
                getActivity().setResult(1, intent);
                getActivity().finish();
                return true;
            } else {
                return super.onOptionsItemSelected(item);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_customize_colors, container, false);
            setHasOptionsMenu(true);
            picker = (ColorPicker) rootView.findViewById(R.id.picker);
            opacityBar = (OpacityBar) rootView.findViewById(R.id.opacitybar);
            svBar = (SVBar) rootView.findViewById(R.id.svbar);
            setBackgroundColorButton = (Button) rootView.findViewById(R.id.bChangeBackgroundColor);
            setTextColorButton = (Button) rootView.findViewById(R.id.bChangeTextColor);
            currentBackgroundColor = (LinearLayout) rootView.findViewById(R.id.llCurrentBackgroundColor);
            currentTextColor = (LinearLayout) rootView.findViewById(R.id.llCurrentTextColor);


            currentBackgroundColor.setBackgroundColor(atw.getBackgroundColor());
            currentTextColor.setBackgroundColor(atw.getTextColor());

            picker.addSVBar(svBar);
            picker.addOpacityBar(opacityBar);
            picker.setOnColorChangedListener(this);

            setBackgroundColorButton.setOnClickListener(v -> {
                picker.setOldCenterColor(picker.getColor());
                currentBackgroundColor.setBackgroundColor(picker.getColor());
                atw.setBackgroundColor(picker.getColor());
            });

            setTextColorButton.setOnClickListener(v -> {
                picker.setOldCenterColor(picker.getColor());
                currentTextColor.setBackgroundColor(picker.getColor());
                atw.setTextColor(picker.getColor());
            });

            currentBackgroundColor.setOnClickListener(v -> {
                ColorDrawable d = (ColorDrawable) currentBackgroundColor.getBackground();
                picker.setColor(d.getColor());
            });

            currentTextColor.setOnClickListener(v -> {
                ColorDrawable d = (ColorDrawable) currentTextColor.getBackground();
                picker.setColor(d.getColor());
            });

            return rootView;
        }

        @Override
        public void onColorChanged(int i) {

        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
