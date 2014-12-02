package com.example.android.tv.doordarshan;

import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.FrameworkSampleSource;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.SampleSource;

/**
 * Created by anirudhd on 11/25/14.
 */
public class DoordarshanPlayer implements ExoPlayer.Listener, MediaCodecVideoTrackRenderer.EventListener {


    private final TvMediaPlayer.Listener mListener;
    private boolean mPreparing = true;


    ExoPlayer mPlayer;
    public static final int RENDERER_COUNT = 2;
    private Context mContext;
    private MediaCodecVideoTrackRenderer videoRenderer;
    private Surface mSurface;
    private MediaCodecAudioTrackRenderer audioRenderer;
    private float mVolume;
    private TuneTask mTuneTask;

    private static final int RENDERER_TYPE_VIDEO = 0;
    private static final int RENDERER_TYPE_AUDIO = 1;

    public DoordarshanPlayer(Context context, TvMediaPlayer.Listener listener) {
        mContext = context;
        mPlayer = ExoPlayer.Factory.newInstance(RENDERER_COUNT, 1000, 5000);
        mPlayer.addListener(this);
        mSurface = null;
        mVolume = 1.0f;
        mListener = listener;
    }

    public void release() {
        mPlayer.release();
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
        if (videoRenderer != null) {
            if (mSurface == null) {
                // This is likely in response to a cleanup. Ensure the old surface is released
                // before continuing execution, so we don't perform destructive operations on the
                // old surface while it is in use.
                mPlayer.blockingSendMessage(videoRenderer,
                        MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, mSurface);
            } else {
                mPlayer.sendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, mSurface);
            }
        }

        Log.d(DoordarshanService.TAG, "setSurface: " + mSurface);
    }

    public void setVolume(float volume) {
        mVolume = volume;
        if (audioRenderer != null) {
            mPlayer.sendMessage(audioRenderer,
                    MediaCodecAudioTrackRenderer.MSG_SET_VOLUME, mVolume);
        }
    }

    public void tuneTo(String url) {
        if (mTuneTask != null) {
            mTuneTask.cancel(false);
        }
        mTuneTask = new TuneTask();
        mTuneTask.execute(url);
    }


    @Override
    public void onDroppedFrames(int i, long l) {

    }

    @Override
    public void onVideoSizeChanged(int i, int i2, float f) {
        mListener.onVideoSizeChanged(i, i2, f);
    }


    @Override
    public void onDrawnToSurface(Surface surface) {

    }

    @Override
    public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {

    }

    @Override
    public void onCryptoError(MediaCodec.CryptoException e) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_IDLE:
                break;
            case ExoPlayer.STATE_PREPARING:
                mPreparing = true;
                //mListener.onClosedCaptionChanged();
                break;
            case ExoPlayer.STATE_BUFFERING:
                if (!mPreparing) {
                    mListener.onStateChanged(playWhenReady, playbackState);
                }
                break;
            case ExoPlayer.STATE_READY:
                mPreparing = false;
                mListener.onStateChanged(playWhenReady, playbackState);
                break;
            case ExoPlayer.STATE_ENDED:
                break;
            default:
                Log.e(DoordarshanService.TAG, "Changed to unknown state: " + playbackState);
        }
    }


    @Override
    public void onPlayWhenReadyCommitted() {

    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {

    }

    private class TuneTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
//            String url = "http://commondatastorage.googleapis.com/android-tv/Sample%20videos/April%20Fool's%202013/Introducing%20Google%20Fiber%20to%20the%20Pole.mp4";
            String url = "http://mvads.cdn.espn.com/now/stitched/mp4/4d993388-8b49-4f34-9e48-87906e690281/00000000-0000-0000-0000-000000000000/3a41c6e4-93a3-4108-8995-64ffca7b9106/75b4c038-4889-4e43-98cb-ac72bf364085/0/0/203/366599096/content.mp4";
            return url;
        }

        @Override
        protected void onPreExecute() {
            mPlayer.stop();
            mPlayer.seekTo(0);

        }

        @Override
        protected void onPostExecute(String url) {

            Log.d(DoordarshanService.TAG, "onPostExecute: " + mSurface);
            SampleSource sampleSource =
                    new FrameworkSampleSource(mContext, Uri.parse(url), /* headers */ null, RENDERER_COUNT);

            // Build the track renderers
            videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);
            mPlayer.sendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, mSurface);
            mPlayer.setRendererEnabled(RENDERER_TYPE_VIDEO, true);
            mPlayer.setRendererEnabled(RENDERER_TYPE_AUDIO, true);
            mPlayer.setPlayWhenReady(true);
            mPlayer.prepare(videoRenderer, audioRenderer);

        }
    }

}
