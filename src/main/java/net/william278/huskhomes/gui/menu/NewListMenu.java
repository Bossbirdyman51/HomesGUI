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
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

import static net.william278.huskhomes.gui.config.Locales.textWrap;

public class NewListMenu extends Menu {

    private final List<Home> homes;
    private final User owner;
    private MenuMode mode = MenuMode.TELEPORT;
    private final SortMode sortMode;
    private final boolean addHomeButton;

    private NewListMenu(@NotNull HuskHomesGui plugin, @NotNull List<Home> homes, @NotNull User owner, @NotNull String title, boolean addHomeButton, @NotNull SortMode sortMode) {
        super(plugin, title, getMenuLayout(plugin));
        this.homes = homes;
        this.owner = owner;
        this.addHomeButton = addHomeButton;
        this.sortMode = sortMode;
    }

    @NotNull
    public static NewListMenu create(@NotNull HuskHomesGui plugin, @NotNull List<Home> homes, @NotNull User owner) {
        return new NewListMenu(plugin, homes, owner, plugin.getLocales().getLocale("homes_menu_title", owner.getUsername()), true, SortMode.ALPHABETICAL_ASCENDING);
    }

    @NotNull
    public static NewListMenu create(@NotNull HuskHomesGui plugin, @NotNull List<Home> homes, @NotNull User owner, @NotNull SortMode sortMode) {
        return new NewListMenu(plugin, homes, owner, plugin.getLocales().getLocale("homes_menu_title", owner.getUsername()), true, sortMode);
    }

    @NotNull
    public static NewListMenu createPublic(@NotNull HuskHomesGui plugin, @NotNull List<Home> homes, @NotNull User viewer) {
        return new NewListMenu(plugin, homes, viewer, plugin.getLocales().getLocale("public_homes_menu_title"), false, SortMode.ALPHABETICAL_ASCENDING);
    }

    @NotNull
    private static String[] getMenuLayout(@NotNull HuskHomesGui plugin) {
        final int rows = plugin.getSettings().getMenuSize();
        final String[] layout = new String[rows];
        for (int i = 0; i < rows - 1; i++) {
            layout[i] = "hhhhhhhhh";
        }
        layout[rows - 1] = "<maosf>";
        return layout;
    }

    @Override
    protected Consumer<InventoryGui> buildMenu() {
        return (menu) -> {
            if (mode == MenuMode.DELETE) {
                menu.setFiller(new ItemStack(plugin.getSettings().getDeleteFillerItem()));
            } else {
                menu.setFiller(new ItemStack(plugin.getSettings().getHomesFillerItem()));
            }

            homes.sort(Comparator.comparing(home -> home.getMeta().getName()));
            if (sortMode == SortMode.ALPHABETICAL_DESCENDING) {
                Collections.reverse(homes);
            }

            final GuiElementGroup homeGroup = new GuiElementGroup('h');
            homes.forEach(home -> homeGroup.addElement(createHomeButton(home)));
            menu.addElement(homeGroup);

            menu.addElement(new GuiPageElement('<', new ItemStack(plugin.getSettings().getPaginatePreviousPage()), GuiPageElement.PageAction.PREVIOUS, plugin.getLocales().getLocale("pagination_previous_page")));
            menu.addElement(new GuiPageElement('>', new ItemStack(plugin.getSettings().getPaginateNextPage()), GuiPageElement.PageAction.NEXT, plugin.getLocales().getLocale("pagination_next_page")));

            menu.addElement(createTeleportButton());
            menu.addElement(createDeleteButton());
            menu.addElement(createSortButton());
            if (addHomeButton) {
                menu.addElement(createAddButton());
            }
            menu.setElement(50, createHomeCountElement());
        };
    }

    private String formatServerName(String serverName) {
        switch (serverName.toLowerCase()) {
            case "minage_survie_001":
                return "Minage";
            case "survie":
                return "Survie";
            default:
                return serverName;
        }
    }

