package fun.bm.mili.regression;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fun.bm.mili.config.modules.experiment.CommandConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class R2CommandReaderTestSuite {
    @Test void serverParsesUnicodeWordsWithoutChangingDelimitersOrPermissions() throws CommandSyntaxException {
        boolean saved = CommandConfig.allowNonAsciiUnquotedString;
        try {
            CommandConfig.allowNonAsciiUnquotedString = false;
            assertEquals("ASCII", new StringReader("ASCII测试").readUnquotedString());
            assertEquals("测试 区", new StringReader("\"测试 区\"").readString());
            var commands = new CommandDispatcher<Boolean>();
            commands.register(LiteralArgumentBuilder.<Boolean>literal("fixture").requires(Boolean::booleanValue)
                .then(RequiredArgumentBuilder.<Boolean, String>argument("name", StringArgumentType.word())
                    .executes(ctx -> StringArgumentType.getString(ctx, "name").length())));
            assertThrows(CommandSyntaxException.class, () -> commands.execute("fixture 测试２", true));
            CommandConfig.allowNonAsciiUnquotedString = true;
            assertEquals(3, commands.execute("fixture 测试２", true));
            assertThrows(CommandSyntaxException.class, () -> commands.execute("fixture 测试２", false));
            for (String delimiter : new String[]{" ", ":", "/", "\"", ";", "\n"}) {
                var reader = new StringReader("测试２" + delimiter + "其他");
                assertEquals("测试２", reader.readUnquotedString());
                assertEquals(3, reader.getCursor());
            }
        } finally {
            CommandConfig.allowNonAsciiUnquotedString = saved;
        }
    }
}
