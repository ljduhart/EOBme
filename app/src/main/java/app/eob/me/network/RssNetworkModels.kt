package app.eob.me.network

import com.google.gson.annotations.SerializedName

data class RssResponse(
    val status: String,
    val feed: FeedInfo?,
    val items: List<RssItem>?
)

data class FeedInfo(
    val title: String?
)

data class RssItem(
    val title: String?,
    val pubDate: String?,
    val link: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("description") val description: String?
)
