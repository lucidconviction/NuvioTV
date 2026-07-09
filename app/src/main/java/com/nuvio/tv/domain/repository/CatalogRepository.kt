package com.robbdeeze.nuviotv.domain.repository

import com.robbdeeze.nuviotv.core.network.NetworkResult
import com.robbdeeze.nuviotv.domain.model.CatalogRow
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    fun getCatalog(
        addonBaseUrl: String,
        addonId: String,
        addonName: String,
        catalogId: String,
        catalogName: String,
        type: String,
        skip: Int = 0,
        skipStep: Int = 100,
        extraArgs: Map<String, String> = emptyMap(),
        supportsSkip: Boolean = false
    ): Flow<NetworkResult<CatalogRow>>
}
