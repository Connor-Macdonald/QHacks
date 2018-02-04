package com.example.connormacdonald.qhackstest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.gson.stream.JsonReader;
import com.ibm.watson.developer_cloud.android.library.camera.CameraHelper;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import static java.lang.System.getProperty;


public class MainActivity extends AppCompatActivity {
    private NaturalLanguageUnderstanding service;
    private String username = "9235e8e2-26fa-4ae5-aeea-371ff13ce8ff";
    private String password = "OvItlVOQGuzp";
    private VisualRecognition vrClient;
    private CameraHelper helper;
    private String BestFoodTag = "na", BestImgTag = "na";
    private Double BestFoodScore = 0d, BestImgScore = 0d;
    private List<String> ClassifierIDS = new ArrayList<String>();
    private List<String> Catigories = new ArrayList<String>();

    //Graph Infor
    private int cal = 12;
    private int carbs = 23;
    private int fat = 35;
    private int sodium = 48;
    private int protein = 22;
    //LineChart lineChart;
    ArrayList<String> xAxes = new ArrayList<>();
    ArrayList<DataPoint> yAxes = new ArrayList<>();

    TextView detectedObjects;
    ImageView preview;
    GraphView graph;
    ToggleButton switchButton;

    public void switchData(View view) {
        if(detectedObjects.getVisibility()==View.INVISIBLE) {
            setWords(view);
        }
        else{
            setImage(view);
        }
    }

    public void setWords(View view){
        detectedObjects.setVisibility(View.VISIBLE);
        preview.setVisibility(View.INVISIBLE);
        graph.setVisibility(View.INVISIBLE);
        switchButton.setText("See Image");
    }

    public void setNone(View view){
        detectedObjects.setVisibility(View.INVISIBLE);
        preview.setVisibility(View.INVISIBLE);
        graph.setVisibility(View.INVISIBLE);
        switchButton.setText("See Words");
    }

    public void setImage(View view){
        detectedObjects.setVisibility(View.INVISIBLE);
        preview.setVisibility(View.VISIBLE);
        graph.setVisibility(View.GONE);
        switchButton.setText("See Words");
    }

    public void setGraph(View view){
        detectedObjects.setVisibility(View.INVISIBLE);
        preview.setVisibility(View.INVISIBLE);
        graph.setVisibility(View.VISIBLE);
        switchButton.setText("See Words");
    }

    public void switchGraphVisibility(View view) {
        if (detectedObjects.getVisibility() == View.INVISIBLE) {
            preview.setVisibility(View.VISIBLE);
        } else {
            preview.setVisibility(View.INVISIBLE);
        }
    }

    public void openAnalytics(View view) {
        setGraph(view);
        //setContentView(R.layout.activity_main);
        GraphView graph = (GraphView) findViewById(R.id.graph);
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(new DataPoint[]{
                new DataPoint(0, cal),
                new DataPoint(2, carbs),
                new DataPoint(4, fat),
                new DataPoint(6, sodium),
                new DataPoint(8, protein),
        });

        //graph.setScaleX(1);
        graph.setPadding(10, 10, 10, 10);

        // set manual X bounds
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(10);


        // set manual Y bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(200);
        graph.getViewport().setMaxXAxisSize(6);

        //Dimensions
        //graph.setPadding(10, 10, 10, 10);
        graph.getLegendRenderer().setBackgroundColor(Color.BLUE);


        //labels
        graph.getGridLabelRenderer().setNumVerticalLabels(10);
        graph.getGridLabelRenderer().setVerticalAxisTitle("Percent of Daily Value (%)");
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Nutrients");
        //graph.getGridLabelRenderer().setLabelsSpace(1);
        graph.setTitle("Daily Nutrition Values");


        //naming the x-axis
        String[] horizontalAxisNames = new String[]{"KCal",
                "Carbs", "Fat", "Sodium", "Protein"};

        String[] verticAxisNames = new String[]{"0%", "20%", "40%", "60%",
                "80%", "100%", "120%", "140%", "160%", "180%", "200%"};

        StaticLabelsFormatter staticLabels = new StaticLabelsFormatter(
                graph, horizontalAxisNames, verticAxisNames);
        graph.getGridLabelRenderer().setLabelFormatter(staticLabels);
        graph.getGridLabelRenderer().setHorizontalLabelsAngle(90);
        graph.getGridLabelRenderer().setNumHorizontalLabels(10);
        graph.getLegendRenderer().setTextColor(Color.BLACK);

        graph.addSeries(series);

        series.setDataWidth(0.75);
    }

