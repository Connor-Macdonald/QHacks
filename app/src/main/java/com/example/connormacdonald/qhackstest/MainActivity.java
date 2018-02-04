package com.example.connormacdonald.qhackstest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import static java.lang.System.getProperty;

import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.Header;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    public static final String subscriptionKey = "2368e583599d4695b73b12644065184c";
    public static final String uriBase = "https://westus.api.cognitive.microsoft.com/vision/v1.0/recognizeText?handwriting=false";

    private VisualRecognition vrClient;
    private CameraHelper helper;
    private String BestFoodTag = "na", BestImgTag = "na";
    private Double BestFoodScore = 0d, BestImgScore = 0d;
    private List<String> ClassifierIDS = new ArrayList<String>();

    public void switchData(View view) {
        TextView detectedObjects = findViewById(R.id.detected_objects);
        ImageView preview = findViewById(R.id.preview);
        ToggleButton switchButton = findViewById(R.id.toggleButton2);
        if(detectedObjects.getVisibility()==View.INVISIBLE) {
            detectedObjects.setVisibility(View.VISIBLE);
            preview.setVisibility(View.INVISIBLE);
            switchButton.setText("See Image");
        }
        else{
            detectedObjects.setVisibility(View.INVISIBLE);
            preview.setVisibility(View.VISIBLE);
            switchButton.setText("See Data");
        }
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

    }

    public void takePicture(View view) {
        helper.dispatchTakePictureIntent();
    }

    public void searchData(View view) {

    }

    public void takeFoodPicture(View view) {
        takePicture(view);
    }

    public void takeNutrientPicture(View view) {

    }

    public void openAnalytics(View view) {

    }
    protected void AzureText(Bitmap BM){
        HttpClient textClient = new DefaultHttpClient();
        HttpClient resultClient = new DefaultHttpClient();

        try
        {
            // This operation requrires two REST API calls. One to submit the image for processing,
            // the other to retrieve the text found in the image.
            //
            // Begin the REST API call to submit the image for processing.
            URI uri = new URI(uriBase);
            HttpPost textRequest = new HttpPost(uri);

            // Request headers. Another valid content type is "application/octet-stream".
            textRequest.setHeader("Content-Type", "application/octet-stream");
            textRequest.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);

            // Request body.
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BM.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            InputStreamEntity reqEntity = new InputStreamEntity(inputStream, -1);
            reqEntity.setContentType("image/jpeg");
            reqEntity.setChunked(true);
            textRequest.setEntity(reqEntity);

            // Execute the first REST API call to detect the text.
            HttpResponse textResponse = textClient.execute(textRequest);

            // Check for success.
            if (textResponse.getStatusLine().getStatusCode() != 202)
            {
                // Format and display the JSON error message.
                HttpEntity entity = textResponse.getEntity();
                String jsonString = EntityUtils.toString(entity);
                JSONObject json = new JSONObject(jsonString);
                System.out.println("Error:\n");
                System.out.println(json.toString(2));
                return;
            }

            String operationLocation = null;

            // The 'Operation-Location' in the response contains the URI to retrieve the recognized text.
            Header[] responseHeaders = textResponse.getAllHeaders();
            for(Header header : responseHeaders) {
                if(header.getName().equals("Operation-Location"))
                {
                    // This string is the URI where you can get the text recognition operation result.
                    operationLocation = header.getValue();
                    break;
                }
            }

            // NOTE: The response may not be immediately available. Handwriting recognition is an
            // async operation that can take a variable amount of time depending on the length
            // of the text you want to recognize. You may need to wait or retry this operation.

            Log.d("myTag","\nHandwritten text submitted. Waiting 10 seconds to retrieve the recognized text.\n");
            Thread.sleep(10000);

            // Execute the second REST API call and get the response.
            HttpGet resultRequest = new HttpGet(operationLocation);
            resultRequest.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);

            HttpResponse resultResponse = resultClient.execute(resultRequest);
            HttpEntity responseEntity = resultResponse.getEntity();

            if (responseEntity != null)
            {
                // Format and display the JSON response.
                String jsonString = EntityUtils.toString(responseEntity);
                JSONObject json = new JSONObject(jsonString);
                Log.d("myTag","Text recognition result response: \n");
                Log.d("myTag",json.toString(2));
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
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
                preview.setImageBitmap(Bitmap.createScaledBitmap(photo, 3200,1811, false));
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
                        if(false) {
                            try {
                                QueryDatabase(BestFoodTag);
                            } catch (Exception e) {
                            }
                        }else{
                            AzureText(Bitmap.createScaledBitmap(photo, 2000,611, false));
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
