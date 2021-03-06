package com.example.tushar.pdfdemoapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    /**
     * For identifying current view mode read/create/listing/options
     *
     *
     */
    interface CurrentView {
        int OPTIONS_LAYOUT = 1;
        int READ_LAYOUT = 2;
        int WRITE_LAYOUT = 3;
        int PDF_SELECTION_LAYOUT = 4;
    }

    /**
     * FrameLayout child views. We will manage our UI to one layout Hide/Show
     * these views as per requirement
     */
    LinearLayout optionsLayout;
    LinearLayout readLayout;
    LinearLayout writeLayout;
    LinearLayout pdfSelectionLayout;

    static int currentView;

    // Pdf content will be generated with User Input Text
    EditText pdfContentView;

    // For navigating back
    MenuItem closeOption;

    // List view for showing pdf files
    ListView pdfListView;

    // Background task to generate pdf file listing
    PdfListLoadTask listLoadTask;

    // Adapter to list view
    ArrayAdapter<String> adapter;

    // array of pdf files
    File[] filelist;

    // index to track currentPage in rendered Pdf
    static int currentPage = 0;

    // View on which Pdf content will be rendered
    ImageView pdfImageView;

    // Currently rendered Pdf file
    String openPdfFileName;

    Button gentatePdf;
    Button next;
    Button previous;


    // File Descriptor for rendered Pdf file
    ParcelFileDescriptor mParcelFileDescriptor;

    // For rendering a PDF document
    PdfRenderer mPdfRenderer;

    // For opening current page, render it, and close the page
    PdfRenderer.Page mCurrentPage;

    Button readPdf;
    Button createPdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        optionsLayout = (LinearLayout)findViewById(R.id.options_layout);
        readLayout = (LinearLayout)findViewById(R.id.read_layout);
        writeLayout = (LinearLayout)findViewById(R.id.write_layout);
        pdfSelectionLayout = (LinearLayout)findViewById(R.id.pdf_selection_layout);

        pdfContentView = (EditText)findViewById(R.id.pdf_content);

        readPdf = (Button)findViewById(R.id.read_pdf);
        createPdf = (Button)findViewById(R.id.create_pdf);

        next = (Button)findViewById(R.id.next);
        previous = (Button)findViewById(R.id.previous);
        gentatePdf = (Button)findViewById(R.id.genrate_pdf);

        pdfImageView = (ImageView)findViewById(R.id.pdfview);

        pdfListView = (ListView)findViewById(R.id.pdflist);

        pdfListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // On Clicking list item, Render Pdf file corresponding to
                // filePath
                openPdfFileName = adapter.getItem(position);
                openRender(openPdfFileName);
                updateView(CurrentView.READ_LAYOUT);
            }
        });

        currentView = CurrentView.OPTIONS_LAYOUT;

        readPdf.setOnClickListener(this);
        createPdf.setOnClickListener(this);
        gentatePdf.setOnClickListener(this);
        next.setOnClickListener(this);
        previous.setOnClickListener(this);

