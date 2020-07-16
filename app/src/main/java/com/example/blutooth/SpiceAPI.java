package com.example.blutooth;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

//import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class SpiceAPI extends AppCompatActivity {
    //Remember the CardView.
    private JSONObject apiResponse;
    private String q = "chicken";
    private int from = 0, to = 5;
    private int delta = 0;
    private String nakedUrl = "https://api.edamam.com/search?q=" + q + "&app_id=3e6d9a08&app_key=b6723e25aa0fde680a04c6c7cf456de2&from=" + from + "&to=" + to;

    private CardView card1, card2, card3, card4, card5;
    private TextView recipeTitle1, recipeTitle2, recipeTitle3, recipeTitle4, recipeTitle5;

    Button send;
    LinearLayout layout;
    TextView httpResponse;  //For testing purposes only
    String http = "";
    EditText query;
    ArrayList<String> spiceList = new ArrayList<>(4);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        //Simulate getting info from first page
        spiceList.add("Salt");
        spiceList.add("Black Pepper");
        spiceList.add("Paprika");
        spiceList.add("Oregano");

        send = (Button)findViewById(R.id.sendButton);
        layout = (LinearLayout)findViewById(R.id.linearLayout);
        httpResponse = new TextView(this);
        query = (EditText)findViewById(R.id.query);

        card1 = (CardView)findViewById(R.id.card1);
        card2 = (CardView)findViewById(R.id.card2);
        card3 = (CardView)findViewById(R.id.card3);
        card4 = (CardView)findViewById(R.id.card4);
        card5 = (CardView)findViewById(R.id.card5);

        recipeTitle1 = (TextView)findViewById(R.id.recipeTitle1);
        recipeTitle2 = (TextView)findViewById(R.id.recipeTitle2);
        recipeTitle3 = (TextView)findViewById(R.id.recipeTitle3);
        recipeTitle4 = (TextView)findViewById(R.id.recipeTitle4);
        recipeTitle5 = (TextView)findViewById(R.id.recipeTitle5);

        /*
        try {
            startRequestThread(nakedUrl);
        } catch(IOException e) {
            httpResponse.setText("API call didn't go through.");
        }

        String apiRes = httpResponse.getText().toString();
        try {
            apiResponse = new JSONObject(apiRes);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                q = query.getText().toString();
                nakedUrl = "https://api.edamam.com/search?q=" + q + "&app_id=3e6d9a08&app_key=b6723e25aa0fde680a04c6c7cf456de2&from=" + from + "&to=" + to;
                try {
                    startRequestThread(nakedUrl);
                } catch(IOException e) {
                    Toast.makeText(getApplicationContext(), "API call didn't go through", Toast.LENGTH_LONG).show();
                }

                //while(http.equals("")) ;
                Toast.makeText(getApplicationContext(), "API call went through", Toast.LENGTH_LONG).show();
                try {
                    apiResponse = new JSONObject(http);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                /*
                Toast testMessage = Toast.makeText(getApplicationContext(), httpResponse.getText().toString(), Toast.LENGTH_LONG);
                testMessage.show();*/

                JSONArray recipes = null;
                try {
                    recipes = getRecipesFromHits(apiResponse);
                    //updateCardViews(recipes);
                }
                catch(JSONException e) {
                    e.printStackTrace();
                    /*Toast jsonMessage = Toast.makeText(getApplicationContext(), "Got a JSON exception", Toast.LENGTH_LONG);
                    jsonMessage.show();*/
                }
            }
        });


    }

    public void startRequestThread(final String url) throws IOException {
        new Thread() {
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    int responseCode = connection.getResponseCode();
                    InputStream stream;

                    if (responseCode == 200) {
                        stream = connection.getInputStream();
                    }
                    else
                        stream = connection.getErrorStream();

                    BufferedReader in = new BufferedReader(new InputStreamReader(stream));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }

                    http = response.toString();
                    in.close();
                    connection.disconnect();

                }
                catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "API call didn't go through in thread", Toast.LENGTH_LONG).show();
                }
            }
        }.start();
    }

    private JSONArray getRecipesFromHits(JSONObject res) throws JSONException {
        int from = ((Number)res.get("from")).intValue();
        int to = ((Number)res.get("to")).intValue();

        JSONArray hits = res.getJSONArray("hits");
        JSONArray recipes = new JSONArray();
        int numHits = hits.length();

        for(int i = 0; i < numHits; i++){
            recipes.put(hits.getJSONObject(i));
        }
        return recipes; //This will definitely need to change lol
    }

    private String[] getRecipeInfo(JSONObject recipe) throws JSONException {
        String[] info = new String[3];

        String wholeRecipe = recipe.toString();

        boolean error[] = {false, false, false};
        int uriIndex = wholeRecipe.indexOf("\"uri\"") + 7;
        int labelIndex = wholeRecipe.indexOf("\"label\"") + 9;
        int imageIndex = wholeRecipe.indexOf("\"image\"") + 9;
        int sourceIndex = wholeRecipe.indexOf("\"source\"");

        int indices[] = {uriIndex, labelIndex, imageIndex, sourceIndex};

        if(uriIndex == -1 || labelIndex == -1) {
            info[0] = "N/a";
            error[0] = true;
        }

        if(labelIndex == -1 || imageIndex == -1) {
            info[1] = "N/a";
            error[1] = true;
        }

        if(imageIndex == -1 || sourceIndex == -1) {
            info[2] = "N/a";
            error[2] = true;
        }

        for(int i = 0; i < 3; i++) {
            if(!error[i]) {
                if(i != 2)  info[i] = wholeRecipe.substring(indices[i], indices[i+1]-9);
                else info[i] = wholeRecipe.substring(indices[i], indices[i+1]-2);
            }
        }

        return info;
    }

    private Ingredient[] getIngredients(JSONObject recipe) throws JSONException {
        JSONArray ingrs = recipe.getJSONArray("ingredients");
        int numIngrs = ingrs.length();
        Ingredient[] ingrList = new Ingredient[numIngrs];

        for(int i = 0; i < numIngrs; i++) {
            JSONObject ingr = ingrs.getJSONObject(i);
            JSONObject food = ingr.getJSONObject("food");
            JSONObject measure = ingr.getJSONObject("measure");

            String ingrName = getJSONString(food, "label", "END");
            String unit = getJSONString(measure, "label", "END");
            int amt = ingr.getInt("quantity");  //this might give you issues due to implicit float to int conversion

            ingrList[0] = new Ingredient(ingrName, unit, amt);
        }

        return ingrList;
    }

    private static String getJSONString(JSONObject obj, String firstKey, String secondKey) throws JSONException{
        boolean error;

        String wholeObj = obj.toString();

        int startIndex = wholeObj.indexOf(firstKey);
        int endIndex = wholeObj.indexOf(secondKey);

        if(secondKey.equals("END")) {
            endIndex = wholeObj.indexOf("\"", startIndex + 1);
        }

        if(startIndex == -1 || endIndex == -1) {
            error = true;
            return "N/a";
        }

        startIndex += (firstKey.length() + 2);

        return wholeObj.substring(startIndex, endIndex + 1);
    }

    private void updateCardViews(JSONArray recipes) throws JSONException {
        if(recipes.equals(null)) {
            Toast nullRecipe = Toast.makeText(getApplicationContext(), "Got null recipe JSON object", Toast.LENGTH_LONG);
            nullRecipe.show();
            return;
        }

        ArrayList<String[]> recipeInfo = new ArrayList<>();
        ArrayList<Ingredient[]> ingredients = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            JSONObject currentRecipe = recipes.getJSONObject(i);

            recipeInfo.add(getRecipeInfo(currentRecipe));
            ingredients.add(getIngredients(currentRecipe));
        }


        //CardView[] cards = new CardView[20];
        TextView titles[] = {recipeTitle1, recipeTitle2, recipeTitle3, recipeTitle4, recipeTitle5};
        TextView links[];
        TextView ingredientList[];
        for(int i = 0; i < 5; i++) {
            //populate cards
            titles[i].setText(recipeInfo.get(i)[1]);
            Toast recipeChange = Toast.makeText(getApplicationContext(), "Should have changed the title by now", Toast.LENGTH_LONG);
            recipeChange.show();

        }
        return;
    }

    private class Ingredient {
        private String name;
        private String measureUnit;
        private int measureAmount;

        Ingredient(String nm, String msr, int amt) {
            this.name = nm;
            this.measureUnit = msr;
            this.measureAmount = amt;
        }

        public String getName() {
            return this.name;
        }

        public String getUnit() {
            return this.measureUnit;
        }

        public int getAmt() {
            return this.measureAmount;
        }

        @Override
        public String toString(){
            return this.name + this.measureUnit + this.measureAmount;
        }
    }

    Handler handler = new Handler(new Handler.Callback() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public boolean handleMessage(Message msg) {
            return true;
        }
    });
}