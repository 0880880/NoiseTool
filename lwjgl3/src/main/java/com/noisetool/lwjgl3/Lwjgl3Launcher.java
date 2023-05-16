package com.noisetool.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.noisetool.Main;

public class Lwjgl3Launcher {
	public static void main(String[] args) {
		createApplication();
	}

	private static void createApplication() {
		new Lwjgl3Application(new Main(), getDefaultConfiguration());
	}

	private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
		Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
		configuration.setMaximized(true);
		configuration.setTitle("NoiseTool");
		configuration.useVsync(true);
		configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);
		//configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
		return configuration;
	}
}