    private DynamicGuiElement createHomeButton(@NotNull Home home) {
        plugin.getLogger().info("Creating home button for: " + home.getName() + " at " + home.getX() + "," + home.getY() + "," + home.getZ());
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
                "§b" + home.getName(),
                plugin.getLocales().getLocale(
                        "item_description",
                        !home.getMeta().getDescription().isBlank() ?
                                textWrap(plugin, home.getMeta().getDescription()) :
                                plugin.getLocales().getLocale("item_description_blank")
                ),
                "",
                "§7Coordonnées : §e" + String.format("X: %.1f, Y: %.1f, Z: %.1f", home.getX(), home.getY(), home.getZ()),
                "§7Serveur : §e" + formatServerName(home.getServer()),
                "",
                mode == MenuMode.TELEPORT ? "§7Cliquez pour vous téléporter" : "§cCliquez pour supprimer"
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
                
                // Fermer le menu de confirmation
                click.getGui().close();
                
                // Attendre un court instant pour la synchronisation
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // Puis récupérer la liste mise à jour
                    api.getUserHomes(owner).thenAccept(updatedHomes -> {
                        plugin.getLogger().info("Got " + updatedHomes.size() + " homes after deletion");
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            // Rouvrir le menu principal avec les données à jour
                            NewListMenu.create(plugin, updatedHomes, owner).show(user);
                        });
                    }).exceptionally(e -> {
                        plugin.getLogger().log(Level.SEVERE, "Failed to refresh homes list after deletion", e);
                        return null;
                    });
                }, 10L); // Délai de 10 ticks (0.5 seconde)
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
            final ItemStack icon = new ItemStack(plugin.getSettings().getTeleportButton());
            final ItemMeta meta = icon.getItemMeta();
            if (mode == MenuMode.TELEPORT) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            icon.setItemMeta(meta);
            return new StaticGuiElement('m', icon, click -> {
                plugin.getLogger().info("Teleport mode button clicked");
                this.mode = MenuMode.TELEPORT;
                if (click.getWhoClicked() instanceof Player p) {
                    playSound(p, plugin.getSettings().getClickSound());
                }
                plugin.getLogger().info("Updating menu display - Mode: " + this.mode);
                click.getGui().setFiller(new ItemStack(plugin.getSettings().getHomesFillerItem()));
                click.getGui().draw();
                return true;
            }, plugin.getLocales().getLocale("teleport_mode_button"));
        });
    }

    private DynamicGuiElement createDeleteButton() {
        return new DynamicGuiElement('s', (viewer) -> {
            final ItemStack icon = new ItemStack(plugin.getSettings().getDeleteButton());
            final ItemMeta meta = icon.getItemMeta();
            if (mode == MenuMode.DELETE) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            icon.setItemMeta(meta);
            return new StaticGuiElement('s', icon, click -> {
                plugin.getLogger().info("Delete mode button clicked");
                this.mode = MenuMode.DELETE;
                if (click.getWhoClicked() instanceof Player p) {
                    playSound(p, plugin.getSettings().getClickSound());
                }
                plugin.getLogger().info("Updating menu display - Mode: " + this.mode);
                click.getGui().setFiller(new ItemStack(plugin.getSettings().getDeleteFillerItem()));
                click.getGui().draw();
                return true;
            }, plugin.getLocales().getLocale("delete_mode_button"));
        });
    }

    private StaticGuiElement createAddButton() {
        return new StaticGuiElement('a',
                new ItemStack(plugin.getSettings().getAddButton()),
                click -> {
                    if (click.getWhoClicked() instanceof Player player) {
                        final OnlineUser user = api.adaptUser(player);
                        this.close(user);
                        new AnvilGUI.Builder()
                                .title(plugin.getLocales().getLocale("add_home_title"))
                                .text(plugin.getLocales().getLocale("add_home_default_name"))
                                .itemLeft(new ItemStack(Material.OAK_SIGN))
                                .plugin(plugin)
                                .onClick((slot, state) -> {
                                    if (slot == AnvilGUI.Slot.OUTPUT) {
                                        try {
                                            plugin.getLogger().info("Attempting to create home: " + state.getText());
                                            api.createHome(owner, state.getText(), user.getPosition());
                                            plugin.getLogger().info("Home creation successful");
                                            
                                            // Afficher un message de chargement
                                            List<AnvilGUI.ResponseAction> actions = Collections.singletonList(
                                                AnvilGUI.ResponseAction.replaceInputText("Création en cours...")
                                            );
                                            
                                            // Attendre un court instant pour la synchronisation
                                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                                plugin.getLogger().info("Fetching updated homes list");
                                                api.getUserHomes(owner).thenAccept(updatedHomes -> {
                                                    plugin.getLogger().info("Got " + updatedHomes.size() + " homes");
                                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                                        // Fermer l'interface AnvilGUI
                                                        state.getPlayer().closeInventory();
                                                        // Rouvrir le menu avec les données à jour
                                                        NewListMenu.create(plugin, updatedHomes, owner).show(user);
                                                    });
                                                }).exceptionally(e -> {
                                                    plugin.getLogger().log(Level.SEVERE, "Failed to refresh homes list", e);
                                                    return null;
                                                });
                                            }, 10L); // Délai de 10 ticks (0.5 seconde)
                                            
                                            return actions;
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

    private GuiElement createHomeCountElement() {
        return new DynamicGuiElement('f', viewer -> {
            if (viewer instanceof Player player) {
                final OnlineUser onlineUser = api.adaptUser(player);
                plugin.getLogger().info("Attempting to get max homes for user: " + onlineUser.getUsername());
                final int currentHomes = homes.size();
                final int maxHomes = api.getMaxHomeSlots(onlineUser);
                plugin.getLogger().info("Retrieved max homes: " + maxHomes + " for user: " + onlineUser.getUsername());

                final ItemStack item = new ItemStack(Material.PLAYER_HEAD);
                final ItemMeta meta = item.getItemMeta();

                meta.setDisplayName("§bVos Homes");
                meta.setLore(Collections.singletonList(
                        "§f" + currentHomes + " / " + maxHomes
                ));
                item.setItemMeta(meta);

                return new StaticGuiElement('f', item, click -> true);
            }
            return new StaticGuiElement('f', new ItemStack(Material.AIR));
        });
    }

    private DynamicGuiElement createSortButton() {
        return new DynamicGuiElement('o', (viewer) -> {
            plugin.getLogger().info("Creating sort button with mode: " + sortMode);
            final ItemStack icon = new ItemStack(Material.COMPARATOR);
            final ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§bTrier les homes");
                final String sortDescription = sortMode == SortMode.ALPHABETICAL_ASCENDING ?
                    "§7Tri actuel : §eA → Z" :
                    "§7Tri actuel : §eZ → A";
                meta.setLore(Arrays.asList(
                    sortDescription,
                    "§7Cliquez pour changer l'ordre"
                ));
                icon.setItemMeta(meta);
                plugin.getLogger().info("Sort button meta set: " + meta.getDisplayName());
            }
            return new StaticGuiElement('o', icon, click -> {
                if (click.getWhoClicked() instanceof Player player) {
                    final OnlineUser user = api.adaptUser(player);
                    final SortMode nextSortMode = this.sortMode.getNext();
                    NewListMenu.create(plugin, homes, owner, nextSortMode).show(user);
                }
                return true;
            });
        });
    }

    private enum MenuMode {
        TELEPORT,
        DELETE
    }

    private enum SortMode {
        ALPHABETICAL_ASCENDING,
        ALPHABETICAL_DESCENDING;

        public SortMode getNext() {
            return this == ALPHABETICAL_ASCENDING ? ALPHABETICAL_DESCENDING : ALPHABETICAL_ASCENDING;
        }
    }
}