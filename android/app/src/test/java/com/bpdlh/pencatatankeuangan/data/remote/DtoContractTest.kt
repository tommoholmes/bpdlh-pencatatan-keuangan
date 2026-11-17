package com.bpdlh.pencatatankeuangan.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val sample = PushRecord(
        id = "rec-1",
        name = "Kopi susu",
        price = 18_000,
        photoId = "ph-1",
        latitude = -7.7956,
        longitude = 110.3695,
        accuracyM = 12.5,
        deviceModel = "Pixel 8",
        osVersion = "Android 15",
        appVersion = "1.0.0",
        createdAt = 1_794_800_000_000,
        updatedAt = 1_794_800_000_000,
        deletedAt = null,
    )

    @Test
    fun pushBodyUsesServerFieldNames() {
        val encoded = json.encodeToString(PushBody(deviceId = "dev-1", records = listOf(sample)))
        val fields = listOf(
            "deviceId", "photoId", "accuracyM", "deviceModel", "osVersion", "appVersion",
            "createdAt", "updatedAt", "deletedAt",
        )
        for (field in fields) {
            assertTrue("field $field hilang dari body push", encoded.contains("\"$field\""))
        }
    }

    @Test
    fun pullResponseFromServerParses() {
        val body = """
            {"records":[{"id":"rec-1","name":"Kopi","price":18000,"photoId":null,"latitude":null,
            "longitude":null,"accuracyM":null,"deviceId":"dev-2","deviceModel":"Pixel","osVersion":"Android 15",
            "appVersion":"1.0.0","createdAt":1,"updatedAt":2,"deletedAt":null,"seq":7}],
            "nextSeq":7,"hasMore":false}
        """.trimIndent()

        val parsed = json.decodeFromString<PullResponse>(body)
        assertEquals(7L, parsed.nextSeq)
        assertFalse(parsed.hasMore)
        assertEquals("dev-2", parsed.records.single().deviceId)
        assertEquals(7L, parsed.records.single().seq)
    }

    @Test
    fun rejectedResultCarriesReason() {
        val parsed = json.decodeFromString<PushResponse>(
            """{"results":[{"id":"rec-1","status":"rejected","reason":"invalid_record"}],"serverSeq":3}""",
        )
        assertEquals("invalid_record", parsed.results.single().reason)
        assertEquals(3L, parsed.serverSeq)
    }
}
