package corridors;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.rmi.NotBoundException;
//import java.util.concurrent.ThreadLocalRandom;

public class Client {
    static String[][] latticeSides;
    static String[][] cellColors;
    static int gamerId = 0;
    static int currGamerId = 0;
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String UNIQUE_NAME = "corridors_stub";
    private static BufferedReader commandLineReader;
    private static int nLatticeCellRow = 0;   // number of rows (the lattice height)
    private static int nLatticeCellCol = 0;   // number of columns (the lattice width)
    private static int nLatticeSymbRow = 0;   // кол-во символов в строке
    private static int nLatticeSymbCol = 0;
    private static void printLattice() {
        StringBuilder s = new StringBuilder();
        for (int symbRowIdx = 0; symbRowIdx < nLatticeSymbRow; symbRowIdx++) {
            for (int symbColIdx = 0; symbColIdx < nLatticeSymbCol; symbColIdx++) {
                s.append(cellColors[symbRowIdx][symbColIdx]);
                s.append(latticeSides[symbRowIdx][symbColIdx]).append(ANSI_RESET);
            }
            System.out.println(s);
            s.setLength(0);
        }
    }

    private static void startGameThread(Game game) {
        Thread gameThread = new Thread(() -> {
            commandLineReader = new BufferedReader(new InputStreamReader(System.in));

            while(true) {
                // update the variables from server every 500 ms
                try {
                    // call remote methods
                    latticeSides = game.getLatticeSides();  // get the lattice from the server
                    cellColors = game.getCellColors();      // get the colors of sides from the server
                    currGamerId = game.getCurrGamerId();    // get the id of current gamer from the server
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }

                if (gamerId == currGamerId) {
                    gamerMove(game);            // the current gamer makes a move
                }

                // check if game is over
                if (checkGameStatus()) {
                    break;
                }

                // sleep for 500 ms
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        gameThread.start();
    }

    private static void gamerMove(Game game) {

        // create the temporary variables for input
        String inputRowStr = "", inputColumnStr = "", inputSide = "";
        int inputRow = -1, inputColumn = -1;

        boolean isInputValid = false; // flag to update by game server
        boolean isInTempValid; // flag for local check by gamer

        // try until the input approved by game server
        while (!isInputValid){

            // print the current lattice for convenience
            printLattice();
            System.out.println("Ваш ход, игрок " + Integer.toString(gamerId));

            isInTempValid = false; // initialize the flag by false

            // try until the gamer input is correct
            while (!isInTempValid || inputRowStr.isEmpty()){
                System.out.println("Введите номер строки (от 1 до " + Integer.toString(nLatticeCellRow) + ")");

                // read the input row
                try {
                    inputRowStr = commandLineReader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // try to parse input row as int
                isInTempValid = true;
                try {
                    inputRow = Integer.parseInt(inputRowStr);
                } catch (NumberFormatException e) {
                    isInTempValid = false;
                }

                // check the lattice height condition
                if (isInTempValid && (inputRow < 1 || inputRow > nLatticeCellRow)) {
                    isInTempValid = false;
                }
            }

            isInTempValid = false;
            // try until the gamer input is correct
            while (!isInTempValid || inputColumnStr.isEmpty()){
                System.out.println("Введите номер столбца (от 1 до " + Integer.toString(nLatticeCellCol) + ")");

                // read the input column
                try {
                    inputColumnStr = commandLineReader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // try to parse input column as int
                isInTempValid = true;
                try {
                    inputColumn = Integer.parseInt(inputColumnStr);
                } catch (NumberFormatException e) {
                    isInTempValid = false;
                }
                // check the lattice width condition
                if (isInTempValid && (inputColumn < 1 || inputColumn > nLatticeCellCol)) {
                    isInTempValid = false;
                }
            }

            isInTempValid = false;
            // try until the gamer input is correct
            while (!isInTempValid || inputSide.isEmpty()){
                System.out.println("Введите сторону (Возможные значения: в, н, л, п (верх, низ, лево, право соответственно))");

                // read the input side
                try {
                    inputSide = commandLineReader.readLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // compare with valid input options
                if (inputSide.equals("в") || inputSide.equals("н") || inputSide.equals("л") || inputSide.equals("п")){
                    isInTempValid = true;
                }
                else{
                    isInTempValid = false;
                }
            }

            // game checks the input and makes the move if input is correct
            try {
                isInputValid = game.gamerMove(Integer.parseInt(inputRowStr) - 1, Integer.parseInt(inputColumnStr) - 1, inputSide);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

            // if input is invalid
            if (!isInputValid){
                System.out.println("Некорректный ввод");
                // reset the incorrect input
                inputRowStr = "";
                inputColumnStr = "";
                inputSide = "";
            }

        } // go out when input is correct

        // get the updated game status
        try {
            latticeSides = game.getLatticeSides();
            cellColors = game.getCellColors();
            currGamerId = game.getCurrGamerId();

            printLattice();
            if (currGamerId == gamerId){
                System.out.println("Вы получили дополнительный ход");
            } else {
                System.out.println("");
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean checkGameStatus() {     // checking the status of the game

        if (currGamerId == 0) {
            System.out.println("Ничья");
            return true;
        }

        if (currGamerId < 0) {
            System.out.println("Победитель - игрок " +  Integer.toString(currGamerId*(-1)));
            if ((-1)*currGamerId == gamerId) {
                System.out.println("Вы выиграли");
            } else{
                System.out.println("Вы проиграли");
            }
            return true;
        }

        return false;
    }

    public static void main(String[] args) throws RemoteException, NotBoundException {

        final Registry registry = LocateRegistry.getRegistry(8080);
        Game game = (Game) registry.lookup(UNIQUE_NAME);
        gamerId = game.connectPlayer();
        if (gamerId == 0) throw new RuntimeException();

        // number of rows (the lattice height)
        nLatticeCellRow = game.getNumCellRows();
        // number of columns (the lattice width)
        nLatticeCellCol = game.getNumCellCols();
        // number of rows and columns for symbol matrix (— and |)
        nLatticeSymbRow = nLatticeCellRow * 2 + 1;
        nLatticeSymbCol = nLatticeCellCol * 2 + 1;

        System.out.println("Игра коридорчики");
        System.out.println("Ваш номер - " + gamerId);

        // the game begins for this player
        startGameThread(game);
    }
}
