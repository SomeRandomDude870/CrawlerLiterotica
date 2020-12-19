package org.k.porn;

import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
public class ExtractChapter {
    public static Connection con = null;

    public static void getConnection() {
        String url = "jdbc:postgresql://localhost/literotica";
        Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "admin");
        //props.setProperty("ssl","true");
        try {
            Connection connection = DriverManager.getConnection(url, props);
            con = connection;
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT VERSION()");
            if (rs.next()) {
                //System.out.println(rs.getString(1));
            }

        } catch (SQLException ex) {

            Logger lgr = Logger.getLogger(Main.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    private static List<String> getListKeyChapter() throws SQLException {
        Statement stkey = con.createStatement();
        ResultSet rskey = stkey.executeQuery("select distinct key from story ");
        List<String> toReturn = new ArrayList<>();
        while (rskey.next()) {
            String key = rskey.getString("key");
            toReturn.add(key);
        }
        return toReturn;
    }
    public static void main(String[] args) throws SQLException, IOException {
        getConnection();

        getListKeyChapter().parallelStream().forEach(
        value->{
            try {
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery("select title, author, content, key from story where key ='" + value + "'");
                while (rs.next()) {
                    StringBuilder fullStory = new StringBuilder();
                    String title = null;
                    String author = null;
                    System.out.println(rs.getString(1));
                    title = rs.getString("title");
                    String key = rs.getString("key");
                    author = rs.getString("author");
                    fullStory.append("<h1 class=\"chapter\">");
                    fullStory.append(rs.getString("title"));
                    fullStory.append("</h1>");
                    fullStory.append(rs.getString("content"));
                    FileWriter myWriter = new FileWriter("chapter/" + key + ".html");
                    myWriter.write("<html><head><title>");
                    myWriter.write(title);
                    myWriter.write("</title>");
                    myWriter.write("<meta content=\"" + author + "\" name=\"author\">");
                    myWriter.write("</head><body>");
                    myWriter.write(fullStory.toString());
                    myWriter.write("</html>");
                    myWriter.close();
                    log.info("key");
                }
            }catch(Exception e){
                log.error(e.getMessage());
            }
        }
        );



    }
}