    public class NutritionForFood {
        private String Name;
        private String[][] Nutrition = new String[14][3];
        public NutritionForFood(String Name, String[][] Nutrition){
            this.Name = Name;
            this.Nutrition = Nutrition.clone();
        }
        public String toString(){
            String ret = "Name: "+this.Name + "\n";
                for(int i = 0;i<14;i++){
                    ret = ret + "" + Nutrition[i][0]+ " " +Nutrition[i][2]+" "+Nutrition[i][1]+"\n";
                }
            return ret;
        }
    }
    private List<NutritionForFood> FoundFacts = new ArrayList<NutritionForFood>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vrClient = new VisualRecognition(
                VisualRecognition.VERSION_DATE_2016_05_20,
                getString(R.string.api_key));
        helper = new CameraHelper(this);
        ClassifierIDS.add("products_1758431799");
        ClassifierIDS.add("food");

        detectedObjects = findViewById(R.id.detected_objects);
        preview = findViewById(R.id.preview);
        graph = findViewById(R.id.graph);
        switchButton = findViewById(R.id.toggleButton2);

        graph.setVisibility(View.INVISIBLE);
        preview.setVisibility(View.INVISIBLE);
        detectedObjects.setVisibility(View.INVISIBLE);
    }

    public void takePicture(View view) {
        setNone(view);
        helper.dispatchTakePictureIntent();
    }

    public void searchData(View view) {
        GraphView graph = (GraphView) findViewById(R.id.graph);
        if (graph.getVisibility() == View.VISIBLE) {
            setImage(view);
        }
    }

    public void takeFoodPicture(View view) {
        GraphView graph = (GraphView) findViewById(R.id.graph);
        if (graph.getVisibility() == View.VISIBLE) {
            setImage(view);
        }
        takePicture(view);
    }

    public void takeNutrientPicture(View view) {
        GraphView graph = (GraphView) findViewById(R.id.graph);
        if (graph.getVisibility() == View.VISIBLE) {
            setImage(view);
        }
    }

    protected void QueryDatabase(String query) throws Exception {
            String FoodName;
            String FoodNum = "0";
            URL FoodSearch = new URL(" https://api.nal.usda.gov/ndb/search/?format=json&q=" + query.replace(" ", "%20") + "&sort=n&max=25&offset=0&api_key=vZz63oyg9zKtD9bo4jn8MDSagBPusDC4bXAvDYJJ");
            HttpsURLConnection myConnection = (HttpsURLConnection) FoodSearch.openConnection();
            if (myConnection.getResponseCode() == 200) {
                InputStream responseBody = myConnection.getInputStream();
                InputStreamReader responseBodyReader =
                        new InputStreamReader(responseBody, "UTF-8");
                JsonReader jsonReader = new JsonReader(responseBodyReader);
                jsonReader.beginObject();
                Log.d("MyTag", jsonReader.nextName());
                jsonReader.beginObject();
                jsonReader.nextName();
                jsonReader.nextString();
                jsonReader.nextName();
                jsonReader.nextString();
                jsonReader.nextName();
                jsonReader.nextString();
                jsonReader.nextName();
                jsonReader.nextInt();
                jsonReader.nextName();
                jsonReader.nextInt();
                jsonReader.nextName();
                jsonReader.nextInt();
                jsonReader.nextName();
                jsonReader.nextString();
                jsonReader.nextName();
                jsonReader.nextString();
                jsonReader.nextName();
                jsonReader.beginArray();
                jsonReader.beginObject();
                jsonReader.nextName();
                jsonReader.nextInt();
                jsonReader.nextName();
                jsonReader.nextString();
                jsonReader.nextName();
                FoodName = jsonReader.nextString();
                jsonReader.nextName();
                FoodNum = jsonReader.nextString();
                Log.d("MyTagString FoodNum", FoodName + " " + FoodNum);
                jsonReader.close();
                myConnection.disconnect();
            } else {
                Log.e("MyTag", String.valueOf(myConnection.getResponseCode()));
            }
            URL NutritionSearch = new URL(" https://api.nal.usda.gov/ndb/nutrients/?format=json&api_key=vZz63oyg9zKtD9bo4jn8MDSagBPusDC4bXAvDYJJ&nutrients=205&nutrients=204&nutrients=208&nutrients=269" +
                    "&nutrients=606&nutrients=605&nutrients=601&nutrients=307&nutrients=291&nutrients=203&nutrients=318&nutrients=418&nutrients=301&nutrients=303&ndbno=" + FoodNum);
            HttpsURLConnection myConnection2 =
                    (HttpsURLConnection) NutritionSearch.openConnection();
             if (myConnection2.getResponseCode() == 200) {
                 InputStream responseBody = myConnection2.getInputStream();
                 InputStreamReader responseBodyReader =
                         new InputStreamReader(responseBody, "UTF-8");
                 JsonReader jsonReader = new JsonReader(responseBodyReader);
                 jsonReader.beginObject();
                 jsonReader.nextName();
                 jsonReader.beginObject();
                 jsonReader.nextName();
                 jsonReader.nextString();
                 jsonReader.nextName();
                 jsonReader.nextString();
                 jsonReader.nextName();
                 jsonReader.nextString();
                 jsonReader.nextName();
                 jsonReader.nextInt();
                 jsonReader.nextName();
                 jsonReader.nextInt();
                 jsonReader.nextName();
                 jsonReader.nextInt();
                 jsonReader.nextName();
                 jsonReader.beginArray();
                 jsonReader.beginObject();
                 jsonReader.nextName();
                 jsonReader.nextString();
                 jsonReader.nextName();
                 jsonReader.nextString();
                 jsonReader.nextName();
                 jsonReader.nextDouble();
                 jsonReader.nextName();
                 jsonReader.nextString();
                 Log.d("MyTag", jsonReader.nextName());
                 jsonReader.beginArray();
                 int i = 0;
                 String[][] Nutrition = new String[14][3];
                 while (jsonReader.hasNext()) {
                     jsonReader.beginObject();
                     jsonReader.nextName();
                     jsonReader.nextString();
                     Log.d("MyTag", jsonReader.nextName());
                     Log.d("MyTag", String.valueOf(i));
                     Nutrition[i][0] = jsonReader.nextString();
                     jsonReader.nextName();
                     Nutrition[i][1] = jsonReader.nextString();
                     jsonReader.nextName();
                     Nutrition[i][2] = jsonReader.nextString();
                     jsonReader.nextName();
                     try {
                         jsonReader.nextDouble();
                     } catch (Exception e) {
                         jsonReader.nextString();
                     }
                     jsonReader.endObject();
                     i++;
                 }
                 jsonReader.endArray();
                 jsonReader.close();
                 NutritionForFood NEW = new NutritionForFood(query, Nutrition);
                 Log.d("MyTag", NEW.toString());
                 TextView detectedObjects =
                         findViewById(R.id.detected_objects);
                 detectedObjects.setText(NEW.toString());
                 detectedObjects.setVisibility(View.INVISIBLE);
                 ToggleButton switchButton = findViewById(R.id.toggleButton2);
                 switchButton.setVisibility(View.VISIBLE);
                 FoundFacts.add(NEW);
                 detectedObjects.setVisibility(View.VISIBLE);
                 ImageView preview =
                         findViewById(R.id.preview);
                 preview.setVisibility(View.INVISIBLE);
                 GraphView graph =
                         findViewById(R.id.graph);
                 graph.setVisibility(View.INVISIBLE);
             } else {
                 throw new Exception(String.valueOf(myConnection2.getResponseCode()));
             }
    }


    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == CameraHelper.REQUEST_IMAGE_CAPTURE) {
                final Bitmap photo = helper.getBitmap(resultCode);
                final File photoFile = helper.getFile(resultCode);
                ImageView preview = findViewById(R.id.preview);
                preview.setVisibility(View.VISIBLE);
                preview.setImageBitmap(Bitmap.createScaledBitmap(photo, 4096,2303, false));
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        VisualClassification responseFood =
                                vrClient.classify(
                                        new ClassifyImagesOptions.Builder()
                                                .classifierIds(ClassifierIDS)
                                                .images(photoFile)
                                                .build()
                                ).execute();

                        ImageClassification FoodClassification = responseFood.getImages().get(0);
                        VisualClassifier FoodClassifier = FoodClassification.getClassifiers().get(0);

                        final StringBuffer output = new StringBuffer();
                        for (VisualClassifier.VisualClass object : FoodClassifier.getClasses()) {
                            Log.d("MyTag", object.getName());
                            if (object.getScore() > BestFoodScore) {
                                BestFoodScore = object.getScore();
                                BestFoodTag = object.getName();
                            }
                        }
                        Log.d("MyTag", "Best Tag:" + BestFoodTag);
                        output.append("Item Is: ").append(BestFoodTag).append("\n ");

                        try {
                            QueryDatabase(BestFoodTag);
                        }
                        catch(Exception e)
                        {
                        }

                        /*VisualClassification responseImg =
                                vrClient.classify(
                                        new ClassifyImagesOptions.Builder()
                                                .images(photoFile)
                                                .build()
                                ).execute();

                        ImageClassification ImgClassification = responseImg.getImages().get(0);
                        VisualClassifier ImgClassifier = ImgClassification.getClassifiers().get(0);

                        for (VisualClassifier.VisualClass object : ImgClassifier.getClasses()) {
                            Log.d("MyTag", object.getName());
                            if (object.getScore() > BestImgScore) {
                                BestImgScore = object.getScore();
                                BestImgTag = object.getName();
                            }
                        }
                        Log.d("MyTag", "Best Tag:" + BestImgTag);
                        output.append("Item Is: ").append(BestImgTag).append("\n ");*/


                        /*//Text
                        RecognizedText response2 =
                                vrClient.recognizeText(new VisualRecognitionOptions.Builder()
                                        .images(photoFile).build()).execute();

                        ImageText TextIdentified = response2.getImages().get(0);
                        Log.d("MyTag", TextIdentified.getText());
                        for (Word words: TextIdentified.getWords()){
                            Log.d("MyTag", words.getWord() + words.getScore());
                        }

                        ConceptsOptions concepts = new ConceptsOptions.Builder()
                                .limit(5)
                                .build();
                        Features features = new Features.Builder().concepts(concepts).categories(new CategoriesOptions()).build();
                        AnalyzeOptions parameters = new AnalyzeOptions.Builder()
                                .text(TextIdentified.getText())
                                .features(features)
                                .returnAnalyzedText(true)
                                .build();

                        AnalysisResults results = service.analyze(parameters).execute();
                        results.getAnalyzedText();
                        boolean isNutrition = false;
                        for (ConceptsResult concept : results.getConcepts()) {
                            Log.d("MyTag", "Text: " + concept.getText());
                            Log.d("MyTag", "Reasource: " + concept.getDbpediaResource());
                            Log.d("MyTag", "Score: " + concept.getRelevance());
                            if(concept.getText().toLowerCase().contains("nutrition")||concept.getText().toLowerCase().contains("water")||concept.getText().toLowerCase().contains("carb")){
                                isNutrition = true;
                            }
                        }
                        for (CategoriesResult result : results.getCategories()) {
                            Log.d("MyTag", "Label: " + result.getLabel());
                            Log.d("MyTag", "Score: " + result.getScore());
                            if(result.getLabel().toLowerCase().contains("nutrition")||result.getLabel().toLowerCase().contains("health and fitness")||result.getLabel().toLowerCase().contains("food and drink")){
                                isNutrition = true;
                            }
                        }
                        if(isNutrition){
                            String[] lines = TextIdentified.getText().split(System.getProperty("line.separator"));
                            for(String line : lines){
                                for(String fact : Catigories){
                                    if(line.toLowerCase().contains(fact)){
                                        try {
                                            Integer.parseInt(line.replaceAll("[\\D]", ""));
                                            FoundFacts.add(line);
                                        }
                                        catch( NumberFormatException e){}
                                    }
                                }
                            }
                            Log.d("MyTag", FoundFacts.toString());
                        }*/
                    }
                });
            }
        } catch(Exception e){
        }
    }
}
