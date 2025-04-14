package corridors;

import corridors.service.RemoteGameServer;
import corridors.service.RemoteGameServerService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class Client {
    public static final String url = "http://localhost:8080/Server?wsdl";
    public static List<String> latticeSides;
    public static List<Integer> cellColors;
    public static List<String> gamerColors;
    static int gamerId = 0;
    static int currGamerId = 0;
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_WHITE = "\u001B[37m";
    private static BufferedReader commandLineReader;
    private static int nLatticeCellRow = 0;   // number of rows (the lattice height)
    private static int nLatticeCellCol = 0;   // number of columns (the lattice width)
    private static int nLatticeSymbRow = 0;   // кол-во символов в строке
    private static int nLatticeSymbCol = 0;
    private static void printLattice() {
        StringBuilder s = new StringBuilder();
        for (int symbRowIdx = 0; symbRowIdx < nLatticeSymbRow; symbRowIdx++) {
            for (int symbColIdx = 0; symbColIdx < nLatticeSymbCol; symbColIdx++) {
                if (cellColors.get(symbRowIdx*nLatticeSymbCol+symbColIdx)==0)
                    s.append(ANSI_WHITE);
                else if (cellColors.get(symbRowIdx*nLatticeSymbCol+symbColIdx)==1) {
                    s.append(gamerColors.get(0));
                }
                else if (cellColors.get(symbRowIdx*nLatticeSymbCol+symbColIdx)==2) {
                    s.append(gamerColors.get(1));
                }
                s.append(latticeSides.get((symbRowIdx*nLatticeSymbCol+symbColIdx))).append(ANSI_RESET);
            }
            System.out.println(s);
            s.setLength(0);
        }
    }

    private static void gamerMove(RemoteGameServer RemoteGameServerProxy) {

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
                isInputValid = RemoteGameServerProxy.gamerMove(Integer.parseInt(inputRowStr) - 1, Integer.parseInt(inputColumnStr) - 1, inputSide);

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
        latticeSides = RemoteGameServerProxy.getLatticeSides();
        cellColors = RemoteGameServerProxy.getCellColors();
        currGamerId = RemoteGameServerProxy.getCurrGamerId();

        printLattice();
        if (currGamerId == gamerId){
            System.out.println("Вы получили дополнительный ход");
        } else {
            System.out.println("");
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

    private static void startGameThread(RemoteGameServer RemoteGameServerProxy) {
        Thread gameThread = new Thread(() -> {
            commandLineReader = new BufferedReader(new InputStreamReader(System.in));

            while(true) {
                // update the variables from server every 500 ms
                // call remote methods
                latticeSides = RemoteGameServerProxy.getLatticeSides();  // get the lattice from the server
                cellColors = RemoteGameServerProxy.getCellColors();      // get the colors of sides from the server
                currGamerId = RemoteGameServerProxy.getCurrGamerId();    // get the id of current gamer from the server

                if (gamerId == currGamerId) {
                    gamerMove(RemoteGameServerProxy);            // the current gamer makes a move
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

    public static void main(String[] args) throws MalformedURLException {
        RemoteGameServerService webservice = new RemoteGameServerService(new URL(url));
        RemoteGameServer RemoteGameServerProxy = webservice.getRemoteGameServerPort();  // creating a client proxy

        gamerId = RemoteGameServerProxy.connectPlayer();
        if (gamerId == 0) throw new RuntimeException();

        // number of rows (the lattice height)
        nLatticeCellRow = RemoteGameServerProxy.getNumCellRows();
        // number of columns (the lattice width)
        nLatticeCellCol = RemoteGameServerProxy.getNumCellCols();
        // number of rows and columns for symbol matrix (— and |)
        nLatticeSymbRow = nLatticeCellRow * 2 + 1;
        nLatticeSymbCol = nLatticeCellCol * 2 + 1;

        gamerColors = new ArrayList<String>();
        gamerColors.add(ANSI_BLUE);   // color gamer 1
        gamerColors.add(ANSI_PURPLE); // color gamer 2

        System.out.println("Игра коридорчики");
        System.out.println("Ваш номер - " + gamerId);

        // the game begins for this player
        startGameThread(RemoteGameServerProxy);


    }
}
