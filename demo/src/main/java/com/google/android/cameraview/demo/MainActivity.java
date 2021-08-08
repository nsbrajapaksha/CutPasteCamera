package com.google.android.cameraview.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final int PICK_OUTLINE_IMAGE = 1;
    public static final int PICK_INPUT_IMAGE = 2;
    public static final int PICK_FOREGROUND_IMAGE = 3;
    Uri outline_uri = null;
    Uri input_uri = null;
    Uri foreground_uri = null;


    Button proceed_btn;

    EditText name1;
    EditText location1X;
    EditText location1Y;
    EditText font1Name;
    EditText font1Size;
    EditText font1Color;

    EditText name2;
    EditText location2X;
    EditText location2Y;
    EditText font2Name;
    EditText font2Size;
    EditText font2Color;

    EditText locationCapturedX;
    EditText locationCapturedY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        proceed_btn = findViewById(R.id.proceed_btn);

        name1 = findViewById(R.id.name1Text);
        location1X = findViewById(R.id.location1X);
        location1Y = findViewById(R.id.location1Y);
        font1Name = findViewById(R.id.font1Name);
        font1Color = findViewById(R.id.font1Color);
        font1Size = findViewById(R.id.font1Size);

        name2 = findViewById(R.id.name2Text);
        location2X = findViewById(R.id.location2X);
        location2Y = findViewById(R.id.location2Y);
        font2Name = findViewById(R.id.font2Name);
        font2Color = findViewById(R.id.font2Color);
        font2Size = findViewById(R.id.font2Size);

        locationCapturedX = findViewById(R.id.capturedX);
        locationCapturedY = findViewById(R.id.capturedY);
    }

    public void pickOutlineImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_OUTLINE_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_OUTLINE_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                return;
            }
            outline_uri = data.getData();
        } else if (requestCode == PICK_INPUT_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }
            input_uri = data.getData();
        }
        else if (requestCode == PICK_FOREGROUND_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                return;
            }
            foreground_uri = data.getData();
        }
//        proceed_btn.setEnabled(input_uri != null && outline_uri != null && foreground_uri != null); // if both images selected, allow to proceed
    }

    public void pickOutlineImage(View view) {
        pickOutlineImage();
    }

    public void pickInputImage(View view) {
        pickInputImage();
    }

    private void pickInputImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_INPUT_IMAGE);
    }

    public void proceed(View view) {
        TextDisplay textDisplay1 = new TextDisplay();
        textDisplay1.setName(name1.getText().toString().trim().isEmpty() ? null : name1.getText().toString());
        textDisplay1.setLocationX(location1X.getText().toString().trim().isEmpty() ? -1 : Integer.parseInt(location1X.getText().toString().trim()));
        textDisplay1.setLocationY(location1Y.getText().toString().trim().isEmpty() ? -1 : Integer.parseInt(location1Y.getText().toString().trim()));
        textDisplay1.setFontName(font1Name.getText().toString().trim().isEmpty() ? null : font1Name.getText().toString());
        textDisplay1.setFontSize(font1Size.getText().toString().trim().isEmpty() ? -1 : Integer.parseInt(font1Size.getText().toString()));
        textDisplay1.setColor(font1Color.getText().toString().trim().isEmpty() ? -1 : Color.parseColor(font1Color.getText().toString()));

        TextDisplay textDisplay2 = new TextDisplay();
        textDisplay2.setName(name2.getText().toString().trim().isEmpty() ? null : name2.getText().toString());
        textDisplay2.setLocationX(location2X.getText().toString().trim().isEmpty() ? -1 : Integer.parseInt(location2X.getText().toString().trim()));
        textDisplay2.setLocationY(location2Y.getText().toString().trim().isEmpty() ? -1 : Integer.parseInt(location2Y.getText().toString().trim()));
        textDisplay2.setFontName(font2Name.getText().toString().trim().isEmpty() ? null : font2Name.getText().toString());
        textDisplay2.setFontSize(font2Size.getText().toString().trim().isEmpty() ? -1 : Integer.parseInt(font2Size.getText().toString()));
        textDisplay2.setColor(font2Color.getText().toString().trim().isEmpty() ? -1 : Color.parseColor(font2Color.getText().toString()));

        ArrayList<Uri> uris = new ArrayList<>();
        ArrayList<TextDisplay> inputs = new ArrayList<>();
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);

        uris.add(outline_uri);
        uris.add(input_uri);
        uris.add(foreground_uri);
        intent.putParcelableArrayListExtra("picked_images", uris);

        inputs.add(textDisplay1);
        inputs.add(textDisplay2);

        intent.putExtra("inputs", inputs);
        intent.putExtra("capturedX", locationCapturedX.getText().toString().trim().isEmpty() ? -1 : Integer.parseInt(locationCapturedX.getText().toString().trim()));
        intent.putExtra("capturedY", locationCapturedY.getText().toString().trim().isEmpty() ? -1 : Integer.parseInt(locationCapturedY.getText().toString().trim()));

        startActivity(intent);
    }

    public void pickForegroundImage(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_FOREGROUND_IMAGE);
    }
}
