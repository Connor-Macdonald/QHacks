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
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.RecognizedText;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualRecognitionOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ImageText;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.Word;
import java.io.File;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private VisualRecognition vrClient;
    private CameraHelper helper;
    private List<String> ClassifierIDS;
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
    }

    public void takePicture(View view) {
        helper.dispatchTakePictureIntent();
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String BestImgTag, BestText;
        Double BestImgScore = 0d;
        if(requestCode == CameraHelper.REQUEST_IMAGE_CAPTURE) {
            final Bitmap photo = helper.getBitmap(resultCode);
            final File photoFile = helper.getFile(resultCode);
            //ImageView preview = findViewById(R.id.preview);
            //preview.setImageBitmap(photo);
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    String BestImgTag, BestText;
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
                    for(VisualClassifier.VisualClass object: ImgClassifier.getClasses()) {
                        Log.d("MyTag",object.getName());
                        if(object.getScore() > BestImgScore){
                            BestImgScore = object.getScore();
                             BestImgTag = object.getName();
                        }

                        if(object.getScore() > 0.7f)
                            output.append("<").append(object.getName()).append("> ");
                    }
                    //Text
                    RecognizedText response2 =
                            vrClient.recognizeText(new  VisualRecognitionOptions.Builder()
                                    .images(photoFile).build()).execute();

                    ImageText TextIdentified = response2.getImages().get(0);

                    for(Word words: TextIdentified.getWords()) {
                        Log.d("MyTag",words.getWord());
                        if(words.getScore() > 0.7f)
                            output.append("<")
                                    .append(words.getWord())
                                    .append("> ");
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
    }
}
