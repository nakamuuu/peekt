package net.divlight.peekt.core

/**
 * When stored recordings are deleted automatically. Count limits still apply separately.
 */
enum class ClearingStrategy {
    /**
     * Deletes stored transactions that started before this process, when Peekt is created.
     *
     * Rows recorded in the current process are kept, so creation cannot race with the first
     * intercepted request.
     */
    OnLaunch,

    /**
     * Does not delete on create. [PeektRecorder.clear] and [PeektConfig.maxTransactions] still apply.
     */
    Never,
}
