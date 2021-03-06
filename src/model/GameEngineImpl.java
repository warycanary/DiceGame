package model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import model.interfaces.DicePair;
import model.interfaces.GameEngine;
import model.interfaces.Player;
import view.observers.interfaces.GameEngineCallback;

import java.util.ArrayList;

public class GameEngineImpl implements GameEngine {
	
	/* Collections for Storing Callbacks and Players */
	private List<Player> playerList = new ArrayList<>();
	private List<GameEngineCallback> callbackList = new ArrayList<>();
	/* DicePair storing house roll */
	private DicePair houseResult = null;
	
	/* Returns the player's bet */
	@Override
	public boolean placeBet(Player player, int bet) {
		return player.placeBet(bet);
	}

	/* Rolls the dice for the current player */
	@Override
	public void rollPlayer(Player player, int initialDelay, 
			int finalDelay, int delayIncrement) {
		
		DicePair dicePair = roll(player, initialDelay, finalDelay,
				delayIncrement, true);
		
		player.setRollResult(dicePair);
		
		/* Reports results to callback */
		for (GameEngineCallback callback : callbackList) {
			callback.result(player, player.getRollResult(), this);
		}
	}

	/* Rolls the dice for the house */
	@Override
	public void rollHouse(int initialDelay, int finalDelay,
			int delayIncrement) {
		
		houseResult = roll(null, initialDelay, finalDelay,
				delayIncrement, false);
		
		/* Exception can occur if a player is added or the game is reset while
		 * the house is rolling */
		try {
			/* Checks if players win against the house */
			resolveBets(houseResult);
			
			/* Reports results to callback */
			for (GameEngineCallback callback : callbackList) {
				callback.houseResult(houseResult, this);
			}
		} catch (NullPointerException e) {
			System.err.println("\nGame Engine Error: Players were modified while "
					+ "house was rolling. Bets were not resolved.");
		}
		
	}
	
	/* Shared code for rolling dice. Returns a DicePair */
	private DicePair roll(Player player, int initialDelay, int finalDelay,
			int delayIncrement, boolean isPlayer) {
		
		int delay = initialDelay;
		
		/* Rolls Until Final Delay is reached */
		while (delay < finalDelay) {
			
			/* Sleeps */
			try {
				Thread.sleep(delay); 
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			/* Initializes DicePair and rolls dice */
			DicePair dicePair = rollDice();
					
			/* Checks if player or house is rolling and calls appropriate
			 * callback method */
			for (GameEngineCallback callback : callbackList) {
				if (isPlayer)
					callback.intermediateResult(player, dicePair, this);
				else
					callback.intermediateHouseResult(dicePair, this);
			}
			
			delay += delayIncrement;
		}
		
		/* Generates and returns final roll result */
		return rollDice();
	}
	
	private DicePair rollDice() {
		int dice1 = ThreadLocalRandom.current().nextInt(1, NUM_FACES + 1);
		int dice2 = ThreadLocalRandom.current().nextInt(1, NUM_FACES + 1);
		return new DicePairImpl(dice1, dice2, NUM_FACES);
	}
	
	/* Compares each player to House roll and assigns points */
	private void resolveBets(DicePair dicePair) {
		for (Player player: playerList) {
			/* If players roll is greater than the houses roll, add bet */
			if (getDiceTotal(player.getRollResult()) > getDiceTotal(dicePair)) {
				player.setPoints(player.getPoints() + player.getBet());
				
			/* If the players roll is less than the houses, subtract bet */
			} else if (getDiceTotal(player.getRollResult()) < getDiceTotal(dicePair)) {
				player.setPoints(player.getPoints() - player.getBet());
			}
		}
	}
	
	private int getDiceTotal(DicePair dicePair) {
		return dicePair.getDice1() + dicePair.getDice2();
	}

	/* Adds a player to the GameEngineImpl collection */
	@Override
	public void addPlayer(Player player) {
		playerList.add(player);
	}

	/* Returns the player with the requested position */
	@Override
	public Player getPlayer(int index) {
		if (index >= 0 && index < playerList.size())
			return playerList.get(index);
		return null;
	}

	/* Removes a player from the collection */
	@Override
	public void removePlayer(Player player) {
        playerList.remove(player);
    }
	
	/* Adds a GameEngineCallback to a collection */
	@Override
	public void addGameEngineCallback(GameEngineCallback gameEngineCallback) {
		callbackList.add(gameEngineCallback);
	}

	/* Removes a callback from the collection */
	@Override
	public boolean removeGameEngineCallback(
			GameEngineCallback gameEngineCallback) {
		return callbackList.remove(gameEngineCallback);
	}

	/* Returns the players collection */
	@Override
	public Collection<Player> getAllPlayers() {
		return Collections.unmodifiableCollection(playerList);
	}
	
	/* Returns the house result */
	@Override
	public DicePair getHouseResult() {
		return this.houseResult;
	}
	
	/* Clears the house result */
	@Override
	public void clearHouseResult() {
		this.houseResult = null;
	}
	
}
