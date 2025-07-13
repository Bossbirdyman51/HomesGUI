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

package net.william278.huskhomes.gui.menu;

import de.themoep.inventorygui.*;
import net.william278.huskhomes.gui.HuskHomesGui;
import net.william278.huskhomes.position.Warp;
import net.william278.huskhomes.teleport.TeleportationException;
import net.william278.huskhomes.user.OnlineUser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

import static net.william278.huskhomes.gui.config.Locales.textWrap;

public class WarpListMenu extends Menu {

    private final List<Warp> warps;

    private WarpListMenu(@NotNull HuskHomesGui plugin, @NotNull List<Warp> warps) {
        super(plugin, plugin.getLocales().getLocale("warps_menu_title"), getMenuLayout());
        this.warps = warps;
    }

    @NotNull
    public static WarpListMenu create(@NotNull HuskHomesGui plugin, @NotNull List<Warp> warps) {
        return new WarpListMenu(plugin, warps);
    }

    @NotNull
    private static String[] getMenuLayout() {
        return new String[]{
                "wwwwwwwww",
                "wwwwwwwww",
                "<#######>"
        };
    }

    @Override
    protected Consumer<InventoryGui> buildMenu() {
        return (menu) -> {
            menu.setFiller(new ItemStack(plugin.getSettings().getWarpsFillerItem()));

            final GuiElementGroup warpGroup = new GuiElementGroup('w');
            warps.forEach(warp -> warpGroup.addElement(createWarpButton(warp)));
            menu.addElement(warpGroup);

            menu.addElement(new GuiPageElement('<', new ItemStack(plugin.getSettings().getPaginatePreviousPage()), GuiPageElement.PageAction.PREVIOUS, plugin.getLocales().getLocale("pagination_previous_page")));
            menu.addElement(new GuiPageElement('>', new ItemStack(plugin.getSettings().getPaginateNextPage()), GuiPageElement.PageAction.NEXT, plugin.getLocales().getLocale("pagination_next_page")));
        };
    }

    private StaticGuiElement createWarpButton(@NotNull Warp warp) {
        return new StaticGuiElement('w',
                new ItemStack(getPositionMaterial(warp).orElse(plugin.getSettings().getDefaultIcon())),
                click -> {
                    if (click.getWhoClicked() instanceof Player player) {
                        final OnlineUser user = api.adaptUser(player);
                        this.close(user);
                        try {
                            api.teleportBuilder(user).target(warp).toTimedTeleport().execute();
                        } catch (TeleportationException ignored) {
                        }
                    }
                    return true;
                },
                plugin.getLocales().getLocale("item_name", warp.getName()),
                plugin.getLocales().getLocale(
                        "item_description",
                        !warp.getMeta().getDescription().isBlank() ?
                                textWrap(plugin, warp.getMeta().getDescription()) :
                                plugin.getLocales().getLocale("item_description_blank")
                )
        );
    }
}