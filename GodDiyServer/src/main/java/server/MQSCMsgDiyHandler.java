package server;

import com.google.protobuf.InvalidProtocolBufferException;
import connection.activemq.MQConsumerTool;
import connection.activemq.MQMessageParser;
import connection.activemq.MQProducerTool;
import content.WorldMapContent;
import db.DiyMysqlWorker;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQMessage;
import protobuf.*;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.*;

public class MQSCMsgDiyHandler extends MsgHandler {

    Map<SCMessageDiyWorldMap.RequestGet.SortType, String> sortTypeStringMap = createSortTypeStringMap();
    private static Map<SCMessageDiyWorldMap.RequestGet.SortType, String> createSortTypeStringMap() {
        Map<SCMessageDiyWorldMap.RequestGet.SortType, String> myMap = new HashMap<>();
        myMap.put(SCMessageDiyWorldMap.RequestGet.SortType.SORT_ID, "id");
        myMap.put(SCMessageDiyWorldMap.RequestGet.SortType.SORT_TIME, "upload_time");
        myMap.put(SCMessageDiyWorldMap.RequestGet.SortType.SORT_MAP_NAME, "map_name");
        myMap.put(SCMessageDiyWorldMap.RequestGet.SortType.SORT_MAP_AUTHOR, "author_name");
        return myMap;
    }

    Map<SCMessageDiyWorldMap.RequestGet.SearchType, String> searchTypeStringMap = createSearchTypeStringMap();
    private static Map<SCMessageDiyWorldMap.RequestGet.SearchType, String> createSearchTypeStringMap() {
        Map<SCMessageDiyWorldMap.RequestGet.SearchType, String> myMap = new HashMap<>();
        myMap.put(SCMessageDiyWorldMap.RequestGet.SearchType.SEARCH_ID, "id");
        myMap.put(SCMessageDiyWorldMap.RequestGet.SearchType.SEARCH_MAP_NAME, "map_name");
        myMap.put(SCMessageDiyWorldMap.RequestGet.SearchType.SEARCH_MAP_AUTHOR, "author_name");
        return myMap;
    }

    Map<String, DiyMysqlWorker.SearchValueType> searchValueTypeMap = createSearchValueTypeMap();
    private static Map<String, DiyMysqlWorker.SearchValueType> createSearchValueTypeMap() {
        Map<String, DiyMysqlWorker.SearchValueType> myMap = new HashMap<>();
        myMap.put("id", DiyMysqlWorker.SearchValueType.INT);
        myMap.put("map_name", DiyMysqlWorker.SearchValueType.STRING);
        myMap.put("author_name", DiyMysqlWorker.SearchValueType.STRING);
        return myMap;
    }

    private volatile static MQSCMsgDiyHandler singleton;
    public static MQSCMsgDiyHandler getSingleton() {
        if (singleton == null) {
            synchronized (MQSCMsgDiyHandler.class) {
                if (singleton == null) {
                    singleton = new MQSCMsgDiyHandler();
                }
            }
        }
        return singleton;
    }

    Server server;

    public void init(Server server){
        this.server = server;
        server.logger.debug(String.format("服务器ID:%s %s 初始化", server.getServerID(), msgHandlerName));
        try {
            MQConsumerTool.getInstance().addQueueOrTopic("SCMessageRequest_Diy", 1, new MessageListener() {
                        public void onMessage(Message message) {
                            handle(message);
                        }
                    }, String.format("target_server_id='%s' or target_server_id='%s'", server.getServerID(), "anyone")
            );
            MQProducerTool.getInstance().addDestination("SCMessageResponse", 1, 1);
        }
        catch (JMSException e) {
            server.logger.error(e.toString());
        }
    }

    private MQSCMsgDiyHandler(){
        msgHandlerName = "diy信息处理器";
    }

