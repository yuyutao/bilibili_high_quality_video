package cn.autoeditor.sharelibrary;

public class PartInfo {

    public PartInfo(String cid){
        this.cid = cid ;
    }
    public PartInfo(){}
    public String bvid ;
    public String cid ;
    public String title ;
    public long timestamp = 0 ;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartInfo partInfo = (PartInfo) o;
        return bvid.equals(partInfo.bvid) && cid.equals(partInfo.cid);
    }
}
