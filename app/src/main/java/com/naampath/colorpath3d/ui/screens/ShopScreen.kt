package com.naampath.colorpath3d.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.billing.ShopProduct
import com.naampath.colorpath3d.progress.CampaignState
import com.naampath.colorpath3d.theme.Themes
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.theme.Amber
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Mist

@Composable
fun ShopScreen(
    state: CampaignState,
    billingReady: Boolean,
    message: String?,
    onBuy: (ShopProduct) -> Unit,
    onRestore: () -> Unit,
    onTheme: (String, Int, Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SHOP", color = Cyan)
        Text("Coins ${state.wallet.coins}", color = Amber)
        if (!billingReady) Text(message ?: "Billing is unavailable. Theme purchases still use coins.", color = Mist)
        ShopProduct.entries.forEach { product ->
            val owned = (product.removesAds && state.wallet.adsRemoved) || (product.premium && state.wallet.premium)
            NeonButton(if (owned) "${product.title} OWNED" else product.title) {
                if (!owned) onBuy(product)
            }
        }
        NeonButton("RESTORE PURCHASES", onClick = onRestore)
        Text("THEMES", color = Cyan)
        Themes.all.forEach { theme ->
            val owned = theme.id in state.ownedThemes || (theme.premiumOnly && state.wallet.premium) || theme.coinPrice == 0
            NeonButton(if (owned) "USE ${theme.displayName}" else "${theme.displayName}  ${theme.coinPrice} coins") {
                onTheme(theme.id, theme.coinPrice, owned || theme.coinPrice == 0)
            }
        }
        NeonButton("BACK", onClick = onBack)
    }
}
