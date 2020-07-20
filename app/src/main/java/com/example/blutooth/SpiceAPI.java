package com.example.blutooth;

        import android.content.Intent;
        import android.os.Build;
        import android.os.Bundle;
        import android.os.Handler;
        import android.os.Message;
        import android.view.View;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.LinearLayout;
        import android.widget.TextView;

        import androidx.annotation.RequiresApi;
        import androidx.appcompat.app.AppCompatActivity;
//import androidx.cardview.widget.CardView;
        import androidx.constraintlayout.widget.ConstraintLayout;

//import com.google.android.material.snackbar.Snackbar;

        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

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
    private int from = 0, to = 19;
    private String nakedUrl = "https://api.edamam.com/search?q=" + q + "&app_id=3e6d9a08&app_key=b6723e25aa0fde680a04c6c7cf456de2&from=" + from + "&to=" + to;


    Button send,back;
    ConstraintLayout layout;
    TextView httpResponse;  //For testing purposes only
    EditText query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        send = (Button)findViewById(R.id.sendButton);
        back = (Button)findViewById(R.id.backBtn);
        httpResponse = new TextView(this);
        httpResponse.setText("Something for now.");

        query = (EditText)findViewById(R.id.query);
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
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SpiceAPI.this,MainActivity.class);
                startActivity(intent);
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                q = query.getText().toString();
                nakedUrl = "https://api.edamam.com/search?q=" + q + "&app_id=3e6d9a08&app_key=b6723e25aa0fde680a04c6c7cf456de2&from=" + from + "&to=" + to;
                try {
                    startRequestThread(nakedUrl);
                } catch(IOException e) {
                    httpResponse.setText("API call didn't go through.");
                }

                while(httpResponse.getText().toString().contains("Something")) ;

                String apiRes = httpResponse.getText().toString();
                try {
                    apiResponse = new JSONObject(apiRes);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


                JSONArray recipes = null;
                try {
                    recipes = getRecipesFromHits(apiResponse);
                    //updateCardViews(recipes);
                }
                catch(JSONException e) {
                    ;
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
                        System.out.println("We did it.");
                    }
                    else
                        stream = connection.getErrorStream();

                    BufferedReader in = new BufferedReader(new InputStreamReader(stream));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }

                    httpResponse.setText(response.toString());
                    in.close();
                    connection.disconnect();

                }
                catch (IOException e) {
                    httpResponse.setText("API didn't go through");
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
        info[0] = recipe.getString("label");    //Name of recipe
        info[1] = recipe.getString("url");      //Recipe URL
        info[2] = recipe.getString("image");    //Image URL

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

            String ingrName = food.getString("label");
            String unit = measure.getString("label");
            int amt = ingr.getInt("quantity");  //this might give you issues due to implicit float to int conversion

            ingrList[0] = new Ingredient(ingrName, unit, amt);
        }

        return ingrList;
    }
    /*
    private void updateCardViews(JSONArray recipes) throws JSONException {
        if(recipes.equals(null)) {
            return;
        }

        ArrayList<String[]> recipeInfo = new ArrayList<>();
        ArrayList<Ingredient[]> ingredients = new ArrayList<>();
        for(int i = 0; i < 20; i++) {
            JSONObject currentRecipe = recipes.getJSONObject(i);

            recipeInfo.add(getRecipeInfo(currentRecipe));
            ingredients.add(getIngredients(currentRecipe));
        }


        CardView[] cards = new CardView[20];
        LinearLayout cardHolder = new LinearLayout(this);
        for(int i = 0; i < 20; i++) {
            cards[i] = new CardView(this);
            //put formatting stuff here.

            //populate cards
            TextView recipeName = new TextView(this);
            recipeName.setText(recipeInfo.get(i)[0]);
            //add cards to layout
            cards[i].setVisibility(View.VISIBLE);
            cardHolder.addView(cards[i]);
            ;
            //layout.addView(recipeName);
        }
        layout.addView(cardHolder);
        return;
    }*/

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