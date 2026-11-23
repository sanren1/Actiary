package com.actiary.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class WorkoutTrackParserTest {
    @Test
    fun parsesHuaweiStyleGpxWithDurationDistanceElevationAndHeartRate() {
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Huawei Health" xmlns="http://www.topografix.com/GPX/1/1"
                 xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1">
              <trk>
                <name>户外跑步 2026-11-20</name>
                <type>running</type>
                <trkseg>
                  <trkpt lat="31.23000" lon="121.47000">
                    <ele>10.0</ele>
                    <time>2026-11-20T00:00:00Z</time>
                    <extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>140</gpxtpx:hr><gpxtpx:cad>170</gpxtpx:cad></gpxtpx:TrackPointExtension></extensions>
                  </trkpt>
                  <trkpt lat="31.23900" lon="121.47000">
                    <ele>25.5</ele>
                    <time>2026-11-20T00:05:30Z</time>
                    <extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>150</gpxtpx:hr><gpxtpx:cad>174</gpxtpx:cad></gpxtpx:TrackPointExtension></extensions>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val parsed = WorkoutTrackParser.parse(gpx.byteInputStream(), ZoneOffset.UTC)
        assertNotNull(parsed)
        assertEquals(LocalDate.of(2026, 11, 20), parsed!!.date)
        assertEquals("running", parsed.typeKey)
        assertEquals(6, parsed.durationMinutes)
        assertEquals(330, parsed.metrics?.durationSeconds)
        assertTrue(parsed.distanceMeters!! in 980..1020)
        assertEquals(145, parsed.avgHeartRate)
        assertEquals(172, parsed.metrics?.cadenceSpm)
        assertEquals(1550, parsed.metrics?.elevationGainCm)
    }

    @Test
    fun parsesTcxWithLapsCaloriesAndHeartRate() {
        val tcx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <TrainingCenterDatabase xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2">
              <Activities>
                <Activity Sport="Biking">
                  <Id>2026-11-21T08:00:00Z</Id>
                  <Lap StartTime="2026-11-21T08:00:00Z">
                    <TotalTimeSeconds>1800</TotalTimeSeconds>
                    <DistanceMeters>12500.4</DistanceMeters>
                    <Calories>420</Calories>
                    <AverageHeartRateBpm><Value>138</Value></AverageHeartRateBpm>
                    <Track>
                      <Trackpoint><AltitudeMeters>50.0</AltitudeMeters></Trackpoint>
                      <Trackpoint><AltitudeMeters>112.0</AltitudeMeters></Trackpoint>
                    </Track>
                  </Lap>
                </Activity>
              </Activities>
            </TrainingCenterDatabase>
        """.trimIndent()

        val parsed = WorkoutTrackParser.parse(tcx.byteInputStream(), ZoneOffset.UTC)
        assertNotNull(parsed)
        assertEquals(LocalDate.of(2026, 11, 21), parsed!!.date)
        assertEquals("cycling", parsed.typeKey)
        assertEquals(30, parsed.durationMinutes)
        assertEquals(1800, parsed.metrics?.durationSeconds)
        assertEquals(12500, parsed.distanceMeters)
        assertEquals(420, parsed.calories)
        assertEquals(138, parsed.avgHeartRate)
        assertEquals(6200, parsed.metrics?.elevationGainCm)
    }

    @Test
    fun malformedXmlReturnsNull() {
        assertNull(WorkoutTrackParser.parse("not xml".byteInputStream(), ZoneOffset.UTC))
    }
}
