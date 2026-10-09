package app.vazie.vpn.core.designsystem.site

import kotlin.test.Test
import kotlin.test.assertEquals

class VazieSiteTest {

    @Test
    fun `VPN Plus opens on the site with the plan's code`() {
        assertEquals("https://vazie.app/vpn/plus?plan=VPN_PLUS_YEARLY", VazieSite("https://vazie.app").vpnPlus("VPN_PLUS_YEARLY"))
    }

    @Test
    fun `a local site keeps its port`() {
        assertEquals("http://localhost:3010/vpn/plus?plan=VPN_PLUS_MONTHLY", VazieSite("http://localhost:3010").vpnPlus("VPN_PLUS_MONTHLY"))
    }

    @Test
    fun `a code is never put into the address raw`() {
        assertEquals("https://vazie.app/vpn/plus?plan=A%26b%3D1+2", VazieSite("https://vazie.app").vpnPlus("A&b=1 2"))
    }

    @Test
    fun `legal pages stay on the site`() {
        assertEquals("https://vazie.app/legal/offer", VazieSite("https://vazie.app").offer)
    }
}
