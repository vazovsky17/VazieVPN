package app.vazie.vpn.core.model

/** Which of the three canonical Vazie plans something refers to. */
enum class VaziePlanId(val id: String) {

    FREE("free"),
    CONNECT("connect"),
    CORE("core"),
    ;

    /** Whether buying this is a thing that could ever be done. */
    val isPaid: Boolean get() = this != FREE

    companion object {
        fun fromId(id: String): VaziePlanId? = entries.firstOrNull { it.id == id }
    }
}

/** How long a paid plan is bought for. */
enum class VazieBillingPeriod(val id: String, val months: Int) {

    P1M("p1m", months = 1),
    P3M("p3m", months = 3),
    P6M("p6m", months = 6),
    P12M("p12m", months = 12),
    ;

    /** Whether a saving can be stated for this period. */
    val canSave: Boolean get() = this != P1M

    companion object {
        fun fromId(id: String): VazieBillingPeriod? = entries.firstOrNull { it.id == id }
    }
}
