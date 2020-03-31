package com.aydhamm.percalender;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import ir.hamsaa.persiandatepicker.Listener;
import ir.hamsaa.persiandatepicker.PersianDatePickerDialog;
import ir.hamsaa.persiandatepicker.util.PersianCalendar;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button selectDateButton, folderChooser;
    TextView dateTextView, chosenFolderPath;
    String[] dateParams;
    String date;
    DateConverter dateConverter;
    int resultCode, requestCode = 9999, defaultYear, defaultMonth, defaultDay;
    Intent data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //references:
        selectDateButton = findViewById(R.id.datePickerButton);
        dateTextView = findViewById(R.id.dateTextView);
        folderChooser = findViewById(R.id.folderChooserButton);
        chosenFolderPath = findViewById(R.id.folderPathTextView);


        final PersianCalendar initDate = new PersianCalendar();

        // set today date as default
        defaultYear = initDate.getPersianYear();
        defaultMonth = initDate.getPersianMonth();
        defaultDay =  initDate.getPersianDay();
        dateTextView.setText(defaultYear+ "/" + defaultMonth + "/" + defaultDay);

        selectDateButton.setOnClickListener(new View.OnClickListener() { // show persian date picker when its button is clicked
            @Override
            public void onClick(View v) {
                initDate.setPersianDate(defaultYear,defaultMonth, defaultDay);

                PersianDatePickerDialog picker = new PersianDatePickerDialog(MainActivity.this)
                        .setPositiveButtonString(getString(R.string.ok))
                        .setNegativeButton(getString(R.string.cancel))
                        .setTodayButton(getString(R.string.today))
                        .setTodayButtonVisible(true)
                        .setInitDate(initDate)
                        .setMaxYear(1399)
                        .setMinYear(1300)
                        .setActionTextColor(Color.GRAY)
                        .setListener(new Listener() {
                            @Override
                            public void onDateSelected(PersianCalendar persianCalendar) {
                                dateTextView.setText(persianCalendar.getPersianYear() + "/" + persianCalendar.getPersianMonth() + "/" + persianCalendar.getPersianDay());
                                if(data != null){ // if there is a folder selected, show result after user selected date!
                                    MainActivity.this.onActivityResult(requestCode,resultCode,data);
                                }

                                // set previous selected date as default
                                defaultYear = persianCalendar.getPersianYear();
                                defaultMonth = persianCalendar.getPersianMonth();
                                defaultDay = persianCalendar.getPersianDay();
                            }
                            @Override
                            public void onDisimised() { }
                        });
                picker.show();
            }
        });

        folderChooser.setOnClickListener(new View.OnClickListener() { // open document tree for user to select a folder
            @Override
            public void onClick(View v) {
                String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

                if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) { // if read storage permission isn't generated, ask user to generate it
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, 0x4);
                }
                else{
                    MainActivity.this.openDocumentTree();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // if storage permission is generated, open document tree!
        if(ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED)
            this.openDocumentTree();

        else Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == this.requestCode) {
            String folderPath, realFolderPath;
            try {
                folderPath = data.getData().getLastPathSegment();

                switch (folderPath.charAt(0)) { // make selected folder path standard
                    case 'p':
                        realFolderPath = "/storage/emulated/0/" + folderPath.substring(8);
                        chosenFolderPath.setText(realFolderPath);
                        break;
                    case 'r':
                        realFolderPath = folderPath.substring(4);
                        chosenFolderPath.setText(realFolderPath);
                        break;
                    case 'd':
                        realFolderPath = "/storage/emulated/0/" + folderPath;
                        chosenFolderPath.setText(realFolderPath);
                        break;
                    default:
                        realFolderPath = "/storage/emulated/0/";
                }

                dateParams = dateTextView.getText().toString().split("/"); // assign the year, month and day to a StringArray for converting to gregorian date

                dateConverter = new DateConverter();
                dateConverter.persianToGregorian(Integer.parseInt(dateParams[0]), Integer.parseInt(dateParams[1]), Integer.parseInt(dateParams[2]));
                date = dateConverter.getYear() + "/" + dateConverter.getMonth() + "/" + dateConverter.getDay();

                File dir = new File(realFolderPath);
                final File[] files = dir.listFiles();
                List<File> modifiedFilesList = new ArrayList<>();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

                for (File file : files) {
                    if (!file.isDirectory()) {
                        if (date.equals(sdf.format(file.lastModified()))) {
                            modifiedFilesList.add(file);
                        }
                    }
                }

                final File[] modifiedFiles = modifiedFilesList.toArray(new File[0]);

                ListAdapter listViewAdapter = new ListViewAdapter(getApplicationContext(), modifiedFiles);
                ListView listView = findViewById(R.id.filesListView);
                listView.setAdapter(listViewAdapter);

                if (listViewAdapter.getCount() == 0)
                    Toast.makeText(this, getString(R.string.no_files_modified_on_this_date), Toast.LENGTH_LONG).show();

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        openFile(modifiedFiles[position]);
                    }

                });

                this.resultCode = requestCode;
                this.data = data;

            } catch (NullPointerException ignored) { // NullPointerException is thrown when user exits open document tree without choosing a folder
                if (this.data != null) // if data isn't null, it means there is a folder selected previously! so we check the files in previous folder again!
                    this.onActivityResult(this.requestCode, this.resultCode, this.data);
            }
        }
    }

    private void openFile(File file) {
        Uri uri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider",file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, MimeTypeMap.getSingleton().getExtensionFromMimeType(file.getName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Open " + file.getName() + " with ..."));
    }

    private void openDocumentTree(){
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.putExtra("android.content.extra.SHOW_ADVANCED", true);
        i.putExtra("android.content.extra.FANCY", true);
        i.putExtra("android.content.extra.SHOW_FILESIZE", true);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(Intent.createChooser(i, "Choose directory"), requestCode);
    }
}