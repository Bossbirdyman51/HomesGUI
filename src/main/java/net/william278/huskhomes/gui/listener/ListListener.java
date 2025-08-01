/*
 * This file is part of HuskHomesGUI, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskhomes.gui.listener;

import net.william278.huskhomes.event.HomeListEvent;
import net.william278.huskhomes.event.WarpListEvent;
import net.william278.huskhomes.gui.HuskHomesGui;
import net.william278.huskhomes.gui.menu.NewListMenu;
import net.william278.huskhomes.gui.menu.WarpListMenu;
import net.william278.huskhomes.position.Home;
import net.william278.huskhomes.user.OnlineUser;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class ListListener implements Listener {
    private final HuskHomesGui plugin;

    public ListListener(@NotNull HuskHomesGui plugin) {
        this.plugin = plugin;
    }

    // When the home or public home list is viewed
    @EventHandler
    public void onHomeListView(@NotNull HomeListEvent event) {
        if (!(event.getListViewer() instanceof OnlineUser onlineUser)) {
            return;
        }

        plugin.getLogger().info("HomeListEvent intercepted, cancelling and showing GUI.");
        event.setCancelled(true);
        if (event.getIsPublicHomeList()) {
            NewListMenu.createPublic(plugin, event.getHomes(), onlineUser).show(onlineUser);
        } else {
            NewListMenu.create(plugin, event.getHomes(),
                    event.getHomes().stream()
                            .findFirst()
                            .map(Home::getOwner)
                            .orElse(onlineUser)).show(onlineUser);
        }
    }

    // When the warp list is viewed
    @EventHandler
    public void onWarpListView(@NotNull WarpListEvent event) {
        if (!(event.getListViewer() instanceof OnlineUser onlineUser)) {
            return;
        }

        event.setCancelled(true);
        WarpListMenu.create(plugin, event.getWarps()).show(onlineUser);
    }

}