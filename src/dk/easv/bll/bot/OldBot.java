package dk.easv.bll.bot;

import dk.easv.bll.game.GameManager;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OldBot implements IBot {
    private static final String BOTNAME = "Big boi bot";
    private int playerScore = 0;
    private int bestOutcome;
    /**
     * Makes a turn. Implement this method to make your dk.easv.bll.bot do something.
     *
     * @param state the current dk.easv.bll.game state
     * @return The column where the turn was made.
     */

    @Override
    public IMove doMove(IGameState state) {
        GameManager currentGame = new GameManager(state);
        List<IMove> availableMoves = state.getField().getAvailableMoves();
        IMove selectedMove = state.getField().getAvailableMoves().get(0);

        List<IMove> winMoves = availableWinningMoves(state);
        if(!winMoves.isEmpty()) {
            System.out.println(winMoves);
            selectedMove = winMoves.get(0);
            return selectedMove;
        }

        for (IMove move : availableMoves) {
            int score = playerScoreCalculation(currentGame);
            bestOutcome = smartestBotMove(5, currentGame, true);
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
    private int smartestBotMove(int depth, GameManager currentGame, boolean isMax) {

        if (isMax) {
            for (IMove move : currentGame.getCurrentState().getField().getAvailableMoves()) {
                bestOutcome = Integer.MIN_VALUE;
                currentGame.updateGame(move);
                bestOutcome = Math.min(bestOutcome, smartestBotMove(depth -1, currentGame, false));
            }
        }
        else {
            for (IMove move : currentGame.getCurrentState().getField().getAvailableMoves()) {
                bestOutcome = Integer.MAX_VALUE;
                currentGame.updateGame(move);
                bestOutcome = Math.max(bestOutcome, smartestBotMove(-1, currentGame, true));
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

    public boolean isWinningMove(IGameState state, IMove move, String player){
        // Clones the array and all values to a new array, so we don't mess with the game
        String[][] board = Arrays.stream(state.getField().getBoard()).map(String[]::clone).toArray(String[][]::new);

        //Places the player in the game. Sort of a simulation.
        board[move.getX()][move.getY()] = player;

        int startX = move.getX()-(move.getX()%3);
        if(board[startX][move.getY()]==player)
            if (board[startX][move.getY()] == board[startX+1][move.getY()] &&
                    board[startX+1][move.getY()] == board[startX+2][move.getY()])
                return true;

        int startY = move.getY()-(move.getY()%3);
        if(board[move.getX()][startY]==player)
            if (board[move.getX()][startY] == board[move.getX()][startY+1] &&
                    board[move.getX()][startY+1] == board[move.getX()][startY+2])
                return true;


        if(board[startX][startY]==player)
            if (board[startX][startY] == board[startX+1][startY+1] &&
                    board[startX+1][startY+1] == board[startX+2][startY+2])
                return true;

        if(board[startX][startY+2]==player)
            if (board[startX][startY+2] == board[startX+1][startY+1] &&
                    board[startX+1][startY+1] == board[startX+2][startY])
                return true;

        return false;
    }

    public List<IMove> availableWinningMoves(IGameState state){
        String player = "1";
        if(state.getMoveNumber()%2==0)
            player="0";

        List<IMove> avail = state.getField().getAvailableMoves();

        List<IMove> winningMoves = new ArrayList<>();
        for (IMove move:avail) {
            if(isWinningMove(state,move,player))
                winningMoves.add(move);
        }
        return winningMoves;
    }

}