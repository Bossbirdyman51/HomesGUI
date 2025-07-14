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

package net.william278.huskhomes.gui;

import net.kyori.adventure.platform.AudienceProvider;
import net.william278.annotaml.Annotaml;
import net.william278.desertwell.util.Version;
import net.william278.huskhomes.gui.config.Locales;
import net.william278.huskhomes.gui.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

public interface HuskHomesGuiPlugin {

    @NotNull
    default Locales loadLocales() {
        try {
            return Annotaml.create(new File(getDataFolder(), "messages-" + loadSettings().getLanguage() + ".yml"),
                    Annotaml.create(Locales.class, getResource("locales/" + loadSettings().getLanguage() + ".yml")).get()).get();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | IOException e) {
            throw new IllegalStateException("Failed to load locales file", e);
        }
    }

    @NotNull
    default Settings loadSettings() {
        try {
            final File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                getDataFolder().mkdirs();
                try (InputStream input = getResource("config.yml")) {
                    if (input != null) {
                        java.nio.file.Files.copy(input, configFile.toPath());
                    } else {
                        throw new IllegalStateException("Could not find config.yml in plugin resources");
                    }
                }
            }
            return Annotaml.create(configFile, Settings.class).get();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | IOException e) {
            throw new IllegalStateException("Failed to load config file", e);
        }
    }

    @NotNull
    AudienceProvider getAudiences();

    @NotNull
    Version getPluginVersion();

    @NotNull
    Settings getSettings();

    @NotNull
    Locales getLocales();

    File getDataFolder();

    InputStream getResource(@NotNull String fileName);

}
