package com.kaii.photos.domain.news

sealed interface News {
    val id: Int

    data class Section(
        val version: String,
        val date: String,
        val status: Status,
        override val id: Int
    ) : News {
        enum class Status {
            Latest,
            Broken,
            None
        }
    }

    data class Category(
        val category: Type,
        override val id: Int
    ) : News {
        enum class Type {
            Feature,
            Fix,
            Improvement
        }
    }

    data class Item(
        val title: String,
        val issueNumber: Int?,
        override val id: Int
    ) : News

    data class Note(
        val info: String,
        val urgency: Urgency,
        override val id: Int
    ) : News {
        enum class Urgency {
            Normal,
            Critical
        }
    }
}