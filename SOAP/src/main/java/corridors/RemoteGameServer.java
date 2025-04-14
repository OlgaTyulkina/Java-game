package corridors;

import javax.jws.WebMethod;
import javax.jws.WebService;
import java.util.ArrayList;
import java.util.List;

@WebService
public class RemoteGameServer  {
    List<String> latticeSides;
    List<Integer> cellColors;
    List<String> gamerColors;
    Boolean[][] isColored;
    Integer[][] sideIdx2BoundedGamerId;
    Integer[][] cellIdx2BoundedGamerId;
    Integer[][][][] cellToSidesMap;
    Integer[] nCellsBoundedByGamer;

    int gamersCount;
    int currGamerId;
    int nCells;

    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_WHITE = "\u001B[37m";
    private final int nLatticeCellRow = 2;   // number of rows (the lattice height)
    private final int nLatticeCellCol = 3;   // number of columns (the lattice width)
    private static int nLatticeSymbRow = 0;
    private static int nLatticeSymbCol = 0;

    public RemoteGameServer() {    // works when an object on Server is created

        // character height and width of the lattice
        nLatticeSymbRow = nLatticeCellRow * 2 + 1;
        nLatticeSymbCol = nLatticeCellCol * 2 + 1;
        // lattice size (in cells)
        nCells = nLatticeCellRow * nLatticeCellCol;
        gamersCount = 0;
        // sides of the lattice
        latticeSides = new ArrayList<String>();
        // colors of the sides
        cellColors = new ArrayList<Integer>();

        isColored = new Boolean[nLatticeSymbRow][nLatticeSymbCol];
        // which side belongs to which player
        sideIdx2BoundedGamerId = new Integer[nLatticeSymbRow][nLatticeSymbCol];
        gamerColors = new ArrayList<String>();
        // choose the colors for gamers
        gamerColors.add(ANSI_BLUE);   // color gamer 1
        gamerColors.add(ANSI_PURPLE); // color gamer 2

        cellToSidesMap = new Integer[nLatticeCellRow][nLatticeCellCol][4][2];
        // which cell belongs to which player
        cellIdx2BoundedGamerId = new Integer[nLatticeCellRow][nLatticeCellCol];

        nCellsBoundedByGamer = new Integer[2];
        nCellsBoundedByGamer[0] = 0;  // count of the cells belongs to gamer 1
        nCellsBoundedByGamer[1] = 0;  // count of the cells belongs to gamer 2

        for (int cellRowIdx = 0; cellRowIdx < nLatticeCellRow; cellRowIdx++) {
            for (int cellColIdx = 0; cellColIdx < nLatticeCellCol; cellColIdx++) {
                cellIdx2BoundedGamerId[cellRowIdx][cellColIdx] = 0;
                // upper side (0th side)
                cellToSidesMap[cellRowIdx][cellColIdx][0][0] = cellRowIdx * 2; // 0 - row index
                cellToSidesMap[cellRowIdx][cellColIdx][0][1] = cellColIdx * 2 + 1; // 1 - columns index
                // bottom side (1th side)
                cellToSidesMap[cellRowIdx][cellColIdx][1][0] = cellRowIdx * 2 + 2; // 0 - row index
                cellToSidesMap[cellRowIdx][cellColIdx][1][1] = cellColIdx * 2 + 1; // 1 - columns index
                // left side (2th side)
                cellToSidesMap[cellRowIdx][cellColIdx][2][0] = cellRowIdx * 2 + 1; // 0 - row index
                cellToSidesMap[cellRowIdx][cellColIdx][2][1] = cellColIdx * 2; // 1 - columns index
                // right side (3th side)
                cellToSidesMap[cellRowIdx][cellColIdx][3][0] = cellRowIdx * 2 + 1; // 0 - row index
                cellToSidesMap[cellRowIdx][cellColIdx][3][1] = cellColIdx * 2 + 2; // 1 - columns index
            }
        }

        // gamer with ID=1 starts the game
        currGamerId = 1;

        // initial state of lattice
        // create the symbol matrix (— and |)
        for (int i = 0; i < nLatticeSymbRow; i++) {
            for (int j = 0; j < nLatticeSymbCol; j++) {
                sideIdx2BoundedGamerId[i][j] = 0;    // all sides belong to nobody
                cellColors.add(0); // all sides are white (belong to nobody)
                isColored[i][j] = false;             // all sides marked as not colored (belong to nobody)

                if (i % 2 == 0) { // symbol matrix rows with up and down sides
                    latticeSides.add(j % 2 == 0 ? " " : "————");
                }
                if (i % 2 == 1) { // symbol matrix rows with left and right sides
                    latticeSides.add(j % 2 == 0 ? "|   " : " ");
                }
            }
        }
    }
    @WebMethod
    public int connectPlayer()  {
        if (gamersCount < 2) {
            gamersCount++;
            System.out.println("Client " + gamersCount + " connected");
            return gamersCount;
        }
        System.out.println("Error: A two-player game is supported only");
        return 0;
    }

