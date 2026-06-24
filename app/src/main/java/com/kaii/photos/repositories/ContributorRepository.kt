package com.kaii.photos.repositories

import com.kaii.photos.data.datasources.ContributorDataSource
import com.kaii.photos.domain.about.ContributorItem

class ContributorRepository(
    private val dataSource: ContributorDataSource
) {
    suspend fun getNewsData(): List<ContributorItem> {
        return dataSource.getContributorList()
    }
}