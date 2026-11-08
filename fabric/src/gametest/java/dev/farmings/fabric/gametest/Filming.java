package dev.farmings.fabric.gametest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import net.minecraft.client.Minecraft;

/**
 * Films a scene with macOS's screencapture, only the game window's rectangle, into {@code build/scene-videos/<scene>.mov} (the
 * Gradle run folder's parent). A {@code caffeinate} runs alongside so the display stays awake and the recording is not black. Off the Mac, or with no permission to record, the scene still runs, unfilmed.
 */
final class Filming {
	private final Process process;
	private final Process awake;

	private Filming(Process process, Process awake) {
		this.process = process;
		this.awake = awake;
	}

	static Filming start(Minecraft client, String scene) {
		if (!System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).contains("mac")) {
			return new Filming(null, null);
		}
		try {
			File folder = new File("../../scene-videos").getCanonicalFile();
			folder.mkdirs();
			File out = new File(folder, scene + ".mov");
			out.delete();
			var window = client.getWindow();
			String rect = window.getX() + "," + window.getY() + "," + window.getScreenWidth() + "," + window.getScreenHeight();
			Process awake = new ProcessBuilder("caffeinate", "-d", "-i", "-t", "600").start();   // keeps the display and the Mac awake, at most 10 minutes
			Process process = new ProcessBuilder("screencapture", "-x", "-v", "-R", rect, out.getPath()).redirectErrorStream(true)
					.redirectOutput(new File(folder, scene + ".log")).start();
			System.out.println("[farmings-test] filming " + scene + " at " + rect + " to " + out);
			return new Filming(process, awake);
		} catch (IOException ex) {
			System.out.println("[farmings-test] cannot film " + scene + ": " + ex.getMessage());
			return new Filming(null, null);
		}
	}

	/** Any character on its input stops the recording and writes the file. */
	void stop() {
		if (awake != null) {
			awake.destroy();
		}
		if (process == null) {
			return;
		}
		try {
			OutputStream in = process.getOutputStream();
			in.write('\n');
			in.flush();
			if (!process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)) {
				new ProcessBuilder("kill", "-INT", Long.toString(process.pid())).start().waitFor();
				process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
			}
		} catch (IOException | InterruptedException ex) {
			System.out.println("[farmings-test] filming did not stop cleanly: " + ex.getMessage());
		}
	}
}
