package fragment_interface;

public interface Upgrade_fragment_interface {
//    void setUpgradeBtn(final String text, boolean checked);
    void setTextViewStr(String text);
    void setUpgradeInProgress(int progress);
    void setUpgradeBtnStatus(String btnText,boolean enable);
    void setUpgradeImageInfo(int type,String text);
    String getUpdateString();
    int getUpgradeProgress();
    int getImageType();
    String getImageText();
    void addUpgradeFilePath(String filePath);
}
