package com.example.yol_yolakay.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Blue Premium palette:
 * - Accent: BrandBlue
 * - Neutrals: Slate-like (premium, minimal)
 */

// Brand (primary)
val BrandBlue = Color(0xFF2563EB)       // premium blue (not too neon)
val BrandBlueDark = Color(0xFF7AA2FF)   // used as primary in dark theme

// Neutrals (light)
val BgLight = Color(0xFFF8FAFC)              // slate-50
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F9)  // slate-100

val TextPrimaryLight = Color(0xFF0F172A)     // slate-900 (near-black)
val TextSecondaryLight = Color(0xFF64748B)   // slate-500
val OutlineLight = Color(0xFFE2E8F0)         // slate-200

// Neutrals (dark)
val BgDark = Color(0xFF0B1220)               // deep navy
val SurfaceDark = Color(0xFF0F172A)          // slate-900
val SurfaceVariantDark = Color(0xFF111C33)   // slightly lifted surface

val TextPrimaryDark = Color(0xFFE5E7EB)      // slate-200
val TextSecondaryDark = Color(0xFF94A3B8)    // slate-400
val OutlineDark = Color(0xFF334155)          // slate-700

// Status (optional)
val Success = Color(0xFF16A34A)
val Warning = Color(0xFFF59E0B)
val Danger = Color(0xFFEF4444)