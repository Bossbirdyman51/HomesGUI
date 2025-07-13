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
import net.wesjd.anvilgui.AnvilGUI;
import net.william278.huskhomes.gui.HuskHomesGui;
import net.william278.huskhomes.position.Home;
import net.william278.huskhomes.teleport.TeleportationException;
import net.william278.huskhomes.user.OnlineUser;
import net.william278.huskhomes.user.User;
import net.william278.huskhomes.util.ValidationException;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

import static net.william278.huskhomes.gui.config.Locales.textWrap;

public class NewListMenu extends Menu {

    private final List<Home> homes;
    private final User owner;
    private MenuMode mode = MenuMode.TELEPORT;
    private final boolean addHomeButton;

    private NewListMenu(@NotNull HuskHomesGui plugin, @NotNull List<Home> homes, @NotNull User owner, @NotNull String title, boolean addHomeButton) {
        super(plugin, title, getMenuLayout());
        this.homes = homes;
        this.owner = owner;
        this.addHomeButton = addHomeButton;
    }

    @NotNull
    public static NewListMenu create(@NotNull HuskHomesGui plugin, @NotNull List<Home> homes, @NotNull User owner) {
        return new NewListMenu(plugin, homes, owner, plugin.getLocales().getLocale("homes_menu_title", owner.getUsername()), true);
    }

    @NotNull
    public static NewListMenu createPublic(@NotNull HuskHomesGui plugin, @NotNull List<Home> homes, @NotNull User viewer) {
        return new NewListMenu(plugin, homes, viewer, plugin.getLocales().getLocale("public_homes_menu_title"), false);
    }

    @NotNull
    private static String[] getMenuLayout() {
        return new String[]{
                "hhhhhhhhh",
                "hhhhhhhhh",
                "<famsf>"
        };
    }

    @Override
    protected Consumer<InventoryGui> buildMenu() {
        return (menu) -> {
            menu.setFiller(new ItemStack(plugin.getSettings().getHomesFillerItem()));

            final GuiElementGroup homeGroup = new GuiElementGroup('h');
            homes.forEach(home -> homeGroup.addElement(createHomeButton(home)));
            menu.addElement(homeGroup);

            menu.addElement(new GuiPageElement('<', new ItemStack(plugin.getSettings().getPaginatePreviousPage()), GuiPageElement.PageAction.PREVIOUS, plugin.getLocales().getLocale("pagination_previous_page")));
            menu.addElement(new GuiPageElement('>', new ItemStack(plugin.getSettings().getPaginateNextPage()), GuiPageElement.PageAction.NEXT, plugin.getLocales().getLocale("pagination_next_page")));

            menu.addElement(createTeleportButton());
            menu.addElement(createDeleteButton());
            if (addHomeButton) {
                menu.addElement(createAddButton());
            }
        };
    }

    private DynamicGuiElement createHomeButton(@NotNull Home home) {
        return new DynamicGuiElement('h', (viewer) -> new StaticGuiElement('h',
                new ItemStack(getPositionMaterial(home).orElse(plugin.getSettings().getDefaultIcon())),
                click -> {
                    if (click.getWhoClicked() instanceof Player player) {
                        final OnlineUser user = api.adaptUser(player);
                        switch (mode) {
                            case TELEPORT -> {
                                this.close(user);
                                try {
                                    api.teleportBuilder(user).target(home).toTimedTeleport().execute();
                                } catch (TeleportationException ignored) {
                                }
                            }
                            case DELETE -> showDeleteConfirmation(player, home);
                        }
                    }
                    return true;
                },
                plugin.getLocales().getLocale("item_name", home.getName()),
                plugin.getLocales().getLocale(
                        "item_description",
                        !home.getMeta().getDescription().isBlank() ?
                                textWrap(plugin, home.getMeta().getDescription()) :
                                plugin.getLocales().getLocale("item_description_blank")
                ),
                plugin.getLocales().getLocale("home_mode_lore_" + mode.name().toLowerCase())
        ));
    }

