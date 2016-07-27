package com.codamasters.grbus;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.lapism.searchview.SearchAdapter;
import com.lapism.searchview.SearchHistoryTable;
import com.lapism.searchview.SearchItem;
import com.lapism.searchview.SearchView;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.wooplr.spotlight.SpotlightView;
import com.wooplr.spotlight.prefs.PreferencesManager;

import net.mskurt.neveremptylistviewlibrary.NeverEmptyListView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {

    // Credit bus_icon
    //  Maps Icons Collection https://mapicons.mapsmarker.com

    private static final String URL = "http://transportesrober.com:9055/websae/Transportes/parada.aspx?idparada=";

    private SearchView mSearchView;
    private SearchHistoryTable mHistoryDatabase;

    // Tutorial
    private boolean isRevealEnabled = true;
    private FloatingActionMenu fab_menu;
    private android.support.design.widget.FloatingActionButton fab_visual;
    private static final String INTRO_CARD = "searchView";

    // Refresh
    private SwipeRefreshLayout swipeContainer;
    private String last_info = "";
    private static final String PREFS = "app_info";

    // ListView
    private NeverEmptyListView neverEmptyListView;
    private String error;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        last_info = prefs.getString("last_info", "");

        fab_menu = (FloatingActionMenu) findViewById(R.id.fab);
        fab_visual = (android.support.design.widget.FloatingActionButton) findViewById(R.id.fab_visual);

        // Añadimos mediante las keys aquuelas con
        Map<String, ?> allEntries = prefs.getAll();
        allEntries.remove("last_info");
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if( Boolean.valueOf(entry.getValue().toString())) {
                final FloatingActionButton programFab1 = new FloatingActionButton(this);
                programFab1.setButtonSize(FloatingActionButton.SIZE_MINI);
                programFab1.setLabelText(entry.getKey());
                programFab1.setId(Integer.parseInt(entry.getKey()));
                programFab1.setImageResource(R.drawable.ic_star_small);
                programFab1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getData(programFab1.getLabelText());
                        fab_menu.close(true);
                    }
                });
                fab_menu.addMenuButton(programFab1);
            }
        }

        //Set NeverEmptyListView's adapter
        neverEmptyListView=(NeverEmptyListView)findViewById(R.id.listview);
        ArrayList<Bus> buses = new ArrayList<>();
        BusAdapter adapter = new BusAdapter(this, buses);
        neverEmptyListView.setAdapter(adapter);

        neverEmptyListView.setHolderClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchView.open(true);
            }
        });


        setSearchView();
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getData(last_info);
            }
        });

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

    }

    private void showTutorial(View view, String usageId) {
        new SpotlightView.Builder(this)
                .introAnimationDuration(400)
                .enableRevalAnimation(isRevealEnabled)
                .performClick(true)
                .fadeinTextDuration(400)
                .headingTvColor(Color.parseColor("#ba1138"))
                .headingTvSize(32)
                .headingTvText("Horario de autobuses")
                .subHeadingTvColor(Color.parseColor("#ffffff"))
                .subHeadingTvSize(16)
                .subHeadingTvText("Para conocer cuanto le queda a tu autobús indica el número de la parada en el buscador. \n\n" +
                                    "Tras marcar una parada como favorita aparecerá en este menú.")
                .maskColor(Color.parseColor("#dc000000"))
                .target(view)
                .lineAnimDuration(400)
                .lineAndArcColor(Color.parseColor("#eb273f"))
                .dismissOnTouch(true)
                .enableDismissAfterShown(true)
                .usageId(usageId) //UNIQUE ID
                .show();
    }

    protected void setSearchView() {
        mHistoryDatabase = new SearchHistoryTable(this);

        mSearchView = (SearchView) findViewById(R.id.searchView);
        if (mSearchView != null) {
            mSearchView.setVersion(SearchView.VERSION_TOOLBAR);
            mSearchView.setVersionMargins(SearchView.VERSION_MARGINS_TOOLBAR_BIG);
            mSearchView.setHint("Número de la parada");
            mSearchView.setTextSize(16);
            mSearchView.setDivider(false);
            mSearchView.setVoice(false);
            mSearchView.setAnimationDuration(SearchView.ANIMATION_DURATION);
            mSearchView.setShadowColor(ContextCompat.getColor(this, R.color.search_shadow_layout));
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    getData(query);
                    mSearchView.close(false);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });


            List<SearchItem> suggestionsList = new ArrayList<>();

            SearchAdapter searchAdapter = new SearchAdapter(this, suggestionsList);
            searchAdapter.setOnItemClickListener(new SearchAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    TextView textView = (TextView) view.findViewById(R.id.textView_item_text);
                    String query = textView.getText().toString();
                    getData(query);
                    mSearchView.close(false);
                }
            });
            mSearchView.setAdapter(searchAdapter);
        }
    }

    private void savePref(){
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        editor.putString("last_info", last_info);
        editor.commit();
    }


    private void getData(String text) {
        last_info = text;
        savePref();

        mHistoryDatabase.addItem(new SearchItem(text));
        String url = URL + text;

        new RetrieveBusInfo(this, url, text).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                PreferencesManager mPreferencesManager = new PreferencesManager(SearchActivity.this);
                mPreferencesManager.resetAll();
                showTutorial(fab_visual, INTRO_CARD);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    /************************************************************************************
                                    ASYNKTASK DESCARGA DE DATOS
     ***********************************************************************************/


    class RetrieveBusInfo extends AsyncTask<String, Void, String> {

        String url;
        ArrayList<Bus> buses;
        Context context;
        String stop;

        public RetrieveBusInfo(Context context, String url, String stop){
            this.context = context;
            this.url = url;
            this.stop = stop;
        }

        protected void onPreExecute(){
            swipeContainer.setRefreshing(true);
        }

        protected String doInBackground(String... urls) {
            try {

                Document doc  = Jsoup.connect(url).get();

                if(!parseError(doc, stop)){
                    buses = parseResult(doc, stop);
                }else{
                    return "error";
                }

                return "success";

            } catch (Exception e) {
                error = "Error en la consulta";
                return "error";
            }
        }

        protected void onPostExecute(String result) {
            swipeContainer.setRefreshing(false);


            if(result.equals("error")){
                buses = new ArrayList<>();
                if(error.contains("Error")){
                    neverEmptyListView.setHolderImageBackground(R.drawable.no_connection);
                }else if(error.contains("incorrectos")){
                    neverEmptyListView.setHolderImageBackground(R.drawable.incorrect);
                }else{
                    neverEmptyListView.setHolderImageBackground(R.drawable.no_bus);
                }
                neverEmptyListView.setHolderText(error);
            }

            BusAdapter adapter = new BusAdapter(context, buses);
            neverEmptyListView.setAdapter(adapter);
        }
    }

    /************************************************************************************
                                         PARSEADOR
     ***********************************************************************************/

    private ArrayList<Bus> parseResult(Document doc, String stop){
        ArrayList<Bus> buses = new ArrayList<>();

        String result1 = doc.body().getElementsByAttributeValue("width", "590").html();
        Document doc2 = Jsoup.parse(result1);
        Element table = doc2.select("table").get(0);
        Elements rows = table.select("tr");

        buses.add(new Bus(stop, null));

        for (int i = 1; i < rows.size(); i++) { //first row is the col names so skip it.
            Element row = rows.get(i);
            Elements cols = row.select("td");

            Bus bus;
            if(cols.get(2).text() != null || cols.get(2).text()!="") {
                bus = new Bus(cols.get(0).text(), cols.get(2).text());
            }else{
                bus = new Bus(cols.get(0).text(), "<");
            }

            buses.add(bus);

        }

        return buses;
    }

    private boolean parseError(Document doc, String stop){
        String result1 = doc.body().getElementsByAttributeValue("width", "590").html();
        Document doc2 = Jsoup.parse(result1);

        // Log.d("Error1", doc2.body().getElementById("Guaguas2_LabelGuaguas").text());

        if(doc2.text().contains("Error")){
            error =  "Parámetros de búsqueda incorrectos.";
        }else if(doc2.text().contains("No hay")){
            error =  "No hay autobuses acercándose a la parada " + stop+ " :(";
        }else{
            error = null;
        }
        if(error!=null)
            return true;

        return false;
    }


    /************************************************************************************
                                        ADAPTADOR
     ***********************************************************************************/

    class BusAdapter extends ArrayAdapter<Bus> {

        public BusAdapter(Context context, ArrayList<Bus> buses){
            super(context, 0, buses);
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            // Get the data item for this position
            final Bus bus = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (bus.getTime() == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_stop, parent, false);
                LikeButton likeButton = (LikeButton) convertView.findViewById(R.id.like_button);

                SharedPreferences prefs = getContext().getSharedPreferences("app_info", getContext().MODE_PRIVATE);
                boolean liked = prefs.getBoolean(bus.getName(), false);

                if(liked)
                    likeButton.setLiked(true);

                likeButton.setOnLikeListener(new OnLikeListener() {
                    @Override
                    public void liked(LikeButton likeButton) {
                        SharedPreferences.Editor editor = getContext().getSharedPreferences("app_info", getContext().MODE_PRIVATE).edit();
                        editor.putBoolean(bus.getName(), true);
                        editor.commit();

                        final FloatingActionMenu fab_menu = (FloatingActionMenu) ((Activity) getContext()).findViewById(R.id.fab);
                        final FloatingActionButton programFab1 = new FloatingActionButton(getContext());
                        programFab1.setButtonSize(FloatingActionButton.SIZE_MINI);
                        programFab1.setLabelText(bus.getName());
                        programFab1.setId(Integer.parseInt(bus.getName()));
                        programFab1.setImageResource(R.drawable.ic_star_small);
                        programFab1.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                getData(programFab1.getLabelText());
                                fab_menu.close(true);
                            }
                        });
                        fab_menu.addMenuButton(programFab1);

                    }

                    @Override
                    public void unLiked(LikeButton likeButton) {
                        SharedPreferences.Editor editor = getContext().getSharedPreferences("app_info", getContext().MODE_PRIVATE).edit();
                        editor.putBoolean(bus.getName(), false);
                        editor.commit();

                        final FloatingActionMenu fab_menu = (FloatingActionMenu) ((Activity) getContext()).findViewById(R.id.fab);
                        //fab_menu.removeAllMenuButtons();
                        fab_menu.removeMenuButton((FloatingActionButton) fab_menu.findViewById(Integer.parseInt(bus.getName())));
                    }
                });
            }else{
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_bus, parent, false);
                TextView tvTime = (TextView) convertView.findViewById(R.id.tvTime);
                tvTime.setText(bus.getTime());
            }

            // Lookup view for data population
            TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
            // Populate the data into the template view using the data object
            tvName.setText(bus.getName());
            // Return the completed view to render on screen
            return convertView;
        }

    }

}
