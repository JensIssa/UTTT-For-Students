package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Joshua implements IBot {
    //final int moveTimeMs = 1000;
    private String BOT_NAME = getClass().getSimpleName();
    final int moveTimeMs = 1000;
    GameSimulator createSimulator(IGameState state) {
        GameSimulator simulator = new GameSimulator(new GameState());
        simulator.setGameOver(GameOverState.Active);
        simulator.setCurrentPlayer(state.getMoveNumber() % 2);
        simulator.getCurrentState().setRoundNumber(state.getRoundNumber());
        simulator.getCurrentState().setMoveNumber(state.getMoveNumber());
        simulator.getCurrentState().getField().setBoard(state.getField().getBoard());
        simulator.getCurrentState().getField().setMacroboard(state.getField().getMacroboard());
        return simulator;
    }

    @Override
    public IMove doMove(IGameState state) {
        ArrayList<MySpicyRunnable> runablesList = new ArrayList<>();


        ArrayList<Thread> threads = new ArrayList<>();
        //creates 3 threads
        int threadCount = 3;
        for (int i = 0; i < threadCount; i++) {
            MySpicyRunnable runnable = new MySpicyRunnable(state);
            runablesList.add(runnable);
            Thread thread = new Thread(runnable);
            thread.start();
            threads.add(thread);
        }

        try {
            for (Thread thread : threads) thread.join();
        } catch (Exception e) {
            System.out.println("thread joining fucked up");
        }

        return combineMaps(runablesList);
    }

    public IMove combineMaps(ArrayList<MySpicyRunnable> runablesList) {
        HashMap<IMove, Integer> superMap = new HashMap<>();
        int highestValue = 0;
        IMove bestMove = null;
        Map.Entry<IMove, Integer> maxEntry = null;

        //create list of all hashmaps
        ArrayList<HashMap<IMove, Integer>> mapList = new ArrayList<>();
        for (MySpicyRunnable runnable : runablesList) {
            mapList.add(runnable.getHashMap());
        }

        //adds the hashmap from every thread together into one supermap
        for (HashMap<IMove, Integer> inputMap : mapList) {
            inputMap.forEach((IMove, Integer) -> superMap.merge(IMove, Integer, java.lang.Integer::sum));
        }

        //find move assosiated with highest value
        for (HashMap.Entry<IMove, Integer> entry : superMap.entrySet()) {
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
                maxEntry = entry;
            }
        }
        System.out.println(superMap);
        assert maxEntry != null;
        bestMove = maxEntry.getKey();
        System.out.println(bestMove);
        return bestMove;
    }

    /**
     * The code below is a simulator for simulation of gameplay. This is needed for AI.
     * It is put here to make the Bot independent of the GameManager and its subclasses/enums
     * Now this class is only dependent on a few interfaces: IMove, IField, and IGameState
     * You could say it is self-contained. The drawback is that if the game rules change, the simulator must be
     * changed accordingly, making the code redundant.
     */
    public HashMap<IMove, Integer> calculateWinningMove(IGameState state, int maxTimeMs) {
        long time = System.currentTimeMillis();
        Random rand = new Random();
        HashMap<IMove, Integer> potentialWinningMoves = new HashMap<>();

        while (System.currentTimeMillis() < time + maxTimeMs) { // check how much time has passed, stop if over maxTimeMs
            GameSimulator simulator = createSimulator(state);
            IGameState gs = simulator.getCurrentState();
            List<IMove> moves = gs.getField().getAvailableMoves();
            IMove randomMovePlayer = moves.get(rand.nextInt(moves.size()));
            IMove winnerMove = randomMovePlayer;
            int usPlayer = simulator.currentPlayer;

            while (simulator.getGameOver() == GameOverState.Active) {
                simulator.updateGame(randomMovePlayer);

                // Opponent plays randomly
                if (simulator.getGameOver() == GameOverState.Active) { // game still going
                    moves = gs.getField().getAvailableMoves();
                    IMove randomMoveOpponent = moves.get(rand.nextInt(moves.size()));
                    simulator.updateGame(randomMoveOpponent);
                }
                if (simulator.getGameOver() == GameOverState.Active) { // game still going
                    moves = gs.getField().getAvailableMoves();
                    randomMovePlayer = moves.get(rand.nextInt(moves.size()));
                }
            }

            if (simulator.getGameOver() == GameOverState.Win) {
                if (simulator.currentPlayer != usPlayer) {
                    potentialWinningMoves.putIfAbsent(winnerMove, 0);
                    int value = potentialWinningMoves.get(winnerMove);
                    potentialWinningMoves.replace(winnerMove, value + 1);
                }
            }
        }
        return potentialWinningMoves;
    }

    @Override
    public String getBotName() {
        return BOT_NAME;
    }

    public enum GameOverState {
        Active,
        Win,
        Tie
    }

    public class Move implements IMove {
        int x = 0;
        int y = 0;

        public Move(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Move move = (Move) o;
            return x == move.x && y == move.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    class GameSimulator {
        private final IGameState currentState;
        private int currentPlayer = 0; //player0 == 0 && player1 == 1
        private volatile GameOverState gameOver = GameOverState.Active;

        public void setGameOver(GameOverState state) {
            gameOver = state;
        }

        public GameOverState getGameOver() {
            return gameOver;
        }

        public void setCurrentPlayer(int player) {
            currentPlayer = player;
        }

        public IGameState getCurrentState() {
            return currentState;
        }

        public GameSimulator(IGameState currentState) {
            this.currentState = currentState;
        }

        public Boolean updateGame(IMove move) {
            if (!verifyMoveLegality(move))
                return false;

            updateBoard(move);
            currentPlayer = (currentPlayer + 1) % 2;

            return true;
        }

        private Boolean verifyMoveLegality(IMove move) {
            IField field = currentState.getField();
            boolean isValid = field.isInActiveMicroboard(move.getX(), move.getY());

            if (isValid && (move.getX() < 0 || 9 <= move.getX())) isValid = false;
            if (isValid && (move.getY() < 0 || 9 <= move.getY())) isValid = false;

            if (isValid && !field.getBoard()[move.getX()][move.getY()].equals(IField.EMPTY_FIELD))
                isValid = false;

            return isValid;
        }

        private void updateBoard(IMove move) {
            String[][] board = currentState.getField().getBoard();
            board[move.getX()][move.getY()] = currentPlayer + "";
            currentState.setMoveNumber(currentState.getMoveNumber() + 1);
            if (currentState.getMoveNumber() % 2 == 0) {
                currentState.setRoundNumber(currentState.getRoundNumber() + 1);
            }
            checkAndUpdateIfWin(move);
            updateMacroboard(move);

        }

        private void checkAndUpdateIfWin(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            int macroX = move.getX() / 3;
            int macroY = move.getY() / 3;

            if (macroBoard[macroX][macroY].equals(IField.EMPTY_FIELD) ||
                    macroBoard[macroX][macroY].equals(IField.AVAILABLE_FIELD)) {

                String[][] board = getCurrentState().getField().getBoard();

                if (isWin(board, move, "" + currentPlayer))
                    macroBoard[macroX][macroY] = currentPlayer + "";
                else if (isTie(board, move))
                    macroBoard[macroX][macroY] = "TIE";

                //Check macro win
                if (isWin(macroBoard, new Move(macroX, macroY), "" + currentPlayer))
                    gameOver = GameOverState.Win;
                else if (isTie(macroBoard, new Move(macroX, macroY)))
                    gameOver = GameOverState.Tie;
            }

        }

        private boolean isTie(String[][] board, IMove move) {
            int localX = move.getX() % 3;
            int localY = move.getY() % 3;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            for (int i = startX; i < startX + 3; i++) {
                for (int k = startY; k < startY + 3; k++) {
                    if (board[i][k].equals(IField.AVAILABLE_FIELD) ||
                            board[i][k].equals(IField.EMPTY_FIELD))
                        return false;
                }
            }
            return true;
        }


        public boolean isWin(String[][] board, IMove move, String currentPlayer) {
            int localX = move.getX() % 3;
            int localY = move.getY() % 3;
            int startX = move.getX() - (localX);
            int startY = move.getY() - (localY);

            //check col
            for (int i = startY; i < startY + 3; i++) {
                if (!board[move.getX()][i].equals(currentPlayer))
                    break;
                if (i == startY + 3 - 1) return true;
            }

            //check row
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][move.getY()].equals(currentPlayer))
                    break;
                if (i == startX + 3 - 1) return true;
            }

            //check diagonal
            if (localX == localY) {
                //we're on a diagonal
                int y = startY;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][y++].equals(currentPlayer))
                        break;
                    if (i == startX + 3 - 1) return true;
                }
            }

            //check anti diagonal
            if (localX + localY == 3 - 1) {
                int less = 0;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][(startY + 2) - less++].equals(currentPlayer))
                        break;
                    if (i == startX + 3 - 1) return true;
                }
            }
            return false;
        }

        private void updateMacroboard(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            for (int i = 0; i < macroBoard.length; i++)
                for (int k = 0; k < macroBoard[i].length; k++) {
                    if (macroBoard[i][k].equals(IField.AVAILABLE_FIELD))
                        macroBoard[i][k] = IField.EMPTY_FIELD;
                }

            int xTrans = move.getX() % 3;
            int yTrans = move.getY() % 3;

            if (macroBoard[xTrans][yTrans].equals(IField.EMPTY_FIELD))
                macroBoard[xTrans][yTrans] = IField.AVAILABLE_FIELD;
            else {
                // Field is already won, set all fields not won to avail.
                for (int i = 0; i < macroBoard.length; i++)
                    for (int k = 0; k < macroBoard[i].length; k++) {
                        if (macroBoard[i][k].equals(IField.EMPTY_FIELD))
                            macroBoard[i][k] = IField.AVAILABLE_FIELD;
                    }
            }
        }
    }


    class MySpicyRunnable implements Runnable {
        IGameState gameState;
        HashMap<IMove, Integer> hashMap;

        public MySpicyRunnable(IGameState state) {
            this.gameState = state;
        }

        @Override
        public void run() {
            hashMap = calculateWinningMove(gameState, moveTimeMs);
            System.out.println(hashMap);
        }

        public HashMap<IMove, Integer> getHashMap() {
            return hashMap;
        }
    }

}