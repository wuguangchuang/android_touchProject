package dataInformation;

public class ArrayAndClassConversion {
    public static void calibrationSettingsToArray(CalibrationSettings calibrationSettings,byte[] data){
        if(data == null || calibrationSettings == null)
        {
            return;
        }
        data[0] = calibrationSettings.mode;
        data[1] = calibrationSettings.pointCount;
        data[2] = calibrationSettings.defMode;
        data[3] = calibrationSettings.defPointCount;
    }
    public static void arrayToCalibrationSettings(byte[] data,CalibrationSettings calibrationSettings){
        if(data == null || calibrationSettings == null)
        {
            return;
        }
        calibrationSettings.mode = data[0];
        calibrationSettings.pointCount = data[1];
        calibrationSettings.defMode = data[2];
        calibrationSettings.defPointCount = data[3];
    }
}
