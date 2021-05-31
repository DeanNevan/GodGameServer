package db;



import connection.mysql.MysqlConnection;
import content.WorldMapContent;
import protobuf.SCMessageDiyWorldMap;

import java.sql.*;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class DiyMysqlWorker {

    private volatile static DiyMysqlWorker singleton;
    public static DiyMysqlWorker getSingleton() {
        if (singleton == null) {
            synchronized (DiyMysqlWorker.class) {
                if (singleton == null) {
                    singleton = new DiyMysqlWorker();
                }
            }
        }
        return singleton;
    }

    MysqlConnection mysqlConnection;
    Statement statement;

    private DiyMysqlWorker(){
        mysqlConnection = MysqlConnection.getSingleton();
        try {
            statement = mysqlConnection.connection.createStatement();
            statement.executeUpdate("USE diy_db");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public int saveDiyWorldMap(String userName, String mapName, String mapData, String mapInfo){
        int result = -1;
        try {

            //String sql = "select save_diy_world_map(?, ?, ?, ?)";
            String sql = "INSERT INTO world_map (author_name, map_name, map_data, upload_time, map_info) VALUES (?, ?, ?, CURRENT_TIME(), ?)";

            //;

            PreparedStatement pstat = mysqlConnection.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstat.setString(1, userName);
            pstat.setString(2, mapName);
            pstat.setString(3, mapData);
            pstat.setString(4, mapInfo);
            pstat.executeUpdate();

            ResultSet rs = pstat.getGeneratedKeys();

            if (rs.next()){
                result = rs.getInt(1);
            }
            //完成后关闭
            pstat.close();
            rs.close();

        } catch (SQLException e){
            System.err.println(e);
        }
        return result;
    }


    public WorldMapContent loadDiyWorldMap(String userName, int targetMapId){
        WorldMapContent worldMapContent = new WorldMapContent(-1);
        int result = -1;
        try {
            MysqlConnection db = MysqlConnection.getSingleton();
            String sql = "select author_name, map_name, map_data, upload_time, map_info from world_map where id = (?) limit 1";

            PreparedStatement pstat = db.connection.prepareStatement(sql);
            pstat.setInt(1, targetMapId);

            ResultSet rs = pstat.executeQuery();

            // 展开结果集数据库
            while(rs.next()){
                String authorName = rs.getString("author_name");
                String mapName = rs.getString("map_name");
                String mapData = rs.getString("map_data");
                String mapInfo = rs.getString("map_info");
                Date uploadDate = rs.getDate("upload_time");
                worldMapContent.mapName = mapName;
                worldMapContent.authorName = authorName;
                worldMapContent.mapData = mapData;
                worldMapContent.mapInfo = mapInfo;
                worldMapContent.uploadDate = uploadDate;
                worldMapContent.id = targetMapId;
            }
            //完成后关闭
            pstat.close();
            rs.close();

        } catch (SQLException e){
            System.err.println(e);
        }
        return worldMapContent;
    }

    public class PageDiyWorldMapGetResult{
        public int count = 0;
        public Vector<WorldMapContent> worldMapContents = new Vector<>();
        public void addMapContent(WorldMapContent worldMapContent){
            worldMapContents.add(worldMapContent);
        }
    }

    public PageDiyWorldMapGetResult getPageDiyWorldMapSorted(String userName, int pageIdx, int pageSize, boolean asc, String orderBy) {
        PageDiyWorldMapGetResult pageDiyWorldMapGetResult = new PageDiyWorldMapGetResult();
        int result = -1;
        try {
            MysqlConnection db = MysqlConnection.getSingleton();
            String sql = "select id, map_name, author_name, upload_time, map_info from world_map order by %s %s limit ?, ?";
            sql = String.format(sql, orderBy, asc ? "ASC" : "DESC");

            PreparedStatement pstat = db.connection.prepareStatement(sql);
            pstat.setInt(1, pageIdx * pageSize);
            pstat.setInt(2, pageSize);

            ResultSet rs = pstat.executeQuery();

            // 展开结果集数据库
            while (rs.next()) {
                String mapName = rs.getString("map_name");
                String authorName = rs.getString("author_name");
                String mapInfo = rs.getString("map_info");
                Timestamp uploadTime = rs.getTimestamp("upload_time");
                int mapId = rs.getInt("id");

                WorldMapContent worldMapContent = new WorldMapContent(mapId);
                worldMapContent.mapName = mapName;
                worldMapContent.authorName = authorName;
                worldMapContent.mapInfo = mapInfo;
                worldMapContent.uploadDate = new java.util.Date(uploadTime.getTime());
                pageDiyWorldMapGetResult.addMapContent(worldMapContent);
            }

            sql = "select count(*) from world_map";
            pstat = db.connection.prepareStatement(sql);
            rs = pstat.executeQuery();
            int count = 0;
            if (rs.next()) count = rs.getInt(1);

            pageDiyWorldMapGetResult.count = count;

            //完成后关闭
            pstat.close();
            rs.close();

        } catch (SQLException e) {
            System.err.println(e);
        }
        return pageDiyWorldMapGetResult;
    }

    public enum SearchValueType{
        STRING, INT
    }
    public PageDiyWorldMapGetResult getPageDiyWorldMapSortedSearched(String userName, int pageIdx, int pageSize, boolean asc, String orderBy,
                                                                    String searchName, Object searchValue) {
        PageDiyWorldMapGetResult pageDiyWorldMapGetResult = new PageDiyWorldMapGetResult();
        try {
            MysqlConnection db = MysqlConnection.getSingleton();
            String sql = "select id, map_name, author_name, upload_time, map_info from world_map where %s = ? order by %s %s limit ?, ?";
            sql = String.format(sql, searchName, orderBy, asc ? "ASC" : "DESC");

            PreparedStatement pstat = db.connection.prepareStatement(sql);
            pstat.setString(1, (String) searchValue);
            pstat.setInt(2, pageIdx * pageSize);
            pstat.setInt(3, pageSize);

            ResultSet rs = pstat.executeQuery();


            // 展开结果集数据库
            while (rs.next()) {
                int mapId = rs.getInt("id");
                String mapName = rs.getString("map_name");
                String authorName = rs.getString("author_name");
                String mapInfo = rs.getString("map_info");
                //Date uploadDate = rs.getDate("upload_time");
                Timestamp uploadTime = rs.getTimestamp("upload_time");

                WorldMapContent worldMapContent = new WorldMapContent(mapId);
                worldMapContent.mapName = mapName;
                worldMapContent.authorName = authorName;
                worldMapContent.mapInfo = mapInfo;
                worldMapContent.uploadDate = new java.util.Date(uploadTime.getTime());
                pageDiyWorldMapGetResult.addMapContent(worldMapContent);
            }

            sql = "select count(*) from world_map where %s = ?";
            sql = String.format(sql, searchName);
            pstat = db.connection.prepareStatement(sql);
            pstat.setString(1, (String) searchValue);
            rs = pstat.executeQuery();
            int count = 0;
            if (rs.next()) count = rs.getInt(1);

            pageDiyWorldMapGetResult.count = count;


            //完成后关闭
            pstat.close();
            rs.close();

        } catch (SQLException e) {
            System.err.println(e);
        }
        return pageDiyWorldMapGetResult;
    }

    public int getDiyWorldMapsCount(String userName) {
        int mapsCount = -1;
        int result = -1;
        try {
            MysqlConnection db = MysqlConnection.getSingleton();
            String sql = "select count(*) from world_map";

            PreparedStatement pstat = db.connection.prepareStatement(sql);

            ResultSet rs = pstat.executeQuery();


            // 展开结果集数据库
            while (rs.next()) {
                mapsCount = rs.getInt(1);

            }
            //完成后关闭
            pstat.close();
            rs.close();

        } catch (SQLException e) {
            System.err.println(e);
        }
        return mapsCount;
    }

}
