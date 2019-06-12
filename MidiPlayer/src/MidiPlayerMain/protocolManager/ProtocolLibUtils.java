package MidiPlayerMain.protocolManager;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.primesoft.midiplayer.MidiPlayerMain;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
public class ProtocolLibUtils {
    static ProtocolManager get() {
        return ProtocolLibrary.getProtocolManager();
    }
    public static PacketContainer of(PacketType type) {
        return get().createPacket(type);
    }
    public static Result send(Player player, PacketContainer... packets) {
        try {
            ProtocolManager manager = get();
            for (PacketContainer packet : packets) {
                if (packet.getType().isServer())
                    manager.sendServerPacket(player, packet);
                else
                    manager.recieveClientPacket(player, packet);
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            return Result.failure(e);
        }
        return Result.success();
    }

    static Result send(Player player, Runnable whileException, PacketContainer... packets) {
        Result result = send(player, packets);
        if (result.isFailure()) whileException.run();
        return result;
    }

    /**
     * 监听包的发送
     *
     * @param consumer         发送处理
     * @param sendingWhiteList 需要监听的包
     */
    static void listenOnPacketSending(Consumer<PacketEvent> consumer, PacketType... sendingWhiteList) {
        get().addPacketListener(new PacketListener() {
            @Override
            public void onPacketSending(PacketEvent event) {
                consumer.accept(event);
            }

            @Override
            public void onPacketReceiving(PacketEvent event) {

            }

            @Override
            public ListeningWhitelist getSendingWhitelist() {
                return ListeningWhitelist.newBuilder().types(sendingWhiteList).build();
            }

            @Override
            public ListeningWhitelist getReceivingWhitelist() {
                return ListeningWhitelist.EMPTY_WHITELIST;
            }

            @Override
            public Plugin getPlugin() {
                return MidiPlayerMain.s_instance;
            }
        });
    }

    /**
     * 监听包的接收
     *
     * @param consumer           接收处理
     * @param receivingWhiteList 需要监听的包
     */
    static void listenOnPacketReceiving(Consumer<PacketEvent> consumer, PacketType... receivingWhiteList) {
        get().addPacketListener(new PacketListener() {
            @Override
            public void onPacketSending(PacketEvent event) {
            }

            @Override
            public void onPacketReceiving(PacketEvent event) {
                consumer.accept(event);
            }

            @Override
            public ListeningWhitelist getReceivingWhitelist() {
                return ListeningWhitelist.newBuilder().types(receivingWhiteList).build();
            }

            @Override
            public ListeningWhitelist getSendingWhitelist() {
                return ListeningWhitelist.EMPTY_WHITELIST;
            }

            @Override
            public Plugin getPlugin() {
                return MidiPlayerMain.s_instance;
            }
        });
    }

    /**
     * 转换字符串数组为 NBTTagString[] 并封装到 NbtCompound 中，结构如下：
     * text1: lines[0]
     * text2: lines[1]
     * ...
     *
     * @param lines 文字
     */
    static NbtCompound buildStringsNBTBase(String... lines) {
        NbtCompound nbt = NbtFactory.ofCompound("");
        for (int i = 0; i < lines.length; i++) {
            nbt.put("text" + (i + 1), lines[i]);
        }
        return nbt;
    }

    /**
     * 转换字符串数组为 WrappedChatComponent[]
     */
    static WrappedChatComponent[] buildChatComponentArray(String... components) {
        WrappedChatComponent[] cs = new WrappedChatComponent[components.length];
        for (int i = 0; i < components.length; i++) {
            cs[i] = WrappedChatComponent.fromText(components[i]);
        }
        return cs;
    }
}
