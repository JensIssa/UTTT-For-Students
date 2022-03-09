package dk.easv.bll.bot;

import dk.easv.bll.game.GameManager;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.List;

public class OldBot implements IBot {
    private static final String BOTNAME = "Big boi bot";
    int playerScore = 0;
    int bestOutcome;
    /**
     * Makes a turn. Implement this method to make your dk.easv.bll.bot do something.
     *
     * @param state the current dk.easv.bll.game state
     * @return The column where the turn was made.
     */

    @Override
    public IMove doMove(IGameState state) {
        IGameState stateOfGame = new GameState(state);
        GameManager currentGame = new GameManager(stateOfGame);
        List<IMove> availableMoves = state.getField().getAvailableMoves();
        IMove selectedMove = state.getField().getAvailableMoves().get(0);

        for (IMove move : availableMoves) {
            int score = smartestBotMove(0, currentGame, false);
            if(score > bestOutcome)
            {
                bestOutcome = score;
                selectedMove = move;
            }
        }
        return selectedMove;
    }

    @Override
    public String getBotName() {
        return BOTNAME;
    }
    private int smartestBotMove(int depth, GameManager currentGame, boolean playerOptimization) {

        playerScoreCalculation(currentGame);
        if (!playerOptimization) {
            for (IMove move : currentGame.getCurrentState().getField().getAvailableMoves()) {
                bestOutcome = Integer.MAX_VALUE;
                currentGame.updateGame(move);
                smartestBotMove(depth + 1, currentGame, true);
            }
        }
        else {
            for (IMove move : currentGame.getCurrentState().getField().getAvailableMoves()) {
                bestOutcome = Integer.MIN_VALUE;
                currentGame.updateGame(move);
                smartestBotMove(depth - 1, currentGame, false);
            }
        }
        return bestOutcome;
    }

    /*
    Kigger på bottens score, og ser hvad der virker bedst.
     */
    public int playerScoreCalculation(GameManager gameScore)
    {
        if (gameScore.getGameOver() == GameManager.GameOverState.Win)
            if (gameScore.getCurrentPlayer() == playerScore) {
                playerScore++;
            } else {
                playerScore--;
            }
        else if (gameScore.getGameOver() == GameManager.GameOverState.Tie){
            return 0;
        }
        return playerScore;
    }

}