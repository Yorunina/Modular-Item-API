package smartin.miapi.config;

import dev.architectury.platform.Platform;
import net.minecraft.util.math.ColorHelper;
import smartin.miapi.config.oro_config.BooleanConfigItem;
import smartin.miapi.config.oro_config.Config;
import smartin.miapi.config.oro_config.ConfigItemGroup;
import smartin.miapi.config.oro_config.IntegerConfigItem;

import java.io.File;
import java.util.List;

import static org.spongepowered.include.com.google.common.collect.ImmutableList.of;

public class MiapiConfig extends Config {
    public static EnchantmentGroup enchantmentGroup = new EnchantmentGroup();
    public static OtherConfigGroup otherGroup = new OtherConfigGroup();
    public static ServerConfig serverConfig = new ServerConfig();
    public static ColorGroup colorConfig = new ColorGroup();
    public static ClientConfig clientConfig = new ClientConfig();
    protected static MiapiConfig INSTANCE = new MiapiConfig();

    public static MiapiConfig getInstance() {
        return INSTANCE;
    }

    protected MiapiConfig() {
        super(List.of(clientConfig, serverConfig),
                new File(Platform.getConfigFolder().toString(), "miapi.json"),
                "miapi_server");
        if (Platform.isModLoaded("cloth_config")) {
        }
        this.saveConfigToFile();
    }

    public static class ClientConfig extends ConfigItemGroup {
        public static BooleanConfigItem developmentMode = new BooleanConfigItem(
                "server",
                Platform.isDevelopmentEnvironment(),
                "Client Side config");

        protected ClientConfig() {
            super(of(colorConfig), "client");
        }
    }

    public static class ColorGroup extends ConfigItemGroup {
        public static IntegerConfigItem red = new IntegerConfigItem(
                "red",
                ColorHelper.Argb.getArgb(255, 196, 19, 19),
                "red for gui");
        public static IntegerConfigItem green = new IntegerConfigItem(
                "green",
                ColorHelper.Argb.getArgb(255, 0, 255, 0),
                "green for gui");

        protected ColorGroup() {
            super(of(red, green), "Gui Colors");
        }
    }

    public static class ServerConfig extends ConfigItemGroup {
        protected ServerConfig() {
            super(of(otherGroup, enchantmentGroup), "server");
        }
    }

    public static class OtherConfigGroup extends ConfigItemGroup {
        public static BooleanConfigItem developmentMode = new BooleanConfigItem(
                "development_mode",
                Platform.isDevelopmentEnvironment(),
                "Development mode of Miapi - DO NOT ENABLE IF U DONT KNOW WHAT IT DOES");

        protected OtherConfigGroup() {
            super(of(developmentMode), "other");
        }
    }

    public static class EnchantmentGroup extends ConfigItemGroup {
        public static BooleanConfigItem betterInfinity = new BooleanConfigItem(
                "better_infinity",
                true,
                "Modular Bows no longer require any arrows with infinity");
        public static BooleanConfigItem betterLoyalty = new BooleanConfigItem(
                "better_loyalty",
                true,
                "Loyalty triggers in the void with modular Items");

        protected EnchantmentGroup() {
            super(of(betterInfinity, betterLoyalty), "enchants");
        }
    }
}
