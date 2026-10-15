package com.stickers.bank.export

import com.stickers.bank.data.model.Sticker

/**
 * The "Stickers Bank" pack in WhatsApp that stickers sent fewer than 3 at a time build up into.
 * They collect in the app's "Stickers Bank" bank (BankRepository.addToSinglesPack) — one today,
 * two tomorrow — and only once there are [PackBuilder.MIN_STICKERS] of a kind does the pack go
 * to WhatsApp; after that each new one is added to the same pack. Nothing is padded with
 * filler stickers.
 *
 * WhatsApp's rules shape the rest:
 *  - a pack is all-static or all-animated, so the two kinds collect in separate packs;
 *  - max [PackBuilder.MAX_STICKERS] per pack, so the 31st starts "Stickers Bank 2".
 */
object SinglesPack {
    const val NAME = "Stickers Bank"

    /** Identifier, display name and members of the pack [sticker] belongs in, given every
     * sticker in the bank ([all], oldest first, already including [sticker]). */
    data class Target(val identifier: String, val name: String, val stickers: List<Sticker>, val animated: Boolean)

    fun targetFor(all: List<Sticker>, sticker: Sticker): Target {
        val animated = sticker.isAnimatedSticker
        val sameKind = all.filter { it.isAnimatedSticker == animated }
        val page = (sameKind.indexOfFirst { it.id == sticker.id }.coerceAtLeast(0)) / PackBuilder.MAX_STICKERS
        val members = sameKind.chunked(PackBuilder.MAX_STICKERS).getOrElse(page) { listOf(sticker) }
        // A fixed identifier, so the pack GROWS in WhatsApp as stickers are added. Not
        // "stickers_bank": an exported bank called "Stickers Bank" must stay a separate pack.
        // Not the old "sb_singles" either: that filler-padded pack may still be in people's
        // WhatsApp, and reusing its id made the 3rd sticker look "already added" — so WhatsApp's
        // add screen never opened for the new pack.
        val identifier = "sb_bank" + (if (animated) "_anim" else "") + (if (page > 0) "_${page + 1}" else "")
        val name = NAME + (if (animated) " Animated" else "") + (if (page > 0) " ${page + 1}" else "")
        return Target(identifier, name, members, animated)
    }
}
