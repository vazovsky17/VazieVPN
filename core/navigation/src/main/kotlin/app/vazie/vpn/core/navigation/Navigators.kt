package app.vazie.vpn.core.navigation

/* Transitions from one feature module into another. */

interface HomeNavigator {
    fun toAddConfig()

    /** The gear on Home; there is no bottom bar since the "Маршрут" redesign. */
    fun toSettings()
}

interface OnboardingNavigator {
    /** Onboarding is finished, not paused: it leaves the back stack for good. */
    fun toMain()

    fun toAddConfig()

    /** "Get VPN Plus" on the last page: onboarding is finished, and the purchase starts — sign-in first if
     * nobody is signed in, then the payment. [planCode] is the plan chosen, a code from the backend's catalogue. */
    fun toPlus(planCode: String)
}
