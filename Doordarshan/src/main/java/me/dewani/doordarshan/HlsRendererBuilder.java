package me.dewani.doordarshan;


import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.hls.HlsChunkSource;
import com.google.android.exoplayer.hls.HlsPlaylist;
import com.google.android.exoplayer.hls.HlsPlaylistParser;
import com.google.android.exoplayer.hls.HlsSampleSource;
import com.google.android.exoplayer.metadata.ClosedCaption;
import com.google.android.exoplayer.metadata.Eia608Parser;
import com.google.android.exoplayer.metadata.Id3Parser;
import com.google.android.exoplayer.metadata.MetadataTrackRenderer;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.UriDataSource;
import com.google.android.exoplayer.util.ManifestFetcher;
import com.google.android.exoplayer.util.ManifestFetcher.ManifestCallback;
import com.google.android.exoplayer.util.MimeTypes;

import android.media.MediaCodec;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A {@link RendererBuilder} for HLS.
 */
public class HlsRendererBuilder implements TvMediaPlayer.RendererBuilder, ManifestCallback<HlsPlaylist> {

    private final String userAgent;
    private final String url;
    private final String contentId;

    private TvMediaPlayer player;
    private TvMediaPlayer.RendererBuilderCallback callback;

    public HlsRendererBuilder(String userAgent, String url, String contentId) {
        this.userAgent = userAgent;
        this.url = url;
        this.contentId = contentId;
    }

    @Override
    public void buildRenderers(TvMediaPlayer player, TvMediaPlayer.RendererBuilderCallback callback) {
        this.player = player;
        this.callback = callback;
        HlsPlaylistParser parser = new HlsPlaylistParser();
        ManifestFetcher<HlsPlaylist> playlistFetcher =
                new ManifestFetcher<HlsPlaylist>(parser, contentId, url, userAgent);
        playlistFetcher.singleLoad(player.getMainHandler().getLooper(), this);
    }

    @Override
    public void onManifestError(String contentId, IOException e) {
        callback.onRenderersError(e);
    }

    @Override
    public void onManifest(String contentId, HlsPlaylist manifest) {
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(player.getMainHandler(), player);

        DataSource dataSource = new UriDataSource(userAgent, bandwidthMeter);
        boolean adaptiveDecoder = MediaCodecUtil.getDecoderInfo(MimeTypes.VIDEO_H264, false).adaptive;
        HlsChunkSource chunkSource = new HlsChunkSource(dataSource, url, manifest, bandwidthMeter, null,
                adaptiveDecoder ? HlsChunkSource.ADAPTIVE_MODE_SPLICE : HlsChunkSource.ADAPTIVE_MODE_NONE);
        Log.d(TvMediaPlayer.TAG, "adaptive? " + adaptiveDecoder);
        HlsSampleSource sampleSource = new HlsSampleSource(chunkSource, true, 3);
        MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource,
                MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);

        MetadataTrackRenderer<Map<String, Object>> id3Renderer =
                new MetadataTrackRenderer<Map<String, Object>>(sampleSource, new Id3Parser(),
                        player.getId3MetadataRenderer(), player.getMainHandler().getLooper());

        MetadataTrackRenderer<List<ClosedCaption>> closedCaptionRenderer =
                new MetadataTrackRenderer<List<ClosedCaption>>(sampleSource, new Eia608Parser(),
                        player.getClosedCaptionMetadataRenderer(), player.getMainHandler().getLooper());

        TrackRenderer[] renderers = new TrackRenderer[TvMediaPlayer.RENDERER_COUNT];
        renderers[TvMediaPlayer.TYPE_VIDEO] = videoRenderer;
        renderers[TvMediaPlayer.TYPE_AUDIO] = audioRenderer;
        renderers[TvMediaPlayer.TYPE_TIMED_METADATA] = id3Renderer;
        renderers[TvMediaPlayer.TYPE_TEXT] = closedCaptionRenderer;
        callback.onRenderers(null, null, renderers);
    }

}