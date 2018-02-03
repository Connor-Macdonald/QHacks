package com.example.connormacdonald.qhackstest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ibm.watson.developer_cloud.android.library.camera.CameraHelper;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.NaturalLanguageUnderstanding;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.RecognizedText;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualRecognitionOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageText;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.Word;

import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalysisResults;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.AnalyzeOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Author;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.CategoriesResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.ConceptsOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.ConceptsResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EmotionOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EmotionScores;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EntitiesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.EntitiesResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.Features;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.KeywordsOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.KeywordsResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.MetadataOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.MetadataResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.RelationsOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.SemanticRolesOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.SemanticRolesResult;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.SentimentOptions;
import com.ibm.watson.developer_cloud.natural_language_understanding.v1.model.TargetedSentimentResults;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.getProperty;


public class MainActivity extends AppCompatActivity {
    private NaturalLanguageUnderstanding service;
    private String username = "9235e8e2-26fa-4ae5-aeea-371ff13ce8ff";
    private String password = "OvItlVOQGuzp";
    private VisualRecognition vrClient;
    private CameraHelper helper;
    private List<String> ClassifierIDS = new ArrayList<String>();
    private String BestImgTag, BestText;
    private Double BestImgScore = 0d;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vrClient = new VisualRecognition(
                VisualRecognition.VERSION_DATE_2016_05_20,
                getString(R.string.api_key));
        helper = new CameraHelper(this);
        ClassifierIDS.add("FoodProducts_1108557626");
        ClassifierIDS.add("food");
        service = new NaturalLanguageUnderstanding(NaturalLanguageUnderstanding.VERSION_DATE_2017_02_27);
        //service.setDefaultHeaders(getDefaultHeaders());
        service.setUsernameAndPassword(username, password);
        service.setEndPoint(getProperty("natural_language_understanding.url"));
    }

    public void takePicture(View view) {
        helper.dispatchTakePictureIntent();
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            String BestImgTag, BestText;
            Double BestImgScore = 0d;
            if (requestCode == CameraHelper.REQUEST_IMAGE_CAPTURE) {
                final Bitmap photo = helper.getBitmap(resultCode);
                final File photoFile = helper.getFile(resultCode);
                //ImageView preview = findViewById(R.id.preview);
                //preview.setImageBitmap(photo);
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        String BestImgTag = "na", BestText;
                        Double BestImgScore = 0d;
                        VisualClassification response =
                                vrClient.classify(
                                        new ClassifyImagesOptions.Builder()
                                                .classifierIds(ClassifierIDS)
                                                .images(photoFile)
                                                .build()
                                ).execute();

                        ImageClassification ImgClassification = response.getImages().get(0);
                        VisualClassifier ImgClassifier = ImgClassification.getClassifiers().get(0);

                        final StringBuffer output = new StringBuffer();
                        for (VisualClassifier.VisualClass object : ImgClassifier.getClasses()) {
                            Log.d("MyTag", object.getName());
                            if (object.getScore() > BestImgScore) {
                                BestImgScore = object.getScore();
                                BestImgTag = object.getName();
                            }

                            if (object.getScore() > 0.7f)
                                output.append("<").append(object.getName()).append("> ");
                        }
                        Log.d("MyTag", "Best Tag:" + BestImgTag);
                        //Text
                        RecognizedText response2 =
                                vrClient.recognizeText(new VisualRecognitionOptions.Builder()
                                        .images(photoFile).build()).execute();

                        ImageText TextIdentified = response2.getImages().get(0);
                        Log.d("MyTag", TextIdentified.getText());


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
                        for (ConceptsResult concept : results.getConcepts()) {
                            Log.d("MyTag", "Text: " + concept.getText());
                            Log.d("MyTag", "Reasource: " + concept.getDbpediaResource());
                            Log.d("MyTag", "Score: " + concept.getRelevance());
                        }
                        for (CategoriesResult result : results.getCategories()) {
                            Log.d("MyTag", "Label: " + result.getLabel());
                            Log.d("MyTag", "Score: " + result.getScore());
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView detectedObjects =
                                        findViewById(R.id.detected_objects);
                                detectedObjects.setText(output);
                            }
                        });
                    }
                });
            }
        } catch(Exception e){}
    }
}
