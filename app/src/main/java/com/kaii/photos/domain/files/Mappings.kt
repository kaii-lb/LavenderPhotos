package com.kaii.photos.domain.files

import com.kaii.photos.helpers.grid_management.SelectionManager

fun List<SelectionManager.SelectedItem>.toFileOperationMetadataItems() = this.map { it.toFileOperationMetadata() }