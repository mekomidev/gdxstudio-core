package com.kerberjg.gdxstudio.core;

import static com.kerberjg.gdxstudio.core.Stage.StageBuilder;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PerformanceCounter;

/** The main ApplicationListener
 * 
 *  @author kerberjg*/
public final class Game implements ApplicationListener {
	/** An enumerator representing the various states of the game engine */
	public static enum Status { INIT, RUN, PAUSE, RESUME, STOP };
	private static Status status = Status.STOP;
	
	private static void setGameStatus(final Status status) {
		Game.status = status;
		if(stage != null && status != Status.STOP)
			stage.triggerEvent("game:status", status);
	}
	
	public static Status getGameStatus() {
		return status;
	}
	
	/** Current stage */
	public static Stage stage;
	/** Stage queue; at the end of a frame, the stage in this variable is loaded */
	private static Stage nextStage;
	/** A map of Stage factories for fast Stage instancing */
	private static ObjectMap<String, StageBuilder> stages;
	
	/** Game's asset manager */
	public static AssetManager assets;
	
	private static int limitFps, maxDeltaTime;
	/** Time simulation scale factor */
	public static float deltaScale;
	
	/** Performance counters used for performance profiling */
	private PerformanceCounter loopCounter, drawCounter, updateCounter;
	
	private Game() {
		setGameStatus(Status.INIT);
		
		PerformanceCounter initCounter = new PerformanceCounter("Init time");
		initCounter.start();
		
		/*
		 *  Initializes the game
		 */
		assets = new AssetManager();
		stages = new ObjectMap<>(10);
		
		deltaScale = 1f;
		
		loopCounter = new PerformanceCounter("Loop duration");
		drawCounter = new PerformanceCounter("Draw duration");
		updateCounter = new PerformanceCounter("Update duration");
		
		/*
		 *  Loads configs
		 */
		JsonValue config = new JsonReader().parse(Gdx.files.internal("config.json"));
		
		initCounter.stop();
		System.out.println("Game initialized in " + (initCounter.current * 1000) + " ms");
	}
	
	@Override
	public void create() {
		stage.create();
	}

	@Override
	public void resize(int width, int height) {
		Gdx.app.debug("GAME", "Resizing screen to" + width + "x" + height);
		stage.resize(width, height);
	}

	/** Runs the game's main loop, limiting the FPS if requested and profiling the performance 
	 * 
	 * @author kerberjg */
	@Override
	public void render() {
		loopCounter.start();
		
		// Loads new stage if queued
		// TODO: solve this better!
		if(nextStage != null) {
			stage.dispose();
			stage = nextStage;
			nextStage = null;
			stage.create();
		}
		
		// Runs the game loop
		updateLogic();
		renderGraphics();
		
		loopCounter.stop();
		
		// Limits the rendering rate if enabled
		if(limitFps > 0) {
			try {
				float diff = maxDeltaTime - Gdx.graphics.getRawDeltaTime();
				
				if(diff > 0)
					Thread.sleep((long) (diff * 1000));
			}
			catch(InterruptedException e) {
				e.printStackTrace();
				Gdx.app.error("LOOP", "Failed to sleep: " + e.getMessage());
			}
		}
	}
	
	private void renderGraphics() {
		drawCounter.start();
		
		// Clears the screen
		Gdx.gl.glClearColor( 1f, 0f, 1f, 1f );
		Gdx.gl.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT );
		
		stage.render();
		
		drawCounter.stop();
	}
	
	private void updateLogic() {
		updateCounter.start();
		
		float delta = Gdx.graphics.getDeltaTime();
		stage.update(delta * deltaScale);
		
		updateCounter.stop();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		stage.pause();
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		stage.resume();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		stage.dispose();
	}
	
	/** Maps a string to a StageFactory for future Stage loading
	 * 
	 * @author kerberjg */
	public static void addStage(String name, StageBuilder sfactory) {
		stages.put(name, sfactory);
	}
	
	/** Instantiates a Stage via its registered factory and loads it on the next frame
	 * 
	 * @return whether the Stage was properly instanced
	 * @author kerberjg */
	public static boolean loadStage(String name) {
		StageBuilder sfactory = stages.get(name);
		
		if(sfactory != null) {
			nextStage = sfactory.build();
			return true;
		} else
			return false;
	}
	
	/** Sets the FPS limit for the game
	 * 
	 *  @param fps Game's max refresh rate rate. If 0, the limiting will be disabled
	 *  @author kerberjg */
	public static void setFPSLimit(int fps) {
		if(fps == 0) {
			limitFps = 0;
			maxDeltaTime = 0;
		}
		else {
			limitFps = fps;
			maxDeltaTime = 1000 / fps;
		}
	}
	
	/** The singleton instance of this class */
	private static Game instance;
	
	/** Returns the singleton instance of this class
	 * 
	 * @author kerberjg */
	public static Game getInstance() {
		if(instance == null)
			instance = new Game();
		
		return instance;
	}
}
