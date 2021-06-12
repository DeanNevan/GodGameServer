package content;

import protobuf.SCMessageDiyWorldMap;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WorldMapContent {
    public int id = -1;

    public String mapName = "";
    public String authorName = "";
    public String mapData = "";
    public String mapInfo = "";
    public Date uploadDate = new Date();
    public String mapImageData = "";

    public WorldMapContent(int id){
        this.id = id;
    }

    public boolean isValid(){
        if (id == -1) return false;
        return true;
    }

    public String dateToString(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return sdf.format(date);
    }

    public SCMessageDiyWorldMap.MapContent.Builder toMapContentBuilder(){
        SCMessageDiyWorldMap.MapContent.Builder mapContentBuilder = SCMessageDiyWorldMap.MapContent.newBuilder();
        mapContentBuilder
                .setMapId(this.id)
                .setMapAuthor(this.authorName)
                .setMapName(this.mapName)
                .setUploadTime(this.dateToString(this.uploadDate))
                .setMapData(mapData)
                .setMapInfo(mapInfo)
                .setMapImageData(mapImageData);
        return mapContentBuilder;
    }
}
