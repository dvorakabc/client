package me.zeroeightsix.kami;

import com.google.common.base.Converter;
import com.google.gson.JsonObject;
import me.zeroeightsix.kami.command.CommandManager;
import me.zeroeightsix.kami.event.ForgeEventProcessor;
import me.zeroeightsix.kami.event.KamiEventBus;
import me.zeroeightsix.kami.gui.kami.KamiGUI;
import me.zeroeightsix.kami.gui.mc.KamiGuiUpdateNotification;
import me.zeroeightsix.kami.manager.ManagerLoader;
import me.zeroeightsix.kami.manager.managers.FileInstanceManager;
import me.zeroeightsix.kami.module.Module;
import me.zeroeightsix.kami.module.ModuleManager;
import me.zeroeightsix.kami.setting.Setting;
import me.zeroeightsix.kami.setting.Settings;
import me.zeroeightsix.kami.util.ConfigUtils;
import me.zeroeightsix.kami.util.graphics.font.KamiFontRenderer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;

@Mod(
        modid = NecronClient.ID,
        name = NecronClient.NAME,
        version = NecronClient.VERSION
)
public class NecronClient {

    public static final String NAME = "NECRON Client";
    public static final String ID = "necron";
    public static final String VERSION = "beta.0.20-2b2d263f"; // Used for debugging. R.MM.DD-hash format.
    public static final String VERSION_SIMPLE = VERSION; // Shown to the user. R.MM.DD[-beta] format.
    public static final String VERSION_MAJOR = VERSION; // Used for update checking. RR.MM-beta.X.XX format.
    public static final int BUILD_NUMBER = -1; // Do not remove, currently unused but will be used in the future.

    public static final String APP_ID = "785818119242514432";

    public static final String DOWNLOADS_API = "https://raw.githubusercontent.com/dvorakabc/client/master/latest.json";
    public static final String CAPES_JSON = "https://raw.githubusercontent.com/dvorakabc/client/master/capes.json";
    public static final String GITHUB_LINK = "https://github.com/dvorakabc/client/";
    public static final String WEBSITE_LINK = "https://github.com/dvorakabc/client/";

    public static final String DIRECTORY = "NECRON/";
    public static final Logger LOG = LogManager.getLogger("NECRON");

    @Mod.Instance
    public static NecronClient INSTANCE;
    public static Thread MAIN_THREAD;

    private KamiGUI guiManager;
    private CommandManager commandManager;
    private Setting<JsonObject> guiStateSetting;

    @SuppressWarnings("ResultOfMethodCallIgnored") // Java meme
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        final File directory = new File(DIRECTORY);
        if (!directory.exists()) directory.mkdir();

        MAIN_THREAD = Thread.currentThread();
        KamiGuiUpdateNotification.updateCheck();
        ModuleManager.preLoad();
        ManagerLoader.preLoad();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOG.info("Initializing " + NAME + " " + VERSION);
        ModuleManager.load();
        ManagerLoader.load();

        MinecraftForge.EVENT_BUS.register(ForgeEventProcessor.INSTANCE);

        guiStateSetting = Settings.custom("gui", new JsonObject(), new Converter<JsonObject, JsonObject>() {
            @Override
            protected JsonObject doForward(@Nullable JsonObject jsonObject) {
                return jsonObject;
            }

            @Override
            protected JsonObject doBackward(@Nullable JsonObject jsonObject) {
                return jsonObject;
            }
        }).buildAndRegister("");
        guiManager = new KamiGUI();
        guiManager.initializeGUI();
        commandManager = new CommandManager();

        FileInstanceManager.fixEmptyFiles();
        ConfigUtils.INSTANCE.loadAll();

        // After settings loaded, we want to let the enabled modules know they've been enabled (since the setting is done through reflection)
        for (Module module : ModuleManager.getModules()) {
            if (module.getAlwaysListening()) KamiEventBus.INSTANCE.subscribe(module);
            if (module.isEnabled()) module.enable();
        }

        // Need to reload the font after the settings were loaded
        KamiFontRenderer.INSTANCE.reloadFonts();

        LOG.info(NAME + " Mod initialized!");
    }

    public KamiGUI getGuiManager() {
        return guiManager;
    }

    public void setGuiManager(KamiGUI guiManager) {
        this.guiManager = guiManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public Setting<JsonObject> getGuiStateSetting() {
        return guiStateSetting;
    }

}
