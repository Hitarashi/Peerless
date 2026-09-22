package org.shilpo.peerless.theme

/**
 * Returns true when the platform asks apps to reduce non-essential motion.
 *
 * - Android: `Settings.Global.ANIMATOR_DURATION_SCALE == 0`
 * - iOS: `UIAccessibility.isReduceMotionEnabled`
 * - Desktop: always false (no standard API)
 *
 * Used by the liquid glass system to auto-disable the scroll-reactive
 * refraction pulse while keeping the static glass material intact.
 */
internal expect fun isReducedMotionEnabled(): Boolean