    public void handle(Message msg) {
        if (msg instanceof ActiveMQBytesMessage) {
            ActiveMQBytesMessage bytesMsg = (ActiveMQBytesMessage) msg;

            //if (handledCommandsID.contains(bytesMsg.getCommandId())) return;

            //handledCommandsID.add(bytesMsg.getCommandId());

            SCMessage.Request request = (SCMessage.Request) MQMessageParser.parseMessageToProtobuf(bytesMsg, SCMessage.Request.parser());
            assert request != null;
            if (request.toByteString().size() < 500){
                server.logger.debug(String.format("服务器ID:%s %s 处理消息：%s", server.getServerID(), msgHandlerName, request.toString()));
            }
            server.logger.debug(String.format("服务器ID:%s %s 处理消息长度：%d", server.getServerID(), msgHandlerName, request.toByteString().size()));
            SCMessageDiy.Request requestDiy = null;
            try {
                requestDiy = SCMessageDiy.Request.parseFrom(request.getContent());
            } catch (InvalidProtocolBufferException e) {
                server.logger.error(String.valueOf(e));
            }

            SCMessageDiy.Response.Builder responseBuilder2 = SCMessageDiy.Response.newBuilder();
            SCMessage.Response.Builder responseBuilder1 = SCMessage.Response.newBuilder();

            String userName = requestDiy.getRequestDiyWorldMap().getUserName();

            switch (requestDiy.getDataType()){
                case DIY_WORLD_MAP:
                    responseBuilder2.setDataType(SCMessageDiy.Response.ResponseType.DIY_WORLD_MAP);

                    SCMessageDiy.ResponseDiyWorldMap.Builder responseBuilder3 = SCMessageDiy.ResponseDiyWorldMap.newBuilder();
                    responseBuilder3.setUserName(userName);

                    SCMessageDiyWorldMap.Request requestDiyWorldMap = null;
                    try {
                        requestDiyWorldMap = SCMessageDiyWorldMap.Request.parseFrom(requestDiy.getRequestDiyWorldMap().getContent());
                    } catch (InvalidProtocolBufferException e) {
                        server.logger.error(String.valueOf(e));
                    }

                    assert requestDiyWorldMap != null;

                    SCMessageDiyWorldMap.Response.Builder responseBuilder4 = SCMessageDiyWorldMap.Response.newBuilder();

                    switch (requestDiyWorldMap.getDataType()){
                        case SAVE:
                            responseBuilder4.setDataType(SCMessageDiyWorldMap.Response.ResponseType.SAVE);
                            SCMessageDiyWorldMap.ResponseSave.Builder responseSaveBuilder = SCMessageDiyWorldMap.ResponseSave.newBuilder();
                            SCMessageDiyWorldMap.RequestSave requestSave = requestDiyWorldMap.getRequestSave();
                            int saveResult = DiyMysqlWorker.getSingleton().saveDiyWorldMap(
                                    userName,
                                    requestSave.getMapName(),
                                    requestSave.getMapData(),
                                    requestSave.getMapInfo(),
                                    requestSave.getMapImageData()
                                    );
                            if (saveResult <= 0){
                                server.logger.debug(String.format("保存地图错误，错误代码：%s", saveResult));
                                responseSaveBuilder.setResult(false).setWords(String.valueOf(saveResult));
                            }
                            else{
                                server.logger.debug(String.format("保存地图成功，地图id：%d", saveResult));

                                DiyMysqlWorker.PageDiyWorldMapGetResult getResult = DiyMysqlWorker.getSingleton().getPageDiyWorldMapSortedSearched(
                                        userName,
                                        0,
                                        1,
                                        true,
                                        "id",
                                        "id",
                                        String.valueOf(saveResult)
                                );

                                Vector<WorldMapContent> worldMapContents = getResult.worldMapContents;
                                int mapsCount = getResult.count;
                                if (worldMapContents.size() > 0){

                                    SCMessageDiyWorldMap.MapContent.Builder mapContentBuilder = SCMessageDiyWorldMap.MapContent.newBuilder();

                                    Iterator iter = worldMapContents.iterator();
                                    WorldMapContent worldMapContent = (WorldMapContent) iter.next();
                                    mapContentBuilder.setMapData(worldMapContent.mapData);
                                    mapContentBuilder.setMapId(worldMapContent.id);
                                    mapContentBuilder.setMapName(worldMapContent.mapName);
                                    mapContentBuilder.setMapAuthor(worldMapContent.authorName);
                                    mapContentBuilder.setMapInfo(worldMapContent.mapInfo);
                                    mapContentBuilder.setUploadTime(worldMapContent.dateToString(worldMapContent.uploadDate));

                                    responseSaveBuilder.setResult(true).setWords(String.valueOf(saveResult))
                                            .setMapContent(mapContentBuilder.build());
                                }
                                else{
                                    server.logger.debug(String.format("保存地图错误，错误代码：%s", saveResult));
                                    responseSaveBuilder.setResult(false).setWords(String.valueOf(saveResult));
                                }
                            }
                            responseBuilder4.setResponseSave(responseSaveBuilder.build());
                            responseBuilder3.setContent(responseBuilder4.build().toByteString());
                            responseBuilder2.setResponseDiyWorldMap(responseBuilder3);
                            break;
                        case LOAD:
                            responseBuilder4.setDataType(SCMessageDiyWorldMap.Response.ResponseType.LOAD);
                            SCMessageDiyWorldMap.ResponseLoad.Builder responseLoadBuilder = SCMessageDiyWorldMap.ResponseLoad.newBuilder();

                            SCMessageDiyWorldMap.RequestLoad requestLoad = requestDiyWorldMap.getRequestLoad();
                            WorldMapContent loadResult = DiyMysqlWorker.getSingleton().loadDiyWorldMap(
                                    userName,
                                    requestLoad.getMapId()
                            );
                            if (!loadResult.isValid()){
                                server.logger.debug(String.format("读取地图错误，错误代码：%d", loadResult.id));
                                responseLoadBuilder.setResult(false).setWords(String.valueOf(loadResult.id));
                            }
                            else{
                                server.logger.debug(String.format("读取地图成功，地图id：%d", loadResult.id));

                                responseLoadBuilder.setResult(true).setWords(String.valueOf(loadResult.id))
                                .setMapContent(loadResult.toMapContentBuilder().build());
                            }
                            responseBuilder4.setResponseLoad(responseLoadBuilder.build());
                            responseBuilder3.setContent(responseBuilder4.build().toByteString());
                            responseBuilder2.setResponseDiyWorldMap(responseBuilder3);
                            break;
                        case GET:
                            responseBuilder4.setDataType(SCMessageDiyWorldMap.Response.ResponseType.GET);
                            SCMessageDiyWorldMap.ResponseGet.Builder responseGetBuilder = SCMessageDiyWorldMap.ResponseGet.newBuilder();

                            SCMessageDiyWorldMap.RequestGet requestGet = requestDiyWorldMap.getRequestGet();

                            Vector<WorldMapContent> getListResult = null;
                            DiyMysqlWorker.PageDiyWorldMapGetResult getResult;

                            SCMessageDiyWorldMap.RequestGet.SearchType searchType = requestGet.getSearchType();
                            SCMessageDiyWorldMap.RequestGet.SortType sortType = requestGet.getSortType();

                            String searchTypeString = searchTypeStringMap.get(searchType);
                            String sortTypeString = sortTypeStringMap.get(sortType);

                            if (requestGet.getSearchType() == SCMessageDiyWorldMap.RequestGet.SearchType.SEARCH_NULL || requestGet.getSearchValue().equals("")){
                                getResult = DiyMysqlWorker.getSingleton().getPageDiyWorldMapSorted(
                                        userName,
                                        requestGet.getPageIdx(),
                                        requestGet.getPageSize(),
                                        requestGet.getAscending(),
                                        sortTypeString
                                );

                            }
                            else{
                                getResult = DiyMysqlWorker.getSingleton().getPageDiyWorldMapSortedSearched(
                                        userName,
                                        requestGet.getPageIdx(),
                                        requestGet.getPageSize(),
                                        requestGet.getAscending(),
                                        sortTypeString,
                                        searchTypeString,
                                        requestGet.getSearchValue()
                                );
                            }

                            Vector<WorldMapContent> worldMapContents = getResult.worldMapContents;
                            int mapsCount = getResult.count;

                            Iterator iter = worldMapContents.iterator();
                            while (iter.hasNext()) {
                                WorldMapContent worldMapContent = (WorldMapContent) iter.next();
                                if (worldMapContent.isValid()) {
                                    //server.logger.debug(worldMapContent.dateToString(worldMapContent.uploadDate));
                                    responseGetBuilder.addMapContents(worldMapContent.toMapContentBuilder().build());
                                }
                            }

                            server.logger.debug(String.format("获取到地图 %d 个", worldMapContents.size()));

                            responseGetBuilder.setResult(true).setWords(String.valueOf(worldMapContents.size()))
                                    .setSortTypeValue(requestGet.getSortTypeValue()).setAscending(requestGet.getAscending())
                                    .setPageIdx(requestGet.getPageIdx()).setPageSize(requestGet.getPageSize())
                                    .setSearchTypeValue(requestGet.getSearchTypeValue()).setSortTypeValue(requestGet.getSortTypeValue())
                                    .setSearchValue(requestGet.getSearchValue()).setOnlyMapsCount(requestGet.getOnlyMapsCount())
                                    .setMapsCount(mapsCount);

                            responseBuilder4.setResponseGet(responseGetBuilder.build());
                            responseBuilder3.setContent(responseBuilder4.build().toByteString());
                            responseBuilder2.setResponseDiyWorldMap(responseBuilder3);
                            break;
                        case MAPS_COUNT:
//                            responseBuilder4.setDataType(SCMessageDiyWorldMap.Response.ResponseType.MAPS_COUNT);
//                            SCMessageDiyWorldMap.ResponseMapsCount.Builder responseMapsCountBuilder = SCMessageDiyWorldMap.ResponseMapsCount.newBuilder();
//
//                            SCMessageDiyWorldMap.RequestMapsCount requestMapsCount = requestDiyWorldMap.getRequestMapsCount();
//
//                            int mapsCount = DiyMysqlWorker.getSingleton().getDiyWorldMapsCount(userName);
//
//                            responseMapsCountBuilder.setCount(mapsCount);
//
//                            responseBuilder4.setResponseMapsCount(responseMapsCountBuilder.build());
//                            responseBuilder3.setContent(responseBuilder4.build().toByteString());
//                            responseBuilder2.setResponseDiyWorldMap(responseBuilder3);


                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + requestDiy.getDataType());

                    }


            }

            responseBuilder1.setContent(responseBuilder2.build().toByteString());
            responseBuilder1.setType(SCMessage.Type.DIY);
            responseBuilder1.setClientId(request.getClientId());
            responseBuilder1.setGateServerId(request.getGateServerId());
            responseBuilder1.addAllPassedServersId(request.getPassedServersIdList());
            responseBuilder1.addPassedServersId(server.getServerID());
            responseBuilder1.setRequestId(request.getRequestId());

            try {
                MQProducerTool.getInstance().sendBuilder("SCMessageResponse", responseBuilder1, "target_server_id", request.getGateServerId());
            } catch (Exception e) {
                server.logger.error(e.toString());
            }

        } else {
            server.logger.debug("Consumer received:" + msg.toString());
        }
    }

}
