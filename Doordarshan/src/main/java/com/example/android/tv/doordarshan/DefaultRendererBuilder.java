package com.example.android.tv.doordarshan;

import com.google.android.exoplayer.FrameworkSampleSource;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;


import android.content.Context;
import android.media.MediaCodec;
import android.net.Uri;
import android.widget.TextView;

/**
 * A {@link RendererBuilder} for streams that can be read using
 * {@link android.media.MediaExtractor}.
 */
public class DefaultRendererBuilder implements TvMediaPlayer.RendererBuilder {

    private final Context context;
    private final Uri uri;
    private final TextView debugTextView;

    public DefaultRendererBuilder(Context context, Uri uri, TextView debugTextView) {
        this.context = context;
        this.uri = uri;
        this.debugTextView = debugTextView;
    }

    @Override
    public void buildRenderers(TvMediaPlayer player, TvMediaPlayer.RendererBuilderCallback callback) {
        // Build the video and audio renderers.
        FrameworkSampleSource sampleSource = new FrameworkSampleSource(context, uri, null, 2);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);

        // Invoke the callback.
        TrackRenderer[] renderers = new TrackRenderer[TvMediaPlayer.RENDERER_COUNT];
        renderers[TvMediaPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[TvMediaPlayer.TYPE_AUDIO] = audioRenderer;
        callback.onRenderers(null, null, renderers);
    }

}