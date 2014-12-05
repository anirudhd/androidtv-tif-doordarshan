package me.dewani.doordarshan;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaCodec;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import me.dewani.doordarshan.R;

import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

public class DoordarshanService extends TvInputService {

    public static final String TAG = DoordarshanService.class.getSimpleName();

    @Override
    public Session onCreateSession(String inputId) {
        return new StubSessionImpl(this);
    }

    static class StubSessionImpl extends Session implements TvMediaPlayer.Listener, TvMediaPlayer.InternalErrorListener, TvMediaPlayer.InfoListener {
        private static final int[] COLORS = {Color.RED, Color.GREEN, Color.BLUE};
        private final View mOverlayView;
        private final TextView mMessageView;
        private final TextView mStatusView;
        private Surface mSurface;
        private Context mContext;
        private TvMediaPlayer mPlayer;
        private final List<TvTrackInfo> mTracks = new LinkedList<>();

        StubSessionImpl(Context context) {

            super(context);
            try {
                mContext = context;
                mPlayer = new TvMediaPlayer(context, getRendererBuilder());
                mPlayer.setInternalErrorListener(this);
                mPlayer.setInfoListener(this);
                mPlayer.addListener(this);
                mTracks.clear();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
            setOverlayViewEnabled(true);


            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            mOverlayView = inflater.inflate(R.layout.overlay_view, null);
            mMessageView = (TextView) mOverlayView.findViewById(R.id.message);
            mMessageView.setVisibility(View.INVISIBLE);
            mStatusView = (TextView) mOverlayView.findViewById(R.id.tuner_status);
        }


        @Override
        public View onCreateOverlayView() {
            return mOverlayView;
        }


        @Override
        public void onRelease() {
            mPlayer.release();
        }


        @Override
        public boolean onSetSurface(Surface surface) {
            mSurface = surface;
            mPlayer.setSurface(mSurface);
            return true;
        }

        @Override
        public void onSetStreamVolume(float volume) {
            //mPlayer.setVolume(volume);
        }

        @Override
        public boolean onTune(final Uri channelUri) {

            try {
                Log.d(DoordarshanService.TAG, "Tuning channel");
                String[] projection = {
                        TvContract.Channels._ID,
                        TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA,
                        TvContract.Channels.COLUMN_DISPLAY_NAME
                };

                Cursor cursor = mContext.getContentResolver().query(channelUri, projection, null, null, null);
                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            byte[] bytes = cursor.getBlob(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA));
                            try {
                                String decoded = new String(bytes, "UTF-8");
                                Log.d(DoordarshanService.TAG, "Tuning channel: " + cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME)));
                                Log.d(DoordarshanService.TAG, "Channel stream: " + decoded);
                                mPlayer.tuneTo(decoded);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                        }
                    } finally {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                Log.d(DoordarshanService.TAG, e.toString());
            }

            return true;
        }

        private void addTrack(TvTrackInfo track) {
            for (TvTrackInfo temp : mTracks) {
                if (temp.getType() == track.getType() && temp.getId().equals(track.getId())) {
                    mTracks.remove(temp);
                    break;
                }
            }
            mTracks.add(track);
            notifyTracksChanged(mTracks);
            notifyTrackSelected(track.getType(), track.getId());
        }

        @Override
        public boolean onSelectTrack(int type, String trackId) {
            notifyTrackSelected(type, trackId);
            return true;
        }

        @Override
        public void onSetCaptionEnabled(boolean enabled) {
        }


        private TvMediaPlayer.RendererBuilder getRendererBuilder() {
            // Implement later for HLS + MP4 + SmoothStreaming
            // For now everything is HLS
            return null;

        }

        @Override
        public void onStateChanged(boolean playWhenReady, int playbackState) {
        }

        @Override
        public void onError(Exception e) {

        }

        @Override
        public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {

            TvTrackInfo track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_VIDEO, "video")
                    .setLanguage("en")
                    .setVideoWidth(width)
                    .setVideoHeight(height)
                    .build();
            addTrack(track);
            mStatusView.setText("Video size: " + width + "," + height);

        }

        @Override
        public void onPlaying() {
            notifyVideoAvailable();

        }

        @Override
        public void onBuffering() {
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_BUFFERING);

        }

        @Override
        public void onRendererInitializationError(Exception e) {
            Log.d(TAG, e.toString());
        }

        @Override
        public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
            Log.d(TAG, e.toString());
        }

        @Override
        public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
            mStatusView.setText("Exoplayer: " + e.toString());

            Log.d(TAG, e.toString());
        }

        @Override
        public void onCryptoError(MediaCodec.CryptoException e) {
            Log.d(TAG, e.toString());

        }

        @Override
        public void onUpstreamError(int sourceId, IOException e) {
            Log.d(TAG, e.toString());

        }

        @Override
        public void onConsumptionError(int sourceId, IOException e) {
            Log.d(TAG, e.toString());

        }

        @Override
        public void onDrmSessionManagerError(Exception e) {
            Log.d(TAG, e.toString());
        }

        @Override
        public void onVideoFormatEnabled(String formatId, int trigger, int mediaTimeMs) {

        }

        @Override
        public void onAudioFormatEnabled(String formatId, int trigger, int mediaTimeMs) {

        }

        @Override
        public void onDroppedFrames(int count, long elapsed) {
            mStatusView.setText("Frames dropped: " + count);
        }

        @Override
        public void onBandwidthSample(int elapsedMs, long bytes,long bitrateEstimate) {
            mStatusView.setText(String.format("elapsed: %s , bytes: %d bitrateEstimate: %d",elapsedMs,bytes, bitrateEstimate));


        }

        @Override
        public void onLoadStarted(int sourceId, String formatId, int trigger, boolean isInitialization, int mediaStartTimeMs, int mediaEndTimeMs, long length) {

        }

        @Override
        public void onLoadCompleted(int sourceId, long bytesLoaded) {

        }
    }
}
