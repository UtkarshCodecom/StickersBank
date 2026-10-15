package com.stickers.bank.data.model

import kotlinx.serialization.Serializable

/**
 * A user-created bank: a named collection of stickers the user curates and can
 * later export as a WhatsApp/Telegram pack.
 */
@Serializable
data class Bank(
    val id: String,
    val name: String,
    val createdAt: Long = 0L,
    val stickers: List<Sticker> = emptyList(),
)
