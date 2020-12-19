package org.k.porn;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.k.porn.model.Story;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
public class ExportJson {
    public static Connection con = null;
    public static SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

    public static void getConnection() {
        log.info("Get BDD connection");
        String url = "jdbc:postgresql://localhost/literotica";
        Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "admin");
        //props.setProperty("ssl","true");
        try {
            Connection connection = DriverManager.getConnection(url, props);
            con = connection;
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT VERSION()");
            if (rs.next()) {
                System.out.println(rs.getString(1));
            }

        } catch (SQLException ex) {

            Logger lgr = Logger.getLogger(Main.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    public static void main(String[] args) throws SQLException, IOException {
        getConnection();
        Statement st = con.createStatement();
        List<String> listType = getMapType();
        Map<String, List<String>> mapTag = getMapTag();
        Map<String, String> mapAuthor = getMapAuthor();
        for (String typeGlobale: listType) {
            if(typeGlobale == null){
                continue;
            }
            ResultSet rs = st.executeQuery("select title, description, date, author, type, note , key" +
                    " from story where type='"+typeGlobale+"' order by author, key --limit 10000");
            List<Story> list = new ArrayList<>();
            while (rs.next()) {
                Story s = new Story();
                s.setTitle(rs.getString("title"));
                s.setAuthor(rs.getString("author"));
                if(mapAuthor.containsKey(s.getAuthor())){
                    s.setIdAuthor( mapAuthor.get(s.getAuthor()));
                }

                s.setKey(rs.getString("key"));
                s.setDesc(rs.getString("description"));
                s.setType(rs.getString("type"));
                Date date = convertUtilFromSql(rs.getDate("date"));
                if(date != null) {
                    s.setDate(formatter.format(date));
                }
                s.setNote(rs.getString("note"));
                if(mapTag.containsKey(s.getKey())){
                    s.getTags().addAll(mapTag.get(s.getKey()));
                }

                list.add(s);
            }
            ObjectMapper mapper = new ObjectMapper();
            log.info(typeGlobale.replace("/", " "));
            mapper.writeValue(new File("test/json/"+typeGlobale.replace("/", " ")+".json"),list);
        }
    }

    private static List<String> getMapType() throws SQLException {
        Statement stTag = con.createStatement();
        ResultSet rsTag = stTag.executeQuery("select distinct type from story ");
        List<String> toReturn = new ArrayList<>();
        while (rsTag.next()) {
            String type = rsTag.getString("type");
            toReturn.add(type);
        }
        return toReturn;
    }
    private static Map<String, String> getMapAuthor() throws SQLException {
        Statement stTag = con.createStatement();
        ResultSet rsTag = stTag.executeQuery("select id, name from author ");
        Map<String,String> mapTag = new HashMap<>();
        while (rsTag.next()) {
            String key = rsTag.getString("id");
            String name = rsTag.getString("name");
            if(!mapTag.containsKey(key)){
                mapTag.put(name, key);
            }
        }
        return mapTag;
    }

    private static Map<String, List<String>> getMapTag() throws SQLException {
        Statement stTag = con.createStatement();
        ResultSet rsTag = stTag.executeQuery("select key, tag from story_tag ");
        Map<String,List<String>> mapTag = new HashMap<>();
        while (rsTag.next()) {
            String key = rsTag.getString("key");
            String tag = rsTag.getString("tag");
            if(!mapTag.containsKey(key)){
                mapTag.put(key, new ArrayList<>());
            }
            mapTag.get(key).add(tag);
        }
        return mapTag;
    }

    private static java.util.Date convertUtilFromSql(java.sql.Date uDate) {
        if(uDate == null){
            return null;
        }
        java.util.Date sDate = new java.util.Date(uDate.getTime());
        return sDate;
    }
}

