package fragment_interface;

import java.util.List;
import java.util.Map;

import dataInformation.CalibrationData;

public interface Setting_fragment_interface {
    void setEnable(boolean enable);
    void refreshSettings();
    void refreshAllUiInfo(Map<String,Byte> map, List<CalibrationData> dataList);
    void refreshCalibrationData(int where);
}
