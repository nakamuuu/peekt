package net.divlight.peekt.core

/**
 * When stored recordings are deleted automatically. Count limits still apply separately.
 */
enum class ClearingStrategy {
    /**
     * Deletes every stored transaction when Peekt is created.
     */
    OnLaunch,

    /**
     * Does not delete on create. [PeektRecorder.clear] and [PeektConfig.maxTransactions] still apply.
     */
    Never,
}
