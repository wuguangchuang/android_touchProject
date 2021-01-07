package fragment_interface;

import dataInformation.TouchTestData;

public interface Signal_fragment_interface {
    void refreshItems(boolean force);
    void startSignalChart(boolean force);
    void upgradeSignaldata(TouchTestData listSignalData);
    void restoreCoords();
    void clearSignaldata();
}
