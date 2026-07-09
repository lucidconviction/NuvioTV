package com.robbdeeze.nuviotv.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import com.robbdeeze.nuviotv.DrawerItem

internal fun <T> syncRouteMap(
    existing: MutableMap<String, T>,
    routes: List<String>,
    create: () -> T
) {
    val validRoutes = routes.toSet()
    existing.keys.retainAll(validRoutes)
    routes.forEach { route ->
        existing.getOrPut(route, create)
    }
}

@Composable
internal fun rememberDrawerItemFocusRequesters(
    drawerItems: List<DrawerItem>
): Map<String, FocusRequester> {
    val focusRequesters = remember { linkedMapOf<String, FocusRequester>() }
    syncRouteMap(
        existing = focusRequesters,
        routes = drawerItems.map(DrawerItem::route),
        create = ::FocusRequester
    )
    return focusRequesters
}
