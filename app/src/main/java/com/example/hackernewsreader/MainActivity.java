package com.example.hackernewsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    Map<Integer, String> articleUrls = new HashMap<Integer, String>();
    Map<Integer, String> articleTitles = new HashMap<Integer, String>();
    ArrayList<Integer> articleIds = new ArrayList<Integer>();

    SQLiteDatabase sqLiteDatabase;
    ArrayAdapter arrayAdapter;
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> urls = new ArrayList<String>();
    ArrayList<String> content = new ArrayList<String>();

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data = inputStreamReader.read();

                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();
                }


                JSONArray jsonArray = new JSONArray(result);
                sqLiteDatabase.execSQL("DELETE FROM articles");
                for (int i=0; i< 20; i++) {
//                    String title = "";
//                    URL url = "";
//                Log.i("Here", jsonArray.getString(i));
                    String articleId = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);

                    data = inputStreamReader.read();

                    while (data != -1) {
                        char current = (char) data;
                        result += current;

                        data = inputStreamReader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleUrls);

//                Log.i("here", jsonObject.toString());
                    String articleTitle;
                    String articleUrl;
                    if (jsonObject.has("title")) {
                        articleTitle = jsonObject.getString("title");
                    } else {
                        articleTitle = "";
                    }
                    if (jsonObject.has("url")) {
                        articleUrl = jsonObject.getString("url");
                    } else {
                        articleUrl = "";
                    }

//                    String articleTitle = jsonObject.getString("title");
//                    String articleUrl = jsonObject.getString("url");

//                    url = new URL(url_);
//                    urlConnection = (HttpURLConnection) url.openConnection();
//                    inputStream = urlConnection.getInputStream();
//                    inputStreamReader = new InputStreamReader(inputStream);
//
//                    data = inputStreamReader.read();
//
                    String articleContent = "";
//
//                    while (data != -1) {
//                        char current = (char) data;
//                        result += current;
//
//                        data = inputStreamReader.read();
//                    }


                    articleUrls.put(Integer.valueOf(articleId), articleUrl);
                    articleTitles.put(Integer.valueOf(articleId), articleTitle);
                    articleIds.add(Integer.valueOf(articleId));
//                Log.i("Here", title);
//                Log.i("Here", url);
                    String sql = "INSERT INTO articles (articleId, url, title, content) VALUES (?,?,?, ?)";

                    SQLiteStatement sqLiteStatement = sqLiteDatabase.compileStatement(sql);
                    sqLiteStatement.bindString(1, articleId);
                    sqLiteStatement.bindString(2, articleUrl);
                    sqLiteStatement.bindString(3, articleTitle);
                    sqLiteStatement.bindString(4, articleContent);

                    sqLiteStatement.execute();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
//            Log.i("qqqqq", "error");
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i("Click", urls.get(i));
                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("articleUrl", urls.get(i));
                intent.putExtra("content", content.get(i));
                startActivity(intent);
            }
        });

        sqLiteDatabase = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");

        updateListView();

        DownloadTask downloadTask = new DownloadTask();

        try {
            downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
            updateListView();

//            Log.i("Here ids",articleIds.toString());
//            Log.i("Here titles",articleTitles.toString());
//            Log.i("Here urls",articleUrls.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void updateListView() {
        try {
            Log.i("UpdateList View", "Done");
            Cursor c = sqLiteDatabase.rawQuery("SELECT * FROM articles ORDER BY articleId DESC", null);
            int indexContent = c.getColumnIndex("content");
            int indexUrl = c.getColumnIndex("url");
            int indexTitle = c.getColumnIndex("title");

            c.moveToFirst();
            titles.clear();
            urls.clear();
            while (c != null) {
                titles.add(c.getString(indexTitle));
                urls.add(c.getString(indexUrl));
                content.add(c.getString(indexContent));

                c.moveToNext();
            }
            arrayAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