    private void showDeleteConfirmation(@NotNull Player player, @NotNull Home home) {
        final OnlineUser user = api.adaptUser(player);
        final String[] layout = {"xxxxxxxxx", "xcyxxxxnx", "xxxxxxxxx"};
        final InventoryGui gui = new InventoryGui(plugin, plugin.getLocales().getLocale("delete_home_title", home.getName()), layout);
        gui.setFiller(new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        gui.setCloseAction(close -> false);
        gui.addElement(new StaticGuiElement('c', new ItemStack(Material.LIME_WOOL), click -> {
            try {
                plugin.getLogger().info("Attempting to delete home: " + home.getName());
                api.deleteHome(home);
                plugin.getLogger().info("Home deletion successful");
                api.getUserHomes(owner).thenAccept(updatedHomes -> {
                    plugin.getLogger().info("Got " + updatedHomes.size() + " homes after deletion");
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        NewListMenu.create(plugin, updatedHomes, owner).show(user);
                    });
                }).exceptionally(e -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to refresh homes list after deletion", e);
                    return null;
                });
            } catch (ValidationException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete home", e);
            }
            return true;
        }, plugin.getLocales().getLocale("delete_confirm_button")));
        gui.addElement(new StaticGuiElement('n', new ItemStack(Material.RED_WOOL), click -> {
            this.show(user);
            return true;
        }, plugin.getLocales().getLocale("delete_cancel_button")));
        gui.show(player);
    }

    private DynamicGuiElement createTeleportButton() {
        return new DynamicGuiElement('m', (viewer) -> {
            final ItemStack icon = new ItemStack(Material.ENDER_PEARL);
            final ItemMeta meta = icon.getItemMeta();
            if (mode == MenuMode.TELEPORT) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            icon.setItemMeta(meta);
            return new StaticGuiElement('m', icon, click -> {
                this.mode = MenuMode.TELEPORT;
                click.getGui().draw();
                return true;
            }, plugin.getLocales().getLocale("teleport_mode_button"));
        });
    }

    private DynamicGuiElement createDeleteButton() {
        return new DynamicGuiElement('s', (viewer) -> {
            final ItemStack icon = new ItemStack(Material.BARRIER);
            final ItemMeta meta = icon.getItemMeta();
            if (mode == MenuMode.DELETE) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            icon.setItemMeta(meta);
            return new StaticGuiElement('s', icon, click -> {
                this.mode = MenuMode.DELETE;
                click.getGui().draw();
                return true;
            }, plugin.getLocales().getLocale("delete_mode_button"));
        });
    }

    private StaticGuiElement createAddButton() {
        return new StaticGuiElement('a',
                new ItemStack(Material.OAK_SIGN),
                click -> {
                    if (click.getWhoClicked() instanceof Player player) {
                        final OnlineUser user = api.adaptUser(player);
                        this.close(user);
                        new AnvilGUI.Builder()
                                .title(plugin.getLocales().getLocale("add_home_title"))
                                .text(plugin.getLocales().getLocale("add_home_default_name"))
                                .itemLeft(new ItemStack(Material.OAK_SIGN))
                                .plugin(plugin)
                                .onClose(p -> this.show(user))
                                .onClick((slot, state) -> {
                                    if (slot == AnvilGUI.Slot.OUTPUT) {
                                        try {
                                            plugin.getLogger().info("Attempting to create home: " + state.getText());
                                            api.createHome(owner, state.getText(), user.getPosition());
                                            plugin.getLogger().info("Home creation successful");
                                            return Arrays.asList(
                                                AnvilGUI.ResponseAction.close(),
                                                AnvilGUI.ResponseAction.run(() -> {
                                                    plugin.getLogger().info("Fetching updated homes list");
                                                    api.getUserHomes(owner).thenAccept(updatedHomes -> {
                                                        plugin.getLogger().info("Got " + updatedHomes.size() + " homes");
                                                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                                                            NewListMenu.create(plugin, updatedHomes, owner).show(user);
                                                        });
                                                    }).exceptionally(e -> {
                                                        plugin.getLogger().log(Level.SEVERE, "Failed to refresh homes list", e);
                                                        return null;
                                                    });
                                                })
                                            );
                                        } catch (ValidationException e) {
                                            return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText(plugin.getLocales().getLocale("error_invalid_name")));
                                        }
                                    }
                                    return Collections.emptyList();
                                })
                                .open(player);
                    }
                    return true;
                },
                plugin.getLocales().getLocale("add_home_button")
        );
    }

    private enum MenuMode {
        TELEPORT,
        DELETE
    }
}