//        readPdf.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                updateView(CurrentView.PDF_SELECTION_LAYOUT);
//
//            }
//        });
//
//        createPdf.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                updateView(CurrentView.WRITE_LAYOUT);
//            }
//        });
//
//
//        gentatePdf.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                if (pdfContentView.getText().toString().isEmpty())
//                {
//                    Toast.makeText(getApplicationContext(),
//                            "Please enter text to generate Pdf", Toast.LENGTH_SHORT)
//                            .show();
//                }
//                else {
//                    new PdfGenerationTask().execute();
//                    v.setEnabled(false);
//                }
//            }
//        });
//
//        next.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                currentPage++;
//                showPage(currentPage);
//
//            }
//        });
//
//        previous.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                currentPage--;
//                showPage(currentPage);
//
//            }
//        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main,menu);
        closeOption = menu.findItem(R.id.action_close);
        closeOption.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_close)
        {
            if (currentView == CurrentView.PDF_SELECTION_LAYOUT)
            {
                updateView(CurrentView.OPTIONS_LAYOUT);
                updateActionBarText();
            }
            else if (currentView == CurrentView.READ_LAYOUT)
            {
                if (listLoadTask != null)
                    listLoadTask.cancel(true);
                listLoadTask = new PdfListLoadTask();
                listLoadTask.execute();
                updateView(CurrentView.PDF_SELECTION_LAYOUT);

            }
            else if (currentView == CurrentView.WRITE_LAYOUT)
            {
                hideInputMethodIfShow();
                updateView(CurrentView.OPTIONS_LAYOUT);

            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback for handling view click events
     */
    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.read_pdf:
                updateView(CurrentView.PDF_SELECTION_LAYOUT);
                break;
            case R.id.create_pdf:
                updateView(CurrentView.WRITE_LAYOUT);
                break;
            case R.id.genrate_pdf:
                if (pdfContentView.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(),
                            "Please enter text to generate Pdf", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    new PdfGenerationTask().execute();
                    v.setEnabled(false);
                }
                break;
            case R.id.next:
                currentPage++;
                showPage(currentPage);
                break;
            case R.id.previous:
                currentPage--;
                showPage(currentPage);
                break;
        }

    }

    /**
     * API for initializing file descriptor and pdf renderer after selecting pdf
     * from list
     *
     * @param filePath
     */
    private void openRender(String filePath) {

        File file = new File(filePath);

        try {
            mParcelFileDescriptor = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
            mPdfRenderer = new PdfRenderer(mParcelFileDescriptor);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * API for cleanup of objects used in rendering
     */
    private void closeRender() {

        try {

            if (mCurrentPage != null)
                mCurrentPage.close();
            if (mPdfRenderer != null)
                mPdfRenderer.close();
            if (mParcelFileDescriptor != null)
                mParcelFileDescriptor.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    /**
     * API to update View
     *
     * @param updateView updateView specifies the target view
     */
    private void updateView(int updateView) {

        switch (updateView)
        {
            case CurrentView.OPTIONS_LAYOUT:
                currentView = CurrentView.OPTIONS_LAYOUT;
                closeOption.setVisible(false);
                optionsLayout.setVisibility(View.VISIBLE);
                readLayout.setVisibility(View.INVISIBLE);
                writeLayout.setVisibility(View.INVISIBLE);
                pdfSelectionLayout.setVisibility(View.INVISIBLE);
                break;

            case CurrentView.READ_LAYOUT:
                currentView = CurrentView.READ_LAYOUT;
                showPage(currentPage);

                closeOption.setVisible(true);

                optionsLayout.setVisibility(View.INVISIBLE);
                readLayout.setVisibility(View.VISIBLE);
                writeLayout.setVisibility(View.INVISIBLE);
                pdfSelectionLayout.setVisibility(View.INVISIBLE);
                break;

            case CurrentView.PDF_SELECTION_LAYOUT:
                currentView = CurrentView.PDF_SELECTION_LAYOUT;

                closeRender();

                if (listLoadTask != null)
                    listLoadTask.cancel(true);

                listLoadTask = new PdfListLoadTask();
                listLoadTask.execute();

                optionsLayout.setVisibility(View.INVISIBLE);
                readLayout.setVisibility(View.INVISIBLE);
                writeLayout.setVisibility(View.INVISIBLE);
                pdfSelectionLayout.setVisibility(View.VISIBLE);
                break;

            case CurrentView.WRITE_LAYOUT:
                currentView = CurrentView.WRITE_LAYOUT;

                optionsLayout.setVisibility(View.INVISIBLE);
                readLayout.setVisibility(View.INVISIBLE);
                writeLayout.setVisibility(View.VISIBLE);
                pdfSelectionLayout.setVisibility(View.INVISIBLE);
                break;



        }
    }

    /**
     * API to update ActionBar text
     */
    private void updateActionBarText() {

        if (currentView == CurrentView.READ_LAYOUT)
        {
            int index = mCurrentPage.getIndex();
            int pageCount = mPdfRenderer.getPageCount();
            previous.setEnabled( 0 != index);
            next.setEnabled(index + 1 < pageCount);
            getActionBar().setTitle(openPdfFileName + "(" + index + 1 + "/ "+ pageCount + ")");
        }
        else {
            getActionBar().setTitle(R.string.app_name);
        }
    }

    /**
     * Handler back key Update UI current view is not options view else call
     * super.onBackPressed()
     */
    @Override
    public void onBackPressed() {

        if (currentView == CurrentView.PDF_SELECTION_LAYOUT)
        {
            updateView(CurrentView.OPTIONS_LAYOUT);
            updateActionBarText();
        }
        else if (currentView == CurrentView.READ_LAYOUT)
        {
            if (listLoadTask != null)
                listLoadTask.cancel(true);
            listLoadTask = new PdfListLoadTask();
            listLoadTask.execute();
            updateView(CurrentView.PDF_SELECTION_LAYOUT);

        }
        else if (currentView == CurrentView.WRITE_LAYOUT)
        {
            hideInputMethodIfShow();
            updateView(CurrentView.OPTIONS_LAYOUT);

        }
        else {
            super.onBackPressed();
        }
    }

    /**
     * API to hide keyboard if shown Will be used when user naviagates after
     * generating Pdf
     */
    private void hideInputMethodIfShow() {

        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(pdfContentView.getWindowToken(),0,null);

    }

    /**
     * API show to particular page index using PdfRenderer
     *
     * @param index
     */
    private void showPage(int index) {

        if (mPdfRenderer == null || mPdfRenderer.getPageCount() <= index || index<0 )
        {
            return;
        }
        // For closing the current page before opening another one.
        try {
            if (mCurrentPage != null)
                mCurrentPage.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        // Open page with specified index
        mCurrentPage = mPdfRenderer.openPage(index);

        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(),mCurrentPage.getHeight(), Bitmap.Config.ARGB_8888);

        // Pdf page is rendered on Bitmap
        mCurrentPage.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Set rendered bitmap to ImageView
        pdfImageView.setImageBitmap(bitmap);
        updateActionBarText();

    }



    /**
     * Background task for listing pdf files
     *
     * @author androidsrc.net
     */
    class PdfListLoadTask extends AsyncTask
    {

        @Override
        protected Object doInBackground(Object[] params) {

            File file = new File("/sdcard/pdfFempAppData");
            filelist = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".pdf");
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {

            if (filelist != null && filelist.length >=1 )
            {
                ArrayList<String> arrayList = new ArrayList<>();
                for (int i = 0; i<filelist.length; i++)
                    arrayList.add(filelist[i].getPath());
                adapter = new ArrayAdapter<String>(getApplicationContext(),R.layout.list_item,arrayList);
                pdfListView.setAdapter(adapter);
            }
            else {
                Toast.makeText(getApplicationContext(),
                        "No pdf file found, Please create new Pdf file",
                        Toast.LENGTH_LONG).show();
                updateView(CurrentView.OPTIONS_LAYOUT);
                updateActionBarText();
            }
        }
    }


    /**
     * Background task to generate pdf from users content
     *
     *
     */
    class PdfGenerationTask extends AsyncTask<Void,Void,String>
    {
        File outputFile;

        @Override
        protected String doInBackground(Void... params) {


            PdfDocument pdfDocument = new PdfDocument();

            // repaint the user's text into the page
            View content = findViewById(R.id.pdf_content);

            // crate a page description
            int pageNumber = 1;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(content.getWidth(),
                    content.getHeight() - 20, pageNumber).create();


            // create a new page from the PageInfo
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            content.draw(page.getCanvas());

            // do final processing of the page
            pdfDocument.finishPage(page);

            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyhhmmss");
            String pdfName = "pdfdemo"
                    + sdf.format(Calendar.getInstance().getTime()) + ".pdf";

            File outputFile = new File("/sdcard/PDFDemo_AndroidSRC/", pdfName);

            if (isExternalStorageAvailable()) {
//                outputFile = new File("/sdcard/PDFDemo_AndroidSRC/", pdfName);
                Log.v("MainActivity","Extenal storage avaalable "+outputFile.getAbsolutePath());
            }
            else if (!isExternalStorageAvailable())
            {
//                outputFile = new File(pdfName);
                Log.v("MainActivity","Extenal storage avaalable "+outputFile.getAbsolutePath());
            }

            try {
                outputFile.createNewFile();
                OutputStream out = new FileOutputStream(outputFile);
                pdfDocument.writeTo(out);

                pdfDocument.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return outputFile.getPath();
        }

        @Override
        protected void onPostExecute(String filePath) {

            if (filePath != null)
            {
                gentatePdf.setEnabled(true);
                pdfContentView.setText("");
                Toast.makeText(getApplicationContext(),
                        "Pdf saved at " + filePath, Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getApplicationContext(),
                        "Error in Pdf creation" + filePath, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

}
