package me.zeroeightsix.kami.command.commands;

import me.zeroeightsix.kami.NecronClient;
import me.zeroeightsix.kami.command.Command;
import me.zeroeightsix.kami.command.syntax.ChunkBuilder;
import me.zeroeightsix.kami.module.Module;
import me.zeroeightsix.kami.module.ModuleManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.ForgeVersion;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static me.zeroeightsix.kami.util.text.MessageSendHelper.sendChatMessage;

/**
 * @author l1ving
 * Updated by l1ving on 07/02/20
 */
public class TroubleshootCommand extends Command {
    public TroubleshootCommand() {
        super("troubleshoot", new ChunkBuilder().append("filter").append("minified").build(), "tsc");
        setDescription("Prints troubleshooting information");
    }

    @Override
    public void call(String[] args) {
        AtomicReference<String> enabled = new AtomicReference<>("");

        String f = "";
        if (args[0] != null) f = "(filter: " + args[0] + ")";

        for (Module module : ModuleManager.getModules()) {
            if (args[0] == null) {
                if (module.isEnabled()) {
                    enabled.set(enabled + module.getName().getValue() + ", ");
                }
            } else {
                if (module.isEnabled() && Pattern.compile(args[0], Pattern.CASE_INSENSITIVE).matcher(module.getName().getValue()).find()) {
                    enabled.set(enabled + module.getName().getValue() + ", ");
                }
            }
        }

        enabled.set(StringUtils.chop(StringUtils.chop(String.valueOf(enabled)))); // this looks horrible but I don't know how else to do it sorry
        sendChatMessage("Enabled modules: " + f + "\n" + TextFormatting.GRAY + enabled);
        if (args.length >= 2) return;
        sendChatMessage(ForgeVersion.getMajorVersion() + "." + ForgeVersion.getMinorVersion() + "." + ForgeVersion.getRevisionVersion() + "." + ForgeVersion.getBuildVersion());
        sendChatMessage(NecronClient.NAME + " " + NecronClient.VERSION);
        sendChatMessage("CPU: " + OpenGlHelper.getCpu() + " GPU: " + GlStateManager.glGetString(GL11.GL_VENDOR));
        sendChatMessage("Please send a screenshot of the full output to the developer or moderator who's helping you!");
    }

}
