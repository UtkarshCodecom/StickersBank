package com.stickers.bank.ui.theme

import androidx.compose.ui.graphics.Color

// Stickers Bank palette — these are ROLE slots, not literal colours: every screen paints from
// these instead of hardcoding its own values. They now all delegate to [Neo.current], the
// neumorphic theme system (see Neu.kt) — picking a different palette in Settings re-themes the
// whole app instantly, with zero changes needed in the screens that reference these names.
//
// Neumorphism deliberately flattens the old three-tier surface hierarchy (Background/Surface/
// SurfaceElevated) into one shared surface colour — depth now comes from `neuRaised` embossed
// shadows, not from different fill shades.

val Gold: Color get() = Neo.primary        // primary accent
val GoldDark: Color get() = Neo.primary    // pressed / deep accent — same hue, neumorphism reads depth from shadow not shade
val GoldSoft: Color get() = Neo.primary    // highlight tint

// Text/icons drawn ON a Gold fill.
val OnGold: Color get() = Neo.onPrimary

val AppBackground: Color get() = Neo.bg           // page
val AppSurface: Color get() = Neo.surface         // panels / bars / sheets
val AppSurfaceVariant: Color get() = Neo.surface  // chips, insets, tool buttons
val AppSurfaceElevated: Color get() = Neo.surface // cards, elevated blocks

val AppOnSurface: Color get() = Neo.text          // primary text / icons
val AppOnSurfaceMuted: Color get() = Neo.muted    // secondary text
val AppOutline: Color get() = Neo.dark            // hairlines, borders

val Danger: Color get() = Neo.danger
val Success: Color get() = Neo.success

// A translucent accent fill used behind selected tools.
val AccentTint: Color get() = Neo.primary.copy(alpha = 0.13f)
