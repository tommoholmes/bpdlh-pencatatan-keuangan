package com.bpdlh.pencatatankeuangan.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class PushRecord(
    val id: String,
    val name: String,
    val price: Long,
    val photoId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracyM: Double?,
    val deviceModel: String?,
    val osVersion: String?,
    val appVersion: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

@Serializable
data class PushBody(val deviceId: String, val records: List<PushRecord>)

@Serializable
data class PushResult(val id: String? = null, val status: String, val reason: String? = null)

@Serializable
data class PushResponse(val results: List<PushResult>, val serverSeq: Long)

@Serializable
data class PulledRecord(
    val id: String,
    val name: String,
    val price: Long,
    val photoId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyM: Double? = null,
    val deviceId: String? = null,
    val deviceModel: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val seq: Long,
)

@Serializable
data class PullResponse(val records: List<PulledRecord>, val nextSeq: Long, val hasMore: Boolean)

@Serializable
data class BootstrapResponse(val logId: String)
