package corridors;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Game extends Remote {

    String[][] getLatticeSides() throws RemoteException;
    String[][] getCellColors() throws RemoteException;
    boolean gamerMove(int cellRowIdx, int cellColIdx, String cellSide) throws RemoteException;
    int connectPlayer() throws RemoteException;
    int getCurrGamerId() throws RemoteException;
    int getNumCellRows() throws RemoteException;
    int getNumCellCols() throws RemoteException;
}
