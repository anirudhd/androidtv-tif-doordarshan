package me.dewani.doordarshan;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by anirudhd on 11/26/14.
 */
public class EpgHelper {

    private static final String TAG = EpgHelper.class.getSimpleName();

    /**
     * Add TV channels to TVProvider
     *
     * @param resolver
     * @param info
     */
    public static void insertChannels(ContentResolver resolver, TvInputInfo info, String token) {
        if (!info.getServiceInfo().name.equals(DoordarshanService.class.getName())) {
            throw new IllegalArgumentException("info mismatch");
        }
        try {

            List<Channel> channels = getChannels("http://autosample.appspot.com/get?token=" + token);
            for (int i = 0; i < channels.size(); i++) {
                ContentValues redValues = new ContentValues();
                redValues.put(TvContract.Channels.COLUMN_INPUT_ID, info.getId());
                redValues.put(TvContract.Channels.COLUMN_DISPLAY_NUMBER, "" + (800 + i));
                redValues.put(TvContract.Channels.COLUMN_DISPLAY_NAME, channels.get(i).name);
                redValues.put(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID, i);
                redValues.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA, channels.get(i).url.getBytes(Charset.forName("UTF-8")));
                String channel = resolver.insert(TvContract.Channels.CONTENT_URI, redValues).toString();
                Log.d(TAG, "Added:" + channel);

                ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<>();

                // create 10 fake programs
                for (int j = 0; j < 10; j++) {
                    contentProviderOperations.add(ContentProviderOperation
                            .newInsert(TvContract.Programs.CONTENT_URI)
                            .withValue(TvContract.Programs.COLUMN_CHANNEL_ID, channel.replaceFirst(".*/([^/?]+).*", "$1"))
                            .withValues(getProgramValues(channels.get(i), j))
                            .build());
                }

                ContentProviderResult[] contentProviderResults =
                        resolver.applyBatch(TvContract.AUTHORITY, contentProviderOperations);


            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
    }

    /**
     * Generate program data from channel.
     *
     * @param channel
     * @param j
     * @return
     */
    private static ContentValues getProgramValues(Channel channel, int j) {
        ContentValues values = new ContentValues();
        long now = System.currentTimeMillis();
        values.put(TvContract.Programs.COLUMN_TITLE, channel.name);


        values.put(TvContract.Programs.COLUMN_START_TIME_UTC_MILLIS, now + (j * 30) * 24 * 60 * 60 * 1000);
        values.put(TvContract.Programs.COLUMN_END_TIME_UTC_MILLIS, now + ((j + 1) * 30) * 24 * 60 * 60 * 1000 - 1);

        values.put(TvContract.Programs.COLUMN_SHORT_DESCRIPTION, "HLS stream from partner");
        values.put(TvContract.Programs.COLUMN_THUMBNAIL_URI, channel.logoUrl);
        Log.d(TAG, channel.name + " " + channel.logoUrl);
        values.put(TvContract.Programs.COLUMN_POSTER_ART_URI, channel.logoUrl);
        values.put(TvContract.Programs.COLUMN_INTERNAL_PROVIDER_DATA, channel.url);
        return values;
    }

    public static void deleteChannels(ContentResolver resolver, TvInputInfo info) {
        if (!info.getServiceInfo().name.equals(DoordarshanService.class.getName())) {
            throw new IllegalArgumentException("info mismatch");
        }
        resolver.delete(TvContract.buildChannelsUriForInput(info.getId()), null, null);
    }

    /**
     * Fetch channel data from n/w.
     *
     * @param channelUrl
     * @return
     * @throws JSONException
     */
    public static List<Channel> getChannels(String channelUrl)
            throws JSONException {
        InputStream is = null;
        List<Channel> channels = null;
        try {
            java.net.URL url = new java.net.URL(channelUrl);
            URLConnection urlConnection = url.openConnection();
            is = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream(), "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            Gson gson = new Gson();
            channels = gson.fromJson(json, new TypeToken<List<Channel>>() {
            }.getType());


        } catch (Exception e) {
            Log.d(TAG, "Failed to parse the json for media list", e);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.d(TAG, "JSON feed closed", e);
                }
            }
        }

        return channels;

    }

}
