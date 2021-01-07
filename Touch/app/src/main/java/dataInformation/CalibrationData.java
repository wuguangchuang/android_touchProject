package dataInformation;

public class CalibrationData {
    public int number;
    public int targetX;
    public int targetY;
    public int collectX;
    public int collectY;
    public int maxX;
    public int maxY;

    public CalibrationData(){}
    public CalibrationData(int num,int tarX,int tarY,int collX,int collY,int maxX,int maxY)
    {
        this.number = num;
        this.targetX = tarX;
        this.targetY = tarY;
        this.collectX = collX;
        this.collectY = collY;
        this.maxX = maxX;
        this.maxY = maxY;
    }
}
