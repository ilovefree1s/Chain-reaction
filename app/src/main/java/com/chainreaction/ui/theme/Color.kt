package com.chainreaction.ui.theme

import androidx.compose.ui.graphics.Color
import com.chainreaction.data.CardKind

// Ground and surfaces — dark so it isn't blinding at dusk.
val Pine = Color(0xFF0C1A14)
val Panel = Color(0xFF132A21)
val PanelRaised = Color(0xFF1B3B2E)
val Sage = Color(0xFF8AA79A)
val OffWhite = Color(0xFFF2F5F1)

// Function colours — the main visual system.
val Attack = Color(0xFFFF5A4D)
val SelfCard = Color(0xFFFFD23F)
val Dual = Color(0xFFFF57C1)
val React = Color(0xFF4DD9E8)
val Group = Color(0xFFA78BFA)

// Menu-only accents, taken from the title-screen artwork. The in-round screens keep
// the pine/sage/gold system — this is splash, not the working UI.
val MenuOrange = Color(0xFFF7941E)
val MenuBlue = Color(0xFF1B62B5)

val CardKind.color: Color
    get() = when (this) {
        CardKind.ATTACK -> Attack
        CardKind.SELF -> SelfCard
        CardKind.DUAL -> Dual
        CardKind.REACT -> React
        CardKind.GROUP -> Group
    }
