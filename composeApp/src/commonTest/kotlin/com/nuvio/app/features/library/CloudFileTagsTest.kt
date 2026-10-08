package com.nuvio.app.features.library

import kotlin.test.Test
import kotlin.test.assertEquals

class CloudFileTagsTest {

    @Test
    fun reads_episode_resolution_hdr_and_codec() {
        assertEquals(
            listOf("S01E02", "4K", "DV", "HEVC"),
            cloudFileTags("Show.Name.S01E02.2160p.WEB-DL.DV.H.265-GROUP.mkv"),
        )
    }

    @Test
    fun pads_short_episode_numbers() {
        assertEquals(listOf("S03E07", "1080p", "H.264"), cloudFileTags("show s3e7 1080p x264.mp4"))
    }

    @Test
    fun prefers_hdr10_plus_over_plain_hdr() {
        assertEquals(listOf("4K", "HDR10+", "HEVC"), cloudFileTags("Movie.2023.UHD.HDR10+.HEVC.mkv"))
    }

    @Test
    fun plain_names_have_no_tags() {
        assertEquals(emptyList(), cloudFileTags("Home video.mov"))
    }
}

class CloudReleaseTitleTest {

    @Test
    fun keeps_title_and_year_from_dotted_release() {
        assertEquals("Doing Life (2026)", cloudReleaseTitle("Doing.Life.2026.1080p.NF.WEB-DL.DDP5.1.mkv"))
    }

    @Test
    fun keeps_title_and_year_from_spaced_release() {
        assertEquals("Unabomber (2026)", cloudReleaseTitle("Unabomber 2026 2160p NF WEB-DL DDP"))
    }

    @Test
    fun cuts_at_episode_marker_without_year() {
        assertEquals("Show Name", cloudReleaseTitle("Show.Name.S01E02.1080p.WEB.mkv"))
    }

    @Test
    fun drops_bracketed_site_prefix() {
        assertEquals("奇异博士 (2016)", cloudReleaseTitle("【高清影视之家发布 www.SSDSSE.com】奇异博士.2016.2160p.UHD.mkv"))
    }

    @Test
    fun drops_group_tag_and_site_prefixes() {
        assertEquals("Dune Part Two (2024)", cloudReleaseTitle("[YTS.MX] Dune.Part.Two.2024.1080p.mp4"))
        assertEquals("Dune Part Two (2024)", cloudReleaseTitle("www.1TamilMV.com - Dune Part Two (2024) 1080p.mkv"))
    }

    @Test
    fun keeps_names_without_release_markers() {
        assertEquals("Home video", cloudReleaseTitle("Home video.mov"))
    }
}
