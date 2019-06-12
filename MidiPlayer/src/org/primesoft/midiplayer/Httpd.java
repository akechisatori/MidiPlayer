package org.primesoft.midiplayer;

import fi.iki.elonen.NanoHTTPD;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.primesoft.midiplayer.midiparser.MidiParser;
import org.primesoft.midiplayer.midiparser.NoteFrame;
import org.primesoft.midiplayer.midiparser.NoteTrack;
import org.primesoft.midiplayer.track.GlobalTrack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static org.primesoft.midiplayer.MidiPlayerMain.say;

public class Httpd extends NanoHTTPD {
    public static Plugin plugins;
    public static BukkitTask httpd;
    public static GlobalTrack currentTrack;
    public static Map currentPlaying;
    public Httpd(Plugin plugin,String host, int port) {
        super(host,port);
        plugins = plugin;
    }
    public void startService() {
        Runnable httpd = new Runnable() {
            public void run() {
                try {
                    System.out.println("Midi API Service Running..");
                    Httpd.super.start(SOCKET_READ_TIMEOUT, false);
                } catch (IOException ioe) {
                    System.err.println("Couldn't start server:\n" + ioe);
                }

            }
        };
        BukkitTask httpd_task = Bukkit.getScheduler().runTaskAsynchronously(plugins, httpd);
        this.httpd = httpd_task;
    }
    public Response responseJSON(Object map) {
        return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJSON(map));
    }
    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> params = session.getParms();
        Method method = session.getMethod();
        if (Method.POST.equals(method)) {
            try {
                session.parseBody(params);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        MidiPlayerMain player = MidiPlayerMain.getInstance();
        try {
           switch (params.get("method").toString()) {
               case "stop":
                   player.getMusicPlayer().removeTrack(currentTrack);
                   return responseJSON(Map.of(
                           "status",200
                   ));
               case "status":
                   if (currentTrack == null) {
                       return responseJSON(Map.of(
                               "status",200,
                               "finished", true
                       ));
                   }
                   return responseJSON(Map.of(
                           "status",200,
                           "finished", currentTrack.isFinished(),
                           "track", currentPlaying
                   ));
               case "play":
                   if (currentTrack != null) {
                       player.getMusicPlayer().removeTrack(currentTrack);
                   }
                   String name = params.get("name");
                   String custom_name = params.get("custom_name");
                   String loop = params.get("loop");
                   String playing_name;
                   Boolean loopPlay = false;
                   if (loop == "true") {
                       loopPlay = true;
                   }
                   File midi = new File(plugins.getDataFolder() + "/midi/", name + ".mid");
                   if (!midi.exists()) {
                       return responseJSON(Map.of(
                               "status", 404
                       ));
                   }
                   if (custom_name == null) {
                       playing_name = name.replace("_", " ");
                   } else {
                       playing_name = custom_name;
                   }
                   NoteTrack noteTrack = MidiParser.loadFile(midi);
                   if (noteTrack.isError()) {
                       return responseJSON(Map.of(
                               "status",500,
                               "message",noteTrack.getMessage()
                       ));
                   }

                   final NoteFrame[] notes = noteTrack.getNotes();
                   currentTrack = new GlobalTrack(MidiPlayerMain.s_instance, notes, loopPlay);
                   currentPlaying = Map.of(
                           "name",name.replace("_", " "),
                           "custom_name", (custom_name == null) ? "" : custom_name,
                           "realpath", midi.getCanonicalPath().toString(),
                           "loop", loopPlay
                   );
                   player.getMusicPlayer().playTrack(currentTrack);
                   new BukkitRunnable(){
                       @Override
                       public void run() {
                           Bukkit.getOnlinePlayers().forEach((p) -> {
                               p.sendActionBar(ChatColor.RED + "正在播放: " + ChatColor.GREEN + playing_name);
                           });
                       }
                   }.runTaskAsynchronously(plugins);
                   return responseJSON(Map.of(
                           "status",200
                   ));
               case "list":
                   File[] list = new File(plugins.getDataFolder() + "/midi/").listFiles();
                   ArrayList final_list = new ArrayList<>();
                   for (File file : list) {
                       final_list.add(Map.of(
                               "name", file.getName().replace(".mid","")
                       ));
                   }
                   return responseJSON(Map.of(
                           "status",200,
                           "list",final_list
                   ));
           }
        } catch (Exception e) {
//           e.printStackTrace();
        }
        return responseJSON(Map.of(
                "status",200
        ));
    }
}