    @WebMethod
    public boolean gamerMove(int cellRowIdx, int cellColIdx, String cellSide) {

        int sideRowIdx = 0, sideColIdx = 0;

        // get the row and columns indexes in symbols matrix using precalculated map
        if (cellSide.equals("в")){
            sideRowIdx = cellToSidesMap[cellRowIdx][cellColIdx][0][0];
            sideColIdx = cellToSidesMap[cellRowIdx][cellColIdx][0][1];
        }
        else if (cellSide.equals("н")){
            sideRowIdx = cellToSidesMap[cellRowIdx][cellColIdx][1][0];
            sideColIdx = cellToSidesMap[cellRowIdx][cellColIdx][1][1];
        }
        else if (cellSide.equals("л")){
            sideRowIdx = cellToSidesMap[cellRowIdx][cellColIdx][2][0];
            sideColIdx = cellToSidesMap[cellRowIdx][cellColIdx][2][1];
        }
        else if (cellSide.equals("п")){
            sideRowIdx = cellToSidesMap[cellRowIdx][cellColIdx][3][0];
            sideColIdx = cellToSidesMap[cellRowIdx][cellColIdx][3][1];
        }
        // checking if side is already colored
        if (isColored[sideRowIdx][sideColIdx]){
            return false;
        }

        // color the side
        cellColors.set(sideRowIdx*nLatticeSymbCol+sideColIdx,currGamerId);
        // mark side as colored
        isColored[sideRowIdx][sideColIdx] = true;
        // assign the gamer ID which will own the side
        sideIdx2BoundedGamerId[sideRowIdx][sideColIdx] = currGamerId;
        // update the status of game (check is some new cells are become fully colored)
        updateGameState();

        return true;
    }

    private void updateGameState() {
        int nSidesColored, sideRowIdx, sideColIdx;
        boolean isNewCellBounded = false;
        // check is some new cells are become fully colored
        for (int cellRowIdx = 0; cellRowIdx < nLatticeCellRow; cellRowIdx++) {
            for (int cellColIdx = 0; cellColIdx < nLatticeCellCol; cellColIdx++) {
                // if the cell doesn't belong to anyone (wasn't fully colored)
                if (cellIdx2BoundedGamerId[cellRowIdx][cellColIdx] == 0){
                    // count the current number of colored sides
                    nSidesColored = 0;
                    for (int sideIdx = 0; sideIdx < 4; sideIdx++) {
                        sideRowIdx = cellToSidesMap[cellRowIdx][cellColIdx][sideIdx][0];
                        sideColIdx = cellToSidesMap[cellRowIdx][cellColIdx][sideIdx][1];
                        // update count of colored sides of the cell
                        if (isColored[sideRowIdx][sideColIdx]) {
                            nSidesColored++;
                        }
                    }
                    // if all sides of the cell are colored
                    if (nSidesColored == 4) {
                        // the colored cell belongs to current gamer
                        cellIdx2BoundedGamerId[cellRowIdx][cellColIdx] = currGamerId;
                        // update the number of cells owned by the current gamer
                        nCellsBoundedByGamer[currGamerId-1]++;
                        isNewCellBounded = true; // is used to give additional turn for the current gamer
                        // update the colors for the newly bounded cell according to the current gamer color
                        for (int sideIdx = 0; sideIdx < 4; sideIdx++) {
                            sideRowIdx = cellToSidesMap[cellRowIdx][cellColIdx][sideIdx][0];
                            sideColIdx = cellToSidesMap[cellRowIdx][cellColIdx][sideIdx][1];

                            cellColors.set(sideRowIdx*nLatticeSymbCol+sideColIdx,currGamerId);
                        }
                    }
                }
            }
        }

        // check who makes the next move
        // if no one cell is bounded by the last move, then change the gamer ID
        // otherwise, current gamer will make additional move
        if (!isNewCellBounded) {
            currGamerId = currGamerId == 1 ? 2 : 1;
        }
        // check whether the current gamer is won due to last move (even is some sides are not still colored)
        if (nCellsBoundedByGamer[currGamerId-1] > nCells/2){
            currGamerId = -currGamerId;
            return;
        }
        // check whether game ended in a tie (no remaining uncolored sides and same number of bounded cells by gamers)
        if (nCellsBoundedByGamer[currGamerId-1] == nCells/2 && nCellsBoundedByGamer[(currGamerId==1?2:1)-1] == nCells/2){
            currGamerId = 0;
        }
    }

    @WebMethod
    public int getCurrGamerId() {
        return this.currGamerId;
    }

    @WebMethod
    public int getNumCellRows() {
        return this.nLatticeCellRow;
    }

    @WebMethod
    public int getNumCellCols()  {
        return this.nLatticeCellCol;
    }

    @WebMethod
    public List<String> getLatticeSides()  { // it is needed to transfer the lattice to the client
        return this.latticeSides;
    }

    @WebMethod
    public List<Integer> getCellColors()  {
        return this.cellColors;
    }
}
