package me.elrhezzalimanal.autosportnewsreader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private ArrayList<NewsItem> news;

    private RecyclerView recyclerView;
    private NewsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        news = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView);
        adapter = new NewsAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

//        we won't set the data of our ArrayList news here because
//        adapter.setNews(news);

        new GetNews().execute();

    }

    private class GetNews extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            InputStream inputStream = getInputStream();
            if (null != inputStream) {
                try {
                    initXMLPullParser(inputStream);
                } catch (XmlPullParserException | IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //we set the data for our ArrayList news here inorder to be sure that the AsyncTask GetNews is finished
            adapter.setNews(news);
        }
    }

    //    creating a connection without libraries like Volley or Retrofit
    private InputStream getInputStream() {
        try {
            URL url = new URL("https://www.autosport.com/rss/feed/f1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);//to specify that we expect something from the connection
//            connection.setConnectTimeout(someInt);//Specifies until when your app will wait for a response
//            connection.setReadTimeout(someInt);   //Specifies until when your app will wait for a response
            return connection.getInputStream(); //Returns an input stream that reads from this open connection.
            //at this point if everything goes well
            // we have an internet connection + the url is valid
            // we will get an inputStream of data from the website

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initXMLPullParser(InputStream inputStream) throws XmlPullParserException, IOException {
        Log.d(TAG, "initXMLPullParser: initializing XML Pull Parser to get wanted data from the website's XML");
        XmlPullParser parser = Xml.newPullParser();// we will use it tp parse the XML we receive from the internet
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        // processing namespaces is the process of distinguishing between different XML tags with the same name
        parser.setInput(inputStream, null);
        // now let's parse the XML file and receive only the wanted informations
        // the parser at this point is at the beginning of our XML file
        // we need to move it through the elements to extract the data that we want
        parser.next(); //returns an int, moves parser to the next element:this next element can be the start of a tag
        // can be a TEXT, an end of a tag or the end of the document
        parser.require(XmlPullParser.START_TAG, null, "rss");//makes sure that we are inside the XML element rss
        //let's loop trough the elements that are inside the rss tag the loop will finish when we arrive to the END_TAG of rss
        while (parser.next() != XmlPullParser.END_TAG) {
            // check if the current event is not an opening tag which is the tag channel in our case
            // if it's not just skip this iteration
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            parser.require(XmlPullParser.START_TAG, null, "channel");//makes sure that we are inside the XML element channel

            //let's loop trough the elements that are inside the channel tag the loop will finish when we arrive to the END_TAG of channel
            while (parser.next() != XmlPullParser.END_TAG) {
                // check if the current event is not an opening tag which is an element inside the channel tag in our case
                // if it's not just skip this iteration
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                // now let's look for the item element inside the channel
                // parser.getName() returns the name of the current tag
                if (parser.getName().equals("item")) {
                    parser.require(XmlPullParser.START_TAG, null, "item");

                    String title = "";
                    String description = "";
                    String link = "";
                    String date = "";

                    while (parser.next() != XmlPullParser.END_TAG) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) {
                            continue;
                        }
                        String tagName = parser.getName();
                        if (tagName.equals("title")) {
                            title = getContent(parser, "title");

                        } else if (tagName.equals("description")) {
                            description = getContent(parser, "description");

                        } else if (tagName.equals("link")) {
                            link = getContent(parser, "link");

                        } else if (tagName.equals("pubDate")) {
                            date = getContent(parser, "pubDate");

                        } else {
                            skipTag(parser);
                        }
                    }

                    NewsItem newsItem = new NewsItem(title, description, link, date);
                    news.add(newsItem);


                } else {
                    skipTag(parser);
                }

            }

        }
    }

    private String getContent(XmlPullParser parser, String tagName) throws IOException, XmlPullParserException {
        //let's make sure that we are at the start of the tagName
        parser.require(XmlPullParser.START_TAG, null, tagName);
        String content = "";
        if (parser.next() == XmlPullParser.TEXT) { //if the parser is at the content of the tag
            content = parser.getText();//returns the content of the tag,then we need to move the parser to the next element
            parser.next();
        }
        return content;
    }

    private void skipTag(XmlPullParser parser) throws XmlPullParserException, IOException {
        // if the element that the parser is currently at isn't an opening tag,it isn't a tag
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        //if the parser is currently at a tag that is not a pubdate, link, description or title
        int number = 1;

        while (number != 0) {
            switch (parser.next()) {
                case XmlPullParser.START_TAG:
                    number++;
                    break;
                case XmlPullParser.END_TAG:
                    number--;
                    break;
                default:
                    break;
            }
        }

    }